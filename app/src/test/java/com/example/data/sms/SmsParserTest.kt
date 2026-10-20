package com.example.data.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserTest {

    @Test
    fun parsesDebitMessageWithAmountAccountAndMerchant() {
        val result = SmsParser.parseMessage(
            "USD 89.50 debited on A/C ending 4321 at Target Superstore on 31-Jul-26."
        )

        assertNotNull(result)
        result!!
        assertEquals("DEBIT", result.type)
        assertEquals(89.50, result.amount, 0.001)
        assertEquals("4321", result.accountLast4)
        assertTrue(result.isParsedSuccessfully)
    }

    @Test
    fun parsesCreditMessageAsSalaryIncome() {
        val result = SmsParser.parseMessage(
            "Your A/C XX4321 credited with USD 3,200.00 from TechCorp Payroll."
        )

        assertNotNull(result)
        result!!
        assertEquals("CREDIT", result.type)
        assertEquals(3200.00, result.amount, 0.001)
        assertEquals("Salary & Income", result.suggestedCategory)
    }

    @Test
    fun returnsNullForNonFinancialMessage() {
        val result = SmsParser.parseMessage("Hey, are we still on for lunch tomorrow?")
        assertNull(result)
    }

    @Test
    fun returnsNullForOtpMessageWithNoTransactionKeywords() {
        val result = SmsParser.parseMessage("123456 is your OTP. Do not share it with anyone.")
        assertNull(result)
    }

    @Test
    fun rejectsOtpForAPendingPurchaseEvenThoughItHasAnAmount() {
        // Indian card OTPs quote the amount and merchant, but nothing has been charged yet.
        val result = SmsParser.parseMessage(
            "483921 is the OTP for txn of INR 2,499.00 at AMAZON on HDFC Bank card ending 1122. Valid for 5 mins."
        )
        assertNull(result)
    }

    @Test
    fun suggestsCategoryFromMerchantKeywordsForDebit() {
        val result = SmsParser.parseMessage(
            "A/C XX1122 debited for USD 15.99 to Netflix.com"
        )

        assertNotNull(result)
        result!!
        assertEquals("DEBIT", result.type)
        assertEquals(15.99, result.amount, 0.001)
        assertEquals("Bills & Utilities", result.suggestedCategory)
    }

    @Test
    fun extractsSenderHeaderWhenNoExplicitSenderGiven() {
        val result = SmsParser.parseMessage(
            "VD-HDFCBK-T: Rs. 1,500.00 debited on A/C ending 4321 at Starbucks Cafe."
        )

        assertNotNull(result)
        result!!
        assertEquals("VD-HDFCBK-T", result.senderAddress)
        assertEquals("4321", result.accountLast4)
    }

    @Test
    fun explicitSenderAddressTakesPriorityOverHeaderParsing() {
        val result = SmsParser.parseMessage(
            messageBody = "Rs. 500 debited on A/C ending 9876 at Amazon.",
            senderAddress = "AD-HDFCBK-S"
        )

        assertNotNull(result)
        assertEquals("AD-HDFCBK-S", result!!.senderAddress)
    }

    @Test
    fun rejectsZeroOrNegativeAmounts() {
        // No positive amount anywhere in the message - should fail to parse rather than
        // fabricate a zero/negative transaction.
        val result = SmsParser.parseMessage("Your account was debited. Balance updated.")
        assertNull(result)
    }

    // --- Indian bank formats (shapes from real SMS; names and numbers are made up) ---

    @Test
    fun parsesHdfcUpiSentMessage() {
        val result = SmsParser.parseMessage(
            "Sent Rs.250.00 From HDFC Bank A/C *4321 To SRI GOKULAM BAKERY On 12/10/26 Ref 512345678901 " +
                "Not You? Call 18002586161/SMS BLOCK UPI to 7308080808",
            senderAddress = "VD-HDFCBK-S"
        )!!
        assertEquals("DEBIT", result.type)
        assertEquals(250.0, result.amount, 0.001)
        assertEquals("4321", result.accountLast4)
        assertEquals("Sri Gokulam Bakery", result.merchantOrSender)
        assertEquals("Food & Dining", result.suggestedCategory)
    }

    @Test
    fun parsesIndianBankCreditAndKeepsBalanceSeparateFromAmount() {
        val result = SmsParser.parseMessage(
            "Your A/c *9876 is credited with Rs.1,500.00 on 05-10-26 by Mr A Kumar. RRN 512345678902. " +
                "Available balance is Rs. 23,456.78 - Indian Bank"
        )!!
        assertEquals("CREDIT", result.type)
        assertEquals(1500.0, result.amount, 0.001)
        assertEquals(23456.78, result.availableBalance!!, 0.001)
        assertEquals("9876", result.accountLast4)
        assertEquals("Mr A Kumar", result.merchantOrSender)
    }

    @Test
    fun parsesLakhGroupedSalaryDeposit() {
        val result = SmsParser.parseMessage(
            "Update! INR 1,25,000.00 deposited in HDFC Bank A/c XX4321 on 01-OCT-26 for ACME TECH PVT LTD-Salary OCT 2026." +
                "Avl bal:INR 1,40,210.00"
        )!!
        assertEquals("CREDIT", result.type)
        assertEquals(125000.0, result.amount, 0.001)
        assertEquals("Salary & Income", result.suggestedCategory)
    }

    @Test
    fun cardWithdrawalUsesWithdrawnAmountNotBalance() {
        val result = SmsParser.parseMessage(
            "Withdrawn Rs.2000 From HDFC Bank Card x1122 At MAIN ROAD ATM On 2026-10-04:10:15:22 Bal Rs.18000.50 " +
                "Not You? Call 18002586161"
        )!!
        assertEquals("DEBIT", result.type)
        assertEquals(2000.0, result.amount, 0.001)
        assertEquals("1122", result.accountLast4)
    }

    @Test
    fun creditCardSpendIsADebitNotACredit() {
        val result = SmsParser.parseMessage(
            "Rs.1,299.00 spent on HDFC Bank Credit Card XX1122 at AMAZON on 2026-10-05. Avl Limit: Rs.48,701.00"
        )!!
        assertEquals("DEBIT", result.type)
        assertEquals(1299.0, result.amount, 0.001)
        assertEquals("Shopping", result.suggestedCategory)
    }

    @Test
    fun upiDebitThatAlsoNamesTheCreditedPayeeIsADebit() {
        val result = SmsParser.parseMessage(
            "ICICI Bank Acct XX234 debited for Rs 500.00 on 06-Oct-26; RAVI K credited. UPI:512345678903. " +
                "Call 18002662 for dispute."
        )!!
        assertEquals("DEBIT", result.type)
        assertEquals(500.0, result.amount, 0.001)
    }

    @Test
    fun parsesSbiStyleAmountWithoutCurrencySymbol() {
        val result = SmsParser.parseMessage(
            "Dear UPI user A/C X1234 debited by 75.0 on date 07Oct26 trf to MEERA TEA STALL Refno 512345678904. -SBI"
        )!!
        assertEquals("DEBIT", result.type)
        assertEquals(75.0, result.amount, 0.001)
        assertEquals("Meera Tea Stall", result.merchantOrSender)
    }

    @Test
    fun aBankTheAppHasNeverSeenIsStillDetected() {
        val result = SmsParser.parseMessage(
            "Dear Customer, Rs.540.00 debited from A/c XX5678 on 05-10-26 towards TNPDCL electricity bill. " +
                "Avl Bal: Rs.12,045.10 - Some New Co-op Bank",
            senderAddress = "JM-NEWCOP-S"
        )!!
        assertEquals("DEBIT", result.type)
        assertEquals(540.0, result.amount, 0.001)
        assertEquals("5678", result.accountLast4)
        assertEquals("NEWCOP", SmsParser.senderCode(result.senderAddress))
    }

    @Test
    fun rejectsMessagesThatAreNotTransactions() {
        val notTransactions = listOf(
            // telecom call-charge alert
            "Last-Call:00:01:12, Charge:Rs0.50, Main-Bal:Rs12.30, ULPack-Exp:30-Sep-26",
            // bill-due reminder
            "Your electricity bill of Rs 342.00 is due tomorrow. Pay now on PayZapp using your HDFC Bank account.",
            // offer
            "Get a pre-approved personal loan of Rs 5,00,000 credited to your A/c in 10 mins. Apply now!",
            // UPI collect request
            "ABC STORE has requested money Rs 120 from your UPI ID. Approve in your bank app.",
            // failed transaction
            "Txn of Rs 300.00 from A/c XX4321 failed due to technical error. Ref 512345678905",
            // future-dated mandate debit
            "Rs 649 will be debited from your A/c XX4321 on 10-10-26 for NETFLIX autopay mandate.",
            // reward points, no money amount
            "Reward Points Credited: 500 To HDFC Bank Credit Card 1122 On 01/OCT/2026",
            // invite with no amount
            "Your friend Arun invited you to the Splitwise group 'Goa Trip'. Tap to accept: https://example.com"
        )
        for (msg in notTransactions) {
            assertNull("should reject: $msg", SmsParser.parseMessage(msg))
        }
    }

    @Test
    fun rejectsPromotionalDltSender() {
        val result = SmsParser.parseMessage(
            "Rs.500 cashback credited to your Paytm wallet on your first UPI payment!",
            senderAddress = "AX-PAYTMB-P"
        )
        assertNull(result)
    }

    @Test
    fun senderCodeStripsOperatorPrefixAndTypeSuffix() {
        assertEquals("HDFCBK", SmsParser.senderCode("VD-HDFCBK-T"))
        assertEquals("HDFCBK", SmsParser.senderCode("AD-HDFCBK"))
        assertEquals("+919876543210", SmsParser.senderCode("+919876543210"))
        assertNull(SmsParser.senderCode("  "))
    }
}
