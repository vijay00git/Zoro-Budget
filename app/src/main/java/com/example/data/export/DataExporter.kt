package com.example.data.export

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataExporter {

    /**
     * Standard CSV escaping (RFC 4180): always quote the field and double up any embedded
     * quote characters, rather than stripping commas/quotes and silently losing information.
     */
    private fun csvField(value: String): String {
        return "\"${value.replace("\"", "\"\"")}\""
    }

    fun exportToCsv(transactions: List<TransactionEntity>, accounts: List<AccountEntity>): String {
        val accountMap = accounts.associateBy { it.id }
        val sb = StringBuilder()
        sb.append("ID,Date,Title,Amount,Type,Category,Account,Source,Merchant,IsRecurring,Notes\n")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        for (tx in transactions) {
            val accountName = accountMap[tx.accountId]?.name ?: "Account #${tx.accountId}"
            val dateStr = dateFormat.format(Date(tx.dateTimestamp))

            sb.append(tx.id).append(',')
            sb.append(csvField(dateStr)).append(',')
            sb.append(csvField(tx.title)).append(',')
            sb.append(tx.amount).append(',')
            sb.append(csvField(tx.type)).append(',')
            sb.append(csvField(tx.categoryTag)).append(',')
            sb.append(csvField(accountName)).append(',')
            sb.append(csvField(tx.source)).append(',')
            sb.append(csvField(tx.merchantOrSender ?: "")).append(',')
            sb.append(tx.isRecurring).append(',')
            sb.append(csvField(tx.notes ?: "")).append('\n')
        }

        return sb.toString()
    }

    fun exportToJson(
        transactions: List<TransactionEntity>,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>
    ): String {
        val root = JSONObject()

        val accArray = JSONArray()
        for (acc in accounts) {
            val obj = JSONObject()
            obj.put("id", acc.id)
            obj.put("name", acc.name)
            obj.put("type", acc.type)
            obj.put("balance", acc.balance)
            obj.put("accountNumberLast4", acc.accountNumberLast4 ?: "")
            obj.put("colorHex", acc.colorHex)
            accArray.put(obj)
        }
        root.put("accounts", accArray)

        val catArray = JSONArray()
        for (cat in categories) {
            val obj = JSONObject()
            obj.put("id", cat.id)
            obj.put("name", cat.name)
            obj.put("iconName", cat.iconName)
            obj.put("colorHex", cat.colorHex)
            obj.put("monthlyBudgetLimit", cat.monthlyBudgetLimit)
            catArray.put(obj)
        }
        root.put("categories", catArray)

        val txArray = JSONArray()
        for (tx in transactions) {
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("title", tx.title)
            obj.put("amount", tx.amount)
            obj.put("type", tx.type)
            obj.put("accountId", tx.accountId)
            obj.put("categoryTag", tx.categoryTag)
            obj.put("dateTimestamp", tx.dateTimestamp)
            obj.put("source", tx.source)
            obj.put("merchantOrSender", tx.merchantOrSender ?: "")
            obj.put("isPendingReview", tx.isPendingReview)
            obj.put("notes", tx.notes ?: "")
            obj.put("isRecurring", tx.isRecurring)
            obj.put("recurringFrequency", tx.recurringFrequency ?: "")
            if (tx.recurringDueDate != null) obj.put("recurringDueDate", tx.recurringDueDate)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        return root.toString(2)
    }

    data class ParsedJsonImport(
        val accounts: List<AccountEntity>,
        val categories: List<CategoryEntity>,
        val transactions: List<TransactionEntity>
    )

    fun parseJsonImport(jsonString: String): ParsedJsonImport? {
        return try {
            val root = JSONObject(jsonString)

            val accounts = mutableListOf<AccountEntity>()
            if (root.has("accounts")) {
                val accArray = root.getJSONArray("accounts")
                for (i in 0 until accArray.length()) {
                    val obj = accArray.getJSONObject(i)
                    accounts.add(
                        AccountEntity(
                            id = if (obj.has("id")) obj.getLong("id") else 0,
                            name = obj.getString("name"),
                            type = obj.optString("type", "CHECKING"),
                            balance = obj.optDouble("balance", 0.0),
                            accountNumberLast4 = obj.optString("accountNumberLast4").takeIf { it.isNotBlank() },
                            colorHex = obj.optString("colorHex", "#2563EB"),
                            senderKeywords = obj.optString("senderKeywords").takeIf { it.isNotBlank() },
                            customTypeLabel = obj.optString("customTypeLabel").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }

            val categories = mutableListOf<CategoryEntity>()
            if (root.has("categories")) {
                val catArray = root.getJSONArray("categories")
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    categories.add(
                        CategoryEntity(
                            id = if (obj.has("id")) obj.getLong("id") else 0,
                            name = obj.getString("name"),
                            iconName = obj.optString("iconName", "Label"),
                            colorHex = obj.optString("colorHex", "#64748B"),
                            monthlyBudgetLimit = obj.optDouble("monthlyBudgetLimit", 0.0)
                        )
                    )
                }
            }

            val transactions = mutableListOf<TransactionEntity>()
            if (root.has("transactions")) {
                val txArray = root.getJSONArray("transactions")
                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    transactions.add(
                        TransactionEntity(
                            title = obj.optString("title", "Imported Transaction"),
                            amount = obj.optDouble("amount", 0.0),
                            type = obj.optString("type", "DEBIT"),
                            accountId = obj.optLong("accountId", 1L),
                            categoryTag = obj.optString("categoryTag", "Uncategorized"),
                            dateTimestamp = obj.optLong("dateTimestamp", System.currentTimeMillis()),
                            source = obj.optString("source", "MANUAL"),
                            merchantOrSender = obj.optString("merchantOrSender").takeIf { it.isNotBlank() },
                            isPendingReview = obj.optBoolean("isPendingReview", false),
                            notes = obj.optString("notes").takeIf { it.isNotBlank() },
                            isRecurring = obj.optBoolean("isRecurring", false),
                            recurringFrequency = obj.optString("recurringFrequency").takeIf { it.isNotBlank() },
                            recurringDueDate = if (obj.has("recurringDueDate")) obj.getInt("recurringDueDate") else null
                        )
                    )
                }
            }

            ParsedJsonImport(accounts, categories, transactions)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
