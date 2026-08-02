package com.example.data.sms

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.local.BudgetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsInboxScanner(private val context: Context, private val repository: BudgetRepository) {

    suspend fun scanInboxAndImport(maxCount: Int = 100, minTimestamp: Long = 0L): Int = withContext(Dispatchers.IO) {
        var importedCount = 0
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "date", "body")

        try {
            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "date DESC"
            )

            cursor?.use {
                val bodyIdx = it.getColumnIndex("body")
                val dateIdx = it.getColumnIndex("date")
                val addressIdx = it.getColumnIndex("address")
                var count = 0

                while (it.moveToNext() && count < maxCount) {
                    val date = if (dateIdx >= 0) it.getLong(dateIdx) else System.currentTimeMillis()

                    // Cursor is sorted "date DESC", so once one message is older than the
                    // cutoff every remaining row is too - stop scanning instead of reading
                    // through the rest of the inbox for nothing.
                    if (minTimestamp > 0L && date < minTimestamp) {
                        break
                    }

                    val body = if (bodyIdx >= 0) it.getString(bodyIdx) else null
                    val address = if (addressIdx >= 0) it.getString(addressIdx) else null

                    if (!body.isNullOrBlank()) {
                        val parsed = SmsParser.parseMessage(body, date, address)
                        if (parsed != null) {
                            val isImported = repository.processParsedSmsTransaction(parsed)
                            if (isImported) {
                                importedCount++
                            }
                        }
                    }
                    count++
                }
            }
        } catch (e: Exception) {
            Log.e("SmsInboxScanner", "Failed to scan SMS inbox", e)
        }

        importedCount
    }
}
