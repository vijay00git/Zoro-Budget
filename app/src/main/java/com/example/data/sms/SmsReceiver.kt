package com.example.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.BudgetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val sb = StringBuilder()
            var timestamp = System.currentTimeMillis()
            for (msg in messages) {
                sb.append(msg.messageBody)
                timestamp = msg.timestampMillis
            }
            val rawMessage = sb.toString()

            val senderAddress = messages.firstOrNull()?.originatingAddress

            val prefs = context.getSharedPreferences("smart_budget_prefs", Context.MODE_PRIVATE)
            val minTimestamp = prefs.getLong("sms_sync_from_date", 0L)
            if (minTimestamp > 0L && timestamp < minTimestamp) {
                Log.d("SmsReceiver", "Ignoring incoming SMS because timestamp $timestamp is before sync starting date $minTimestamp")
                return
            }

            val parsed = SmsParser.parseMessage(rawMessage, timestamp, senderAddress) ?: return

            val db = AppDatabase.getDatabase(context)
            val repository = BudgetRepository(db.accountDao(), db.transactionDao(), db.categoryDao())

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.processParsedSmsTransaction(parsed)
                    Log.d("SmsReceiver", "Successfully processed SMS transaction: ${parsed.amount} ${parsed.type}")
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error saving SMS transaction", e)
                }
            }
        }
    }
}
