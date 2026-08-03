package com.example.data.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BillAlertScheduler {
    private const val PERIODIC_WORK_NAME = "bill_alert_periodic_check"
    private const val ONE_TIME_WORK_NAME = "bill_alert_immediate_check"

    /** Runs roughly every 12 hours so due-bill/low-balance notifications stay current without SMS activity. */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<BillAlertWorker>(12, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** Fired right after an SMS import so a balance-changing transaction is reflected without waiting for the periodic run. */
    fun enqueueImmediateCheck(context: Context) {
        val request = OneTimeWorkRequestBuilder<BillAlertWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
