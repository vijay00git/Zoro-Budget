package com.example.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.utils.BillAlertInfo
import java.util.Locale

/**
 * Posting a notification with the same (channel, id) pair as a prior one updates it in place
 * instead of stacking a duplicate, so a bill/account that stays overdue/low across multiple
 * periodic worker runs doesn't spam the user - it just refreshes the same entry.
 */
object NotificationHelper {
    const val CHANNEL_BILL_REMINDERS = "bill_reminders"
    const val CHANNEL_LOW_BALANCE = "low_balance"

    // Bill and low-balance notification IDs are namespaced into disjoint ranges so a
    // transaction id and an account id can never collide on the same notification slot.
    private const val BILL_ID_OFFSET = 10_000
    private const val LOW_BALANCE_ID_OFFSET = 20_000

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_BILL_REMINDERS,
                context.getString(R.string.notification_channel_bill_reminders_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_bill_reminders_desc)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_LOW_BALANCE,
                context.getString(R.string.notification_channel_low_balance_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_low_balance_desc)
            }
        )
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    fun showBillReminder(context: Context, transaction: TransactionEntity, alertInfo: BillAlertInfo, currencySymbol: String) {
        if (!canPostNotifications(context)) return

        val amountText = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, transaction.amount)
        val notification = NotificationCompat.Builder(context, CHANNEL_BILL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(context.getString(R.string.notification_bill_reminder_title, transaction.title))
            .setContentText(context.getString(R.string.notification_bill_reminder_text, alertInfo.alertMessage, amountText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent(context))
            .build()

        androidx.core.app.NotificationManagerCompat.from(context)
            .notify(BILL_ID_OFFSET + transaction.id.toInt(), notification)
    }

    fun showLowBalance(context: Context, account: AccountEntity, currencySymbol: String, threshold: Double) {
        if (!canPostNotifications(context)) return

        val balanceText = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, account.balance)
        val thresholdText = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, threshold)
        val notification = NotificationCompat.Builder(context, CHANNEL_LOW_BALANCE)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(context.getString(R.string.notification_low_balance_title, account.name))
            .setContentText(context.getString(R.string.notification_low_balance_text, balanceText, thresholdText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent(context))
            .build()

        androidx.core.app.NotificationManagerCompat.from(context)
            .notify(LOW_BALANCE_ID_OFFSET + account.id.toInt(), notification)
    }
}
