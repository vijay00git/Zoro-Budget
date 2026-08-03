package com.example.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataExporter {

    /**
     * Writes [content] to a real file under the app's cache dir and returns a share Intent for
     * it via FileProvider, instead of only offering a clipboard copy. This is what lets a backup
     * actually be saved/emailed/uploaded as a genuine .csv/.json file by whatever app the user
     * picks in the share sheet, rather than pasted in as plain text.
     */
    fun buildExportShareIntent(context: Context, content: String, isJson: Boolean): Intent {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val extension = if (isJson) "json" else "csv"
        val mimeType = if (isJson) "application/json" else "text/csv"

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "smart_budget_backup_$timestamp.$extension")
        file.writeText(content)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Smart Budget backup")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, "Share backup file")
    }

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

    data class CsvTransactionRow(
        val dateTimestamp: Long,
        val title: String,
        val amount: Double,
        val type: String,
        val category: String,
        val accountName: String,
        val source: String,
        val merchantOrSender: String?,
        val isRecurring: Boolean,
        val notes: String?
    )

    private const val CSV_HEADER = "ID,Date,Title,Amount,Type,Category,Account,Source,Merchant,IsRecurring,Notes"

    /**
     * Parses the CSV format written by [exportToCsv] (also compatible with any CSV using the same
     * header/column order, e.g. re-exported from another budgeting tool). Returns null only when
     * the header doesn't match at all; a header match with zero data rows returns an empty list.
     */
    fun parseCsvImport(csvString: String): List<CsvTransactionRow>? {
        val lines = csvString.lines().filter { it.isNotBlank() }
        if (lines.isEmpty() || !lines.first().trim().startsWith(CSV_HEADER)) return null

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val rows = mutableListOf<CsvTransactionRow>()

        for (line in lines.drop(1)) {
            val fields = parseCsvLine(line)
            if (fields.size < 11) continue

            val dateTimestamp = try {
                dateFormat.parse(fields[1])?.time
            } catch (e: Exception) {
                null
            } ?: continue
            val amount = fields[3].toDoubleOrNull() ?: continue

            rows.add(
                CsvTransactionRow(
                    dateTimestamp = dateTimestamp,
                    title = fields[2].ifBlank { "Imported Transaction" },
                    amount = amount,
                    type = fields[4].ifBlank { "DEBIT" },
                    category = fields[5].ifBlank { "Uncategorized" },
                    accountName = fields[6].ifBlank { "Imported Account" },
                    source = fields[7].ifBlank { "MANUAL" },
                    merchantOrSender = fields[8].takeIf { it.isNotBlank() },
                    isRecurring = fields[9].equals("true", ignoreCase = true),
                    notes = fields[10].takeIf { it.isNotBlank() }
                )
            )
        }

        return rows
    }

    /** RFC 4180 line splitter: honors quoted fields containing commas and doubled/escaped quotes. */
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        fields.add(current.toString())
        return fields
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
