package com.example.data.local

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.RecurringBillEntity
import com.example.data.local.entity.TransactionEntity
import org.json.JSONObject

/**
 * The rows seeded as demo data on a first install, so "Clear demo data" removes exactly those.
 * Each row is kept by id together with a fingerprint: a row whose fingerprint no longer matches
 * was edited by the user (or the id now belongs to an imported row) and is left alone.
 */
data class DemoData(
    val accounts: Map<Long, String>,
    val transactions: Map<Long, String>,
    val bills: Map<Long, String>
) {
    fun toJson(): String = JSONObject()
        .put(KEY_ACCOUNTS, JSONObject(accounts.mapKeys { it.key.toString() }))
        .put(KEY_TRANSACTIONS, JSONObject(transactions.mapKeys { it.key.toString() }))
        .put(KEY_BILLS, JSONObject(bills.mapKeys { it.key.toString() }))
        .toString()

    companion object {
        private const val KEY_ACCOUNTS = "accounts"
        private const val KEY_TRANSACTIONS = "transactions"
        private const val KEY_BILLS = "bills"

        fun fingerprint(account: AccountEntity) = "${account.name}|${account.accountNumberLast4}"

        // Not the title: reviewing the demo SMS changes its title, and it is still demo data.
        fun fingerprint(transaction: TransactionEntity) = "${transaction.amount}|${transaction.dateTimestamp}"

        fun fingerprint(bill: RecurringBillEntity) = "${bill.name}|${bill.amount}"

        fun fromJson(json: String?): DemoData? {
            if (json.isNullOrBlank()) return null
            return runCatching {
                val root = JSONObject(json)
                DemoData(
                    accounts = root.getJSONObject(KEY_ACCOUNTS).toIdMap(),
                    transactions = root.getJSONObject(KEY_TRANSACTIONS).toIdMap(),
                    bills = root.getJSONObject(KEY_BILLS).toIdMap()
                )
            }.getOrNull()
        }

        private fun JSONObject.toIdMap(): Map<Long, String> =
            keys().asSequence().associate { it.toLong() to getString(it) }
    }
}
