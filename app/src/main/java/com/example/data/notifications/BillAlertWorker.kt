package com.example.data.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.preferences.AppPreferences
import com.example.data.utils.BillAlertSeverity
import com.example.data.utils.RecurringBillUtils

/**
 * Checks every recurring bill and account balance against their alert thresholds and posts a
 * notification for anything that needs the user's attention. Scheduled periodically and also
 * fired as a one-shot right after an SMS import, since a newly-parsed transaction can push an
 * account below the low-balance threshold immediately.
 */
class BillAlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val appContext = applicationContext
        NotificationHelper.ensureChannels(appContext)

        val db = AppDatabase.getDatabase(appContext)
        val prefs = AppPreferences.load(appContext)

        val recurringTransactions = db.transactionDao().getRecurringTransactionsList()
        for (transaction in recurringTransactions) {
            val alert = RecurringBillUtils.calculateAlert(transaction)
            if (alert.severity == BillAlertSeverity.URGENT || alert.severity == BillAlertSeverity.WARNING) {
                NotificationHelper.showBillReminder(appContext, transaction, alert, prefs.currencySymbol)
            }
        }

        val accounts = db.accountDao().getAllAccountsList()
        for (account in accounts) {
            if (account.balance < prefs.lowBalanceThreshold) {
                NotificationHelper.showLowBalance(appContext, account, prefs.currencySymbol, prefs.lowBalanceThreshold)
            }
        }

        return Result.success()
    }
}
