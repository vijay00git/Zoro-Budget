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
    fun ignoresOtpDigitsWhenChoosingTransactionAmount() {
        // "for" is a financial-amount trigger word and "debited" makes this financial, but the
        // OTP number must not be picked as the transaction amount.
        val result = SmsParser.parseMessage(
            "Rs. 250 debited for purchase at Cafe. OTP 883921 was used to authorize."
        )

        assertNotNull(result)
        result!!
        assertEquals(250.0, result.amount, 0.001)
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
}
