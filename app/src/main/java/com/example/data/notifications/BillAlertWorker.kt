package com.example.data.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.preferences.AppPreferences
import com.example.data.utils.BillSchedule
import com.example.data.utils.BillStatus

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

        // Remind for bills that are overdue or inside their own reminder window. A paid bill has
        // already moved to its next cycle, so it stays quiet until that cycle is near.
        for (bill in db.recurringBillDao().getActiveBillsList()) {
            val status = BillSchedule.status(bill.nextDueDate, bill.reminderDaysBefore, bill.isActive)
            if (status == BillStatus.OVERDUE || status == BillStatus.DUE_SOON) {
                NotificationHelper.showBillReminder(appContext, bill, BillSchedule.daysUntil(bill.nextDueDate), prefs.currencySymbol)
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
