package com.example.data.sms

/**
 * Turns a bank SMS into a [ParsedSmsTransaction], or returns null for anything that isn't one.
 *
 * Classification is based on what the message says, not on who sent it, so a bank the app has
 * never seen before is still picked up. A message is a transaction only if it has:
 *  - a money amount (currency-tagged, e.g. "Rs.450", "INR 1,50,000.00", "₹99"),
 *  - a past-tense transaction verb (debited, sent, spent, withdrawn, credited, received, ...),
 *  - a payment-instrument reference (A/c, card, UPI, VPA, IMPS/NEFT, RRN, ...),
 * and does not look like an OTP, offer, due/statement reminder, failed attempt, money request or
 * telecom balance alert. Promotional DLT senders ("XX-NAME-P") are rejected outright.
 */
object SmsParser {

    private const val NUMBER = "([0-9][0-9,]*(?:\\.[0-9]{1,2})?)"

    // "Rs.450", "Rs 1,50,000.00", "INR 99", "₹ 20", "USD 12.50"
    private val CURRENCY_AMOUNT = Regex("(?i)(?:\\brs\\.?|\\binr|₹|\\busd|\\$|\\beur|€|\\bgbp|£)\\s*$NUMBER")

    // Untagged amount right after a verb, e.g. SBI's "A/C X1234 debited by 500.0"
    private val VERB_AMOUNT = Regex("(?i)\\b(?:debited|credited)\\s+(?:by|for|with)\\s+$NUMBER")

    // Text just before an amount that marks it as a balance or limit rather than the transaction
    private val BALANCE_CONTEXT = Regex("(?i)(?:\\bbal(?:ance)?|\\blimit|\\boutstanding|\\bavl|\\bavailable)[^0-9]{0,6}$")

    private val AVAILABLE_BALANCE = Regex(
        "(?i)(?:\\bavl\\.?\\s*bal(?:ance)?|\\bavailable\\s+bal(?:ance)?|\\bbal(?:ance)?)\\s*(?:is|:|-)?\\s*(?:rs\\.?|inr|₹)\\s*$NUMBER"
    )

    private val DEBIT_VERB = Regex(
        "(?i)\\b(?:debited|spent|withdrawn|withdrawal|sent|paid|purchased?|charged|deducted|transferred\\s+to|dr\\.?\\s+(?:with|for|by|rs|inr|₹))\\b"
    )
    private val CREDIT_VERB = Regex(
        "(?i)\\b(?:credited|received|deposited|refunded|refund|reversed|reversal|added\\s+to|cr\\.?\\s+(?:with|for|by|rs|inr|₹))\\b"
    )

    // Card / product names that contain "credit" or "debit" without describing the money flow
    private val CARD_PRODUCT_PHRASES = Regex(
        "(?i)\\b(?:credit|debit|cr)\\s+card\\b|\\bcredit\\s+(?:limit|score|line)\\b|\\bavailable\\s+credit\\b"
    )

    private val INSTRUMENT = Regex(
        "(?i)\\ba/?c\\b|\\bacct\\b|\\baccount\\b|\\bcard\\b|\\bupi\\b|\\bvpa\\b|\\bimps\\b|\\bneft\\b|\\brtgs\\b|\\bnach\\b" +
            "|\\bwallet\\b|\\bbank\\b|\\batm\\b|\\brrn\\b|\\butr\\b|\\bref\\b|@[a-z]{2,}"
    )

    private val REJECT_PATTERNS = listOf(
        // OTPs and verification codes
        Regex("(?i)\\botp\\b|one[- ]time password|verification code|\\bcode is\\b"),
        // Offers and marketing
        Regex(
            "(?i)pre-?approved|\\beligible\\b|\\bapply now\\b|\\bavail\\b|\\bclaim now\\b|\\bwinner\\b" +
                "|\\boffers?\\b|\\bvoucher\\b|\\bcoupon\\b|\\blimited period\\b|%\\s*off\\b|\\bt&c\\b"
        ),
        // Reminders and future-dated debits: nothing has moved yet
        Regex(
            "(?i)\\bwill be (?:auto[- ]?)?(?:debited|deducted|charged|credited)\\b|\\bis due\\b|\\bdue (?:on|date|by|tomorrow|today)\\b" +
                "|\\b(?:minimum|total) (?:amount )?due\\b|\\bstatement\\b|\\breminder\\b|\\bscheduled\\b"
        ),
        // Attempts that did not go through
        Regex("(?i)\\bfailed\\b|\\bdeclined\\b|\\bunsuccessful\\b|\\bcould not be\\b|\\binsufficient\\b|\\brejected\\b"),
        // UPI collect / money requests
        Regex("(?i)\\bhas requested\\b|\\brequested (?:money|rs|inr|₹)|\\bcollect request\\b|\\bpay request\\b"),
        // Telecom balance, usage and recharge notices (the bank sends its own debit SMS)
        Regex("(?i)\\blast-?call\\b|\\bmain-?bal\\b|\\bdata (?:pack|balance|usage|credited)\\b|\\bvalidity\\b|\\brecharge (?:of|is|was|successful)\\b")
    )

    // DLT sender ids: two-letter operator/circle prefix, the brand header, optional type suffix
    // (-S service, -T transactional, -G government, -P promotional)
    private val DLT_SENDER = Regex("^[A-Z]{2}-([A-Z0-9]{3,9})(?:-([A-Z]))?$")

    private val ACCOUNT_LAST4 = Regex("(?i)\\b(?:a/?c|acct|account|card)\\b[^0-9\\n]{0,15}?([0-9]{3,6})\\b")

    private val MERCHANT_STOP = "(?=\\s+(?:on|ref|refno|rrn|utr|bal|avl|via|using|upi|towards|dated|not)\\b|\\s*[,;(]|\\.(?:\\s|[A-Za-z]|$)|\\s+\\d{1,2}[-/]|\\s*$)"
    private val MERCHANT_EXCLUDED = "(?!your\\b|a/?c\\b|ac\\b|acct\\b|account\\b|the\\b|bank\\b|card\\b|vpa\\b)"
    private val PAYEE_PATTERN = Regex("(?i)\\b(?:to|at)\\s+(?:vpa\\s+)?$MERCHANT_EXCLUDED([A-Za-z0-9@._&'*-][A-Za-z0-9@._&' *-]{1,40}?)$MERCHANT_STOP")
    private val PAYER_PATTERN = Regex("(?i)\\b(?:from|by)\\s+(?:vpa\\s+)?$MERCHANT_EXCLUDED([A-Za-z0-9@._&'*-][A-Za-z0-9@._&' *-]{1,40}?)$MERCHANT_STOP")
    private val INFO_PATTERN = Regex("(?i)\\binfo[:\\s]+([A-Za-z0-9 &'-]{3,30})")

    fun parseMessage(
        messageBody: String,
        timestamp: Long = System.currentTimeMillis(),
        senderAddress: String? = null
    ): ParsedSmsTransaction? {
        val detectedSender = senderAddress ?: extractSenderFromHeader(messageBody)
        if (isPromotionalSender(detectedSender)) return null
        if (REJECT_PATTERNS.any { it.containsMatchIn(messageBody) }) return null
        if (!INSTRUMENT.containsMatchIn(messageBody)) return null

        val type = detectType(messageBody) ?: return null
        val amount = extractAmount(messageBody) ?: return null

        val merchantOrSender = extractMerchantOrSender(messageBody, type)
        return ParsedSmsTransaction(
            amount = amount,
            type = type,
            accountLast4 = extractAccountLast4(messageBody),
            merchantOrSender = merchantOrSender,
            suggestedCategory = suggestCategory(messageBody, merchantOrSender, type),
            rawMessage = messageBody,
            senderAddress = detectedSender,
            timestamp = timestamp,
            isParsedSuccessfully = true,
            availableBalance = extractAvailableBalance(messageBody)
        )
    }

    /**
     * The stable part of an SMS sender, used to recognise the same bank across messages:
     * "VD-HDFCBK-T" and "AD-HDFCBK-S" both become "HDFCBK". Other senders are returned as-is.
     */
    fun senderCode(senderAddress: String?): String? {
        val trimmed = senderAddress?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: return null
        return DLT_SENDER.matchEntire(trimmed)?.groupValues?.get(1) ?: trimmed
    }

    private fun isPromotionalSender(sender: String?): Boolean {
        val match = sender?.trim()?.uppercase()?.let { DLT_SENDER.matchEntire(it) } ?: return false
        return match.groupValues[2] == "P"
    }

    private fun extractSenderFromHeader(text: String): String? {
        // Match headers like "VD-HDFCBK-T: Rs 1500 debited..." or "[VD-HDFCBK-T] ..." or "BV-INDBNK-S - ..."
        val pattern = Regex("(?i)^(?:sender[:\\s]+)?\\[?([A-Za-z0-9]{2,4}-[A-Za-z0-9-]{4,12})\\]?(?:[:\\s-]|$)")
        return pattern.find(text.trim())?.groupValues?.get(1)?.uppercase()
    }

    /**
     * Debit vs credit from the earliest transaction verb. Banks lead with what happened to the
     * user's account ("A/c debited for Rs 500 ... NAME credited"), so the first verb wins.
     * Card product names ("Credit Card") are blanked first so they don't read as a credit.
     */
    private fun detectType(text: String): String? {
        val cleaned = CARD_PRODUCT_PHRASES.replace(text) { " ".repeat(it.value.length) }
        val debitAt = DEBIT_VERB.find(cleaned)?.range?.first
        val creditAt = CREDIT_VERB.find(cleaned)?.range?.first
        return when {
            debitAt == null && creditAt == null -> null
            creditAt == null -> "DEBIT"
            debitAt == null -> "CREDIT"
            debitAt < creditAt -> "DEBIT"
            else -> "CREDIT"
        }
    }

    private fun extractAmount(text: String): Double? {
        val transactionAmount = CURRENCY_AMOUNT.findAll(text)
            .filterNot { BALANCE_CONTEXT.containsMatchIn(text.substring(0, it.range.first)) }
            .firstNotNullOfOrNull { it.groupValues[1].toAmount() }
            ?: VERB_AMOUNT.find(text)?.groupValues?.get(1)?.toAmount()
        return transactionAmount?.takeIf { it > 0.0 && it < 100_000_000.0 }
    }

    private fun extractAvailableBalance(text: String): Double? =
        AVAILABLE_BALANCE.find(text)?.groupValues?.get(1)?.toAmount()

    private fun String.toAmount(): Double? = replace(",", "").trimEnd('.').toDoubleOrNull()

    private fun extractAccountLast4(text: String): String? =
        ACCOUNT_LAST4.find(text)?.groupValues?.get(1)?.takeLast(4)

    private fun extractMerchantOrSender(text: String, type: String): String? {
        // Debits name a payee ("To SAHA ENTERPRISE", "at Starbucks"), credits a payer
        // ("by Mr A Ganesan", "from VPA x@ybl"); fall back to the other direction if missing.
        val patterns = if (type == "CREDIT") {
            listOf(PAYER_PATTERN, PAYEE_PATTERN, INFO_PATTERN)
        } else {
            listOf(PAYEE_PATTERN, PAYER_PATTERN, INFO_PATTERN)
        }

        for (pattern in patterns) {
            for (match in pattern.findAll(text)) {
                val found = match.groupValues[1].trim().trimEnd('.', '-').replace(Regex("\\s+"), " ")
                if (found.length < 2 || found.none { it.isLetter() }) continue
                if (found.contains("card", ignoreCase = true)) continue
                return if (found.contains('@')) found else found.capitalizeWords()
            }
        }
        return null
    }

    private val CATEGORY_KEYWORDS = listOf(
        "Food & Dining" to listOf(
            "swiggy", "zomato", "restaurant", "hotel", "cafe", "bakery", "mess", "tea stall", "sweets",
            "dominos", "pizza", "kfc", "mcdonald", "starbucks", "burger", "food", "dining", "biryani"
        ),
        "Groceries" to listOf(
            "bigbasket", "blinkit", "zepto", "dmart", "grocery", "groceries", "supermarket", "super market",
            "mart", "provision", "vegetables", "fruits", "fresh", "kirana", "store"
        ),
        "Shopping" to listOf(
            "amazon", "flipkart", "myntra", "meesho", "ajio", "nykaa", "textile", "jewellers", "fashion",
            "apparel", "mall", "shop", "walmart", "target"
        ),
        "Transportation" to listOf(
            "irctc", "uber", "ola", "rapido", "redbus", "metro", "fuel", "petrol", "filling station",
            "service station", "pump", "hpcl", "iocl", "bpcl", "fastag", "parking", "flight", "transit"
        ),
        "Bills & Utilities" to listOf(
            "electricity", "tneb", "tnpdcl", "bescom", "water", "gas", "broadband", "airtel", "jio", "bsnl",
            "vodafone", "dth", "tata play", "netflix", "spotify", "subscription", "utility", "bill"
        ),
        "Entertainment" to listOf("bookmyshow", "pvr", "inox", "cinema", "theatre", "theater", "movie", "hotstar", "steam", "game"),
        "Health & Wellness" to listOf(
            "pharmacy", "medical", "medplus", "apollo", "hospital", "clinic", "diagnostic", "lab", "doctor", "health"
        ),
        "Investment & Savings" to listOf("zerodha", "groww", "mutual fund", "sip", "stocks", "investment", "fixed deposit")
    ).map { (category, words) -> category to Regex("(?i)\\b(?:${words.joinToString("|") { Regex.escape(it) }})\\b") }

    private fun suggestCategory(text: String, merchant: String?, type: String): String {
        val combined = "$text ${merchant ?: ""}"
        if (type == "CREDIT") {
            val isIncome = Regex("(?i)\\b(?:salary|payroll|stipend|interest|dividend)\\b").containsMatchIn(combined)
            return if (isIncome) "Salary & Income" else "Uncategorized"
        }
        return CATEGORY_KEYWORDS.firstOrNull { (_, regex) -> regex.containsMatchIn(combined) }?.first ?: "Uncategorized"
    }

    private fun String.capitalizeWords(): String {
        return this.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
