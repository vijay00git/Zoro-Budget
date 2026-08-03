package com.example.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.BudgetRepository
import com.example.data.notifications.BillAlertScheduler
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

            // goAsync() keeps the receiver (and its process) alive long enough for the
            // background DB write to finish; without it Android can kill the process the
            // instant onReceive() returns, dropping the transaction silently.
            val pendingResult = goAsync()
            val appContext = context.applicationContext

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(appContext)
                    val repository = BudgetRepository(db, db.accountDao(), db.transactionDao(), db.categoryDao())
                    repository.processParsedSmsTransaction(parsed)
                    Log.d("SmsReceiver", "Successfully processed SMS transaction: ${parsed.amount} ${parsed.type}")
                    BillAlertScheduler.enqueueImmediateCheck(appContext)
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error saving SMS transaction", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
