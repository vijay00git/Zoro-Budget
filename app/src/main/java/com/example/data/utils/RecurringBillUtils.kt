package com.example.data.utils

import com.example.data.local.entity.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class BillAlertSeverity {
    URGENT,   // Due today, yesterday, or overdue
    WARNING,  // Due tomorrow, or in 2-5 days
    INFO,     // Due in > 5 days
    PAID      // Already marked paid for the current cycle
}

data class BillAlertInfo(
    val daysDifference: Long,
    val alertMessage: String,
    val severity: BillAlertSeverity,
    val formattedDueDate: String,
    val frequencyLabel: String
)

object RecurringBillUtils {

    /**
     * Calculates the bill alert info (days until due, status message, severity, formatted target date)
     * for a given recurring transaction entity.
     */
    fun calculateAlert(transaction: TransactionEntity, nowMillis: Long = System.currentTimeMillis()): BillAlertInfo {
        val freq = transaction.recurringFrequency?.uppercase() ?: "MONTHLY"
        val customDay = transaction.recurringDueDate ?: extractDayFromTimestamp(transaction.dateTimestamp, freq)

        val calendarNow = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val todayMillis = calendarNow.timeInMillis

        val targetCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val frequencyLabel: String

        when (freq) {
            "WEEKLY" -> {
                // customDay: 1 = Monday ... 7 = Sunday
                val dayOfWeekCalendar = when (customDay) {
                    1 -> Calendar.MONDAY
                    2 -> Calendar.TUESDAY
                    3 -> Calendar.WEDNESDAY
                    4 -> Calendar.THURSDAY
                    5 -> Calendar.FRIDAY
                    6 -> Calendar.SATURDAY
                    7 -> Calendar.SUNDAY
                    else -> Calendar.MONDAY
                }
                val dayNames = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                val dayName = if (customDay in 1..7) dayNames[customDay - 1] else "Monday"
                frequencyLabel = "Every week on $dayName"

                // Adjust targetCal to this week's or next week's day
                targetCal.set(Calendar.DAY_OF_WEEK, dayOfWeekCalendar)
                var diffDays = ((targetCal.timeInMillis - todayMillis) / (1000 * 60 * 60 * 24)).toDouble().toLong()

                // If it was more than 3 days ago in the past week, shift to upcoming week
                if (diffDays < -3) {
                    targetCal.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            "YEARLY" -> {
                frequencyLabel = "Yearly"
                // Match month and day of original timestamp
                val origCal = Calendar.getInstance().apply { timeInMillis = transaction.dateTimestamp }
                targetCal.set(Calendar.MONTH, origCal.get(Calendar.MONTH))
                targetCal.set(Calendar.DAY_OF_MONTH, origCal.get(Calendar.DAY_OF_MONTH))
                val diffDays = ((targetCal.timeInMillis - todayMillis) / (1000 * 60 * 60 * 24)).toDouble().toLong()
                if (diffDays < -15) {
                    targetCal.add(Calendar.YEAR, 1)
                }
            }
            else -> { // "MONTHLY"
                frequencyLabel = "Every month on Day $customDay"
                // Clamp day to max days in month (e.g., Feb max 28)
                val maxDayThisMonth = targetCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val validDay = customDay.coerceIn(1, maxDayThisMonth)
                targetCal.set(Calendar.DAY_OF_MONTH, validDay)

                var diffDays = ((targetCal.timeInMillis - todayMillis) / (1000 * 60 * 60 * 24)).toDouble().toLong()

                // If it passed more than 7 days ago, show next month's due date
                if (diffDays < -7) {
                    targetCal.add(Calendar.MONTH, 1)
                    val maxDayNextMonth = targetCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    targetCal.set(Calendar.DAY_OF_MONTH, customDay.coerceIn(1, maxDayNextMonth))
                }
            }
        }

        val diffDays = ((targetCal.timeInMillis - todayMillis) / (1000.0 * 60 * 60 * 24)).toLong()
        val dateFormatter = SimpleDateFormat("MMM d", Locale.getDefault())
        val formattedDueDate = dateFormatter.format(Date(targetCal.timeInMillis))

        // A payment recorded any time since the start of the cycle `targetCal` represents
        // covers that cycle - without this, "mark as paid" would insert a payment transaction
        // but the bill would still show as due/overdue right after the user paid it.
        val cycleStartCal = (targetCal.clone() as Calendar).apply {
            when (freq) {
                "WEEKLY" -> add(Calendar.WEEK_OF_YEAR, -1)
                "YEARLY" -> add(Calendar.YEAR, -1)
                else -> add(Calendar.MONTH, -1)
            }
        }
        val paidThisCycle = transaction.lastPaidTimestamp != null &&
            transaction.lastPaidTimestamp >= cycleStartCal.timeInMillis

        val (alertMsg, severity) = when {
            paidThisCycle -> "Paid" to BillAlertSeverity.PAID
            diffDays == 0L -> "Due Today" to BillAlertSeverity.URGENT
            diffDays == 1L -> "Due Tomorrow" to BillAlertSeverity.WARNING
            diffDays in 2L..5L -> "Due in $diffDays days" to BillAlertSeverity.WARNING
            diffDays > 5L -> "Due in $diffDays days ($formattedDueDate)" to BillAlertSeverity.INFO
            diffDays == -1L -> "Due Yesterday" to BillAlertSeverity.URGENT
            diffDays in -5L..-2L -> "Overdue by ${abs(diffDays)} days" to BillAlertSeverity.URGENT
            else -> "Overdue by ${abs(diffDays)} days" to BillAlertSeverity.URGENT
        }

        return BillAlertInfo(
            daysDifference = diffDays,
            alertMessage = alertMsg,
            severity = severity,
            formattedDueDate = formattedDueDate,
            frequencyLabel = frequencyLabel
        )
    }

    private fun extractDayFromTimestamp(timestamp: Long, frequency: String): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return if (frequency == "WEEKLY") {
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }
        } else {
            cal.get(Calendar.DAY_OF_MONTH)
        }
    }
}
