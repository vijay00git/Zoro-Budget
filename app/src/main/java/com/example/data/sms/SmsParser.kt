package com.example.data.sms

import java.util.regex.Pattern

object SmsParser {

    // Regex patterns for amounts (e.g., $12.50, USD 120.00, Rs. 450, INR 1,500.00, €45.00, 450.00)
    private val AMOUNT_PATTERN = Pattern.compile(
        "(?i)(?:\\$|usd|inr|rs\\.?|eur|€|gbp|£|cad|aud|a\\$)?\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)"
    )

    // Regex for specific monetary context in financial SMS
    private val FINANCIAL_AMOUNT_PATTERN = Pattern.compile(
        "(?i)(?:rs\\.?|usd|inr|eur|gbp|cad|aud|\\$|€|£|amount|amt|for|by|of)\\s*[:.-]?\\s*(?:\\$|usd|inr|rs\\.?|eur|€|gbp|£)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"
    )

    // Regex patterns for Account / Card numbers (e.g. A/C ending 4321, card **9876, XX1234, account 5678)
    private val ACCOUNT_PATTERN = Pattern.compile(
        "(?i)(?:a/c|ac|account|card|no\\.?|number|ending|xx|\\*{2,4})\\s*[:.-]?\\s*([0-9]{3,4}|[xX*]+[0-9]{3,4})"
    )

    // Keywords indicating DEBIT vs CREDIT
    private val DEBIT_KEYWORDS = listOf(
        "debited", "debit", "spent", "paid", "withdrawn", "charged",
        "sent to", "purchase at", "dr", "used at", "transferred to", "payment for"
    )

    private val CREDIT_KEYWORDS = listOf(
        "credited", "credit", "received", "deposited", "added",
        "refunded", "salary from", "cr", "transferred from", "cashback"
    )

    fun parseMessage(
        messageBody: String,
        timestamp: Long = System.currentTimeMillis(),
        senderAddress: String? = null
    ): ParsedSmsTransaction? {
        val detectedSender = senderAddress ?: extractSenderFromHeader(messageBody)
        val lowerMsg = messageBody.lowercase()

        // 1. Check if message is a financial transaction SMS
        val isFinancial = DEBIT_KEYWORDS.any { lowerMsg.contains(it) } ||
                CREDIT_KEYWORDS.any { lowerMsg.contains(it) } ||
                lowerMsg.contains("bank") || lowerMsg.contains("a/c") || lowerMsg.contains("balance") ||
                !detectedSender.isNullOrBlank()

        if (!isFinancial) return null

        // 2. Determine Transaction Type (DEBIT vs CREDIT)
        var isCredit = false
        var isDebit = false

        for (kw in CREDIT_KEYWORDS) {
            if (lowerMsg.contains(kw)) {
                isCredit = true
                break
            }
        }

        if (!isCredit) {
            for (kw in DEBIT_KEYWORDS) {
                if (lowerMsg.contains(kw)) {
                    isDebit = true
                    break
                }
            }
        }

        val type = if (isCredit) "CREDIT" else "DEBIT"

        // 3. Extract Amount
        val amount = extractAmount(messageBody) ?: return null
        if (amount <= 0.0) return null

        // 4. Extract Account Suffix
        val accountLast4 = extractAccountLast4(messageBody)

        // 5. Extract Merchant or Sender Name
        val merchantOrSender = extractMerchantOrSender(messageBody, type)

        // 6. Suggest Category Tag
        val suggestedCategory = suggestCategory(messageBody, merchantOrSender, type)

        return ParsedSmsTransaction(
            amount = amount,
            type = type,
            accountLast4 = accountLast4,
            merchantOrSender = merchantOrSender,
            suggestedCategory = suggestedCategory,
            rawMessage = messageBody,
            senderAddress = detectedSender,
            timestamp = timestamp,
            isParsedSuccessfully = true
        )
    }

    private fun extractSenderFromHeader(text: String): String? {
        val trimmed = text.trim()
        // Match headers like "VD-HDFCBK-T: Rs 1500 debited..." or "[VD-HDFCBK-T] ..." or "BV-INDBNK-S - ..."
        val pattern = Pattern.compile("(?i)^(?:sender[:\\s]+)?\\[?([A-Za-z0-9]{2,4}-[A-Za-z0-9-]{4,12})\\]?(?:[:\\s-]|$)")
        val matcher = pattern.matcher(trimmed)
        if (matcher.find()) {
            return matcher.group(1)?.uppercase()
        }
        return null
    }

    private fun extractAmount(text: String): Double? {
        // Try specific financial amount pattern first
        val matcher1 = FINANCIAL_AMOUNT_PATTERN.matcher(text)
        while (matcher1.find()) {
            val amtStr = matcher1.group(1)?.replace(",", "")
            val parsed = amtStr?.toDoubleOrNull()
            if (parsed != null && parsed > 0.0) {
                return parsed
            }
        }

        // Fallback to general amount matcher
        val matcher2 = AMOUNT_PATTERN.matcher(text)
        var highestVal = 0.0
        while (matcher2.find()) {
            val amtStr = matcher2.group(1)?.replace(",", "")
            val parsed = amtStr?.toDoubleOrNull()
            if (parsed != null && parsed > 0.0 && parsed < 10000000.0) {
                // Ignore suspicious large numbers like account numbers or OTPs
                if (parsed > highestVal && !text.contains("otp", ignoreCase = true)) {
                    highestVal = parsed
                }
            }
        }

        return if (highestVal > 0.0) highestVal else null
    }

    private fun extractAccountLast4(text: String): String? {
        val matcher = ACCOUNT_PATTERN.matcher(text)
        if (matcher.find()) {
            val rawAcc = matcher.group(1) ?: return null
            val digitsOnly = rawAcc.replace(Regex("[^0-9]"), "")
            if (digitsOnly.length >= 3) {
                return digitsOnly.takeLast(4)
            }
        }
        return null
    }

    private fun extractMerchantOrSender(text: String, type: String): String? {
        val lower = text.lowercase()

        // Common patterns: "at STARBUCKS", "to JOHN DOE", "from ACME CORP", "for GROCERIES", "vpa MERCHANT@UPI"
        val patterns = listOf(
            Pattern.compile("(?i)\\bat\\s+([A-Za-z0-9\\s&'-]{3,25})(?:\\s+on|\\s+ref|\\s+bal|\\.|$)"),
            Pattern.compile("(?i)\\bto\\s+([A-Za-z0-9\\s&'-]{3,25})(?:\\s+on|\\s+ref|\\s+bal|\\.|$)"),
            Pattern.compile("(?i)\\bfrom\\s+([A-Za-z0-9\\s&'-]{3,25})(?:\\s+on|\\s+ref|\\s+bal|\\.|$)"),
            Pattern.compile("(?i)\\bvpa\\s+([A-Za-z0-9.@_-]{3,30})"),
            Pattern.compile("(?i)\\binfo[:\\s]+([A-Za-z0-9\\s&'-]{3,25})")
        )

        for (pattern in patterns) {
            val m = pattern.matcher(text)
            if (m.find()) {
                val found = m.group(1)?.trim()
                if (!found.isNullOrEmpty() && found.length in 2..30 &&
                    !found.equals("the", ignoreCase = true) &&
                    !found.equals("your", ignoreCase = true) &&
                    !found.contains("card", ignoreCase = true)
                ) {
                    return found.replace(Regex("\\s+"), " ").capitalizeWords()
                }
            }
        }

        return if (type == "CREDIT") "Deposit / Transfer" else "Merchant / Store"
    }

    private fun suggestCategory(text: String, merchant: String?, type: String): String {
        if (type == "CREDIT") {
            if (text.contains("salary", ignoreCase = true) || text.contains("payroll", ignoreCase = true)) {
                return "Salary & Income"
            }
            return "Salary & Income"
        }

        val combined = "$text ${merchant ?: ""}".lowercase()

        return when {
            combined.contains("starbucks") || combined.contains("cafe") || combined.contains("restaurant") ||
                    combined.contains("mcdonald") || combined.contains("burger") || combined.contains("pizza") ||
                    combined.contains("dunkin") || combined.contains("food") || combined.contains("dining") -> "Food & Dining"

            combined.contains("walmart") || combined.contains("target") || combined.contains("amazon") ||
                    combined.contains("store") || combined.contains("shop") || combined.contains("mall") ||
                    combined.contains("apparel") || combined.contains("fashion") -> "Shopping"

            combined.contains("grocery") || combined.contains("supermarket") || combined.contains("whole foods") ||
                    combined.contains("trader joe") || combined.contains("kroger") || combined.contains("fresh") -> "Groceries"

            combined.contains("electric") || combined.contains("utility") || combined.contains("water") ||
                    combined.contains("gas") || combined.contains("bill") || combined.contains("verizon") ||
                    combined.contains("at&t") || combined.contains("broadband") || combined.contains("subscription") ||
                    combined.contains("netflix") || combined.contains("spotify") -> "Bills & Utilities"

            combined.contains("uber") || combined.contains("lyft") || combined.contains("transit") ||
                    combined.contains("subway") || combined.contains("shell") || combined.contains("chevron") ||
                    combined.contains("fuel") || combined.contains("petrol") || combined.contains("flight") -> "Transportation"

            combined.contains("cinema") || combined.contains("theater") || combined.contains("movie") ||
                    combined.contains("game") || combined.contains("steam") || combined.contains("ticket") -> "Entertainment"

            combined.contains("hospital") || combined.contains("pharmacy") || combined.contains("doctor") ||
                    combined.contains("health") || combined.contains("cvs") || combined.contains("walgreens") -> "Health & Wellness"

            combined.contains("investment") || combined.contains("stocks") || combined.contains("crypto") ||
                    combined.contains("mutual fund") || combined.contains("deposit") -> "Investment & Savings"

            else -> "Uncategorized"
        }
    }

    private fun String.capitalizeWords(): String {
        return this.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
