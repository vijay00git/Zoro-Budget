package com.example.data.sms

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity

/**
 * Decides which account an SMS belongs to. Shared by the importer and the review UI so both
 * agree on whether a message came from a bank/card that no account is linked to yet.
 */
object SmsAccountMatcher {

    /**
     * 1. An account whose saved last-4 digits equal the ones in the SMS.
     * 2. An account whose sender keywords match the SMS sender (or appear in the text). When
     *    several accounts at the same bank match, the one whose digits agree wins.
     * Returns null when neither applies, i.e. a new bank, sender or card.
     */
    fun match(
        accounts: List<AccountEntity>,
        senderCode: String?,
        accountLast4: String?,
        rawMessage: String?
    ): AccountEntity? {
        if (!accountLast4.isNullOrEmpty()) {
            accounts.firstOrNull { it.accountNumberLast4?.trim()?.takeLast(4) == accountLast4 }?.let { return it }
        }

        val byKeyword = accounts.filter { acc ->
            keywordsOf(acc).any { kw -> senderMatches(senderCode, kw) || mentions(rawMessage, kw) }
        }
        return byKeyword.firstOrNull { it.accountNumberLast4?.trim()?.takeLast(4) == accountLast4 }
            ?: byKeyword.firstOrNull()
    }

    /** True for an SMS row whose bank/card isn't linked to any account, so the user should pick one. */
    fun needsAccountConfirmation(transaction: TransactionEntity, accounts: List<AccountEntity>): Boolean {
        if (transaction.source != "SMS_AUTO") return false
        if (transaction.smsSender == null && transaction.smsAccountLast4 == null) return false
        return match(accounts, transaction.smsSender, transaction.smsAccountLast4, transaction.rawSmsMessage) == null
    }

    fun keywordsOf(account: AccountEntity): List<String> =
        account.senderKeywords?.split(",")?.map { it.trim() }?.filter { it.length >= 3 }.orEmpty()

    private fun senderMatches(senderCode: String?, keyword: String): Boolean {
        if (senderCode.isNullOrEmpty()) return false
        val keywordCode = SmsParser.senderCode(keyword) ?: return false
        return senderCode.contains(keywordCode, ignoreCase = true)
    }

    private fun mentions(rawMessage: String?, keyword: String): Boolean {
        if (rawMessage.isNullOrEmpty()) return false
        return Regex("(?i)(?<![A-Za-z0-9])${Regex.escape(keyword)}(?![A-Za-z0-9])").containsMatchIn(rawMessage)
    }
}
