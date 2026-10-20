package com.example.data.utils

import java.util.Calendar
import kotlin.math.abs

enum class BillFrequency(val months: Int, val monthlyFactor: Double) {
    WEEKLY(0, 52.0 / 12.0),
    MONTHLY(1, 1.0),
    QUARTERLY(3, 1.0 / 3.0),
    YEARLY(12, 1.0 / 12.0);

    companion object {
        fun from(value: String?): BillFrequency = entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MONTHLY
    }
}

enum class BillStatus {
    OVERDUE, // due date has passed and the cycle isn't paid
    DUE_SOON, // due today or within the bill's reminder window
    UPCOMING, // due later
    PAUSED
}

/**
 * Due-date arithmetic for recurring bills. A bill stores the date its next unpaid cycle is due;
 * paying or skipping a cycle moves that date forward by one period. Monthly-style bills keep
 * their anchor day, clamped to short months (a bill due on the 31st is due on Feb 28/29).
 */
object BillSchedule {

    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** The due date one period after [dueDate]. [anchorDay] is the bill's day of month (1..31). */
    fun advance(dueDate: Long, frequency: BillFrequency, anchorDay: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = startOfDay(dueDate) }
        if (frequency == BillFrequency.WEEKLY) {
            cal.add(Calendar.DAY_OF_YEAR, 7)
            return cal.timeInMillis
        }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.MONTH, frequency.months)
        cal.set(Calendar.DAY_OF_MONTH, anchorDay.coerceIn(1, cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
        return cal.timeInMillis
    }

    /** The due date one period before [dueDate] - used to undo a payment or skip. */
    fun previous(dueDate: Long, frequency: BillFrequency, anchorDay: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = startOfDay(dueDate) }
        if (frequency == BillFrequency.WEEKLY) {
            cal.add(Calendar.DAY_OF_YEAR, -7)
            return cal.timeInMillis
        }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.MONTH, -frequency.months)
        cal.set(Calendar.DAY_OF_MONTH, anchorDay.coerceIn(1, cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
        return cal.timeInMillis
    }

    /**
     * The first unpaid due date for a bill last paid at [lastPaid] (or never paid). The due date
     * nearest the payment counts as the one it paid, so paying a few days early or late both
     * settle that cycle. Used when converting the old "recurring transaction" bills.
     */
    fun nextDueAfterPayment(lastPaid: Long, frequency: BillFrequency, anchorDay: Int): Long {
        val paidDay = startOfDay(lastPaid)
        // Neighbouring due dates are a week apart for weekly bills and a month apart otherwise
        // (a yearly bill paid Oct 25 for Oct 15 must settle Oct 15, not Nov 15).
        val step = if (frequency == BillFrequency.WEEKLY) BillFrequency.WEEKLY else BillFrequency.MONTHLY
        var due = firstOccurrenceOnOrAfter(paidDay, frequency, anchorDay)
        val before = previous(due, step, anchorDay)
        if (abs(paidDay - before) < abs(due - paidDay)) due = before
        return advance(due, frequency, anchorDay)
    }

    /** First due date on or after [fromMillis] for a bill due on [anchorDay] (day of month). */
    fun firstOccurrenceOnOrAfter(fromMillis: Long, frequency: BillFrequency, anchorDay: Int): Long {
        val from = startOfDay(fromMillis)
        val cal = Calendar.getInstance().apply { timeInMillis = from }
        if (frequency == BillFrequency.WEEKLY) {
            // anchorDay is ISO day of week here: 1 = Monday .. 7 = Sunday
            val target = (anchorDay.coerceIn(1, 7) % 7) + 1 // Calendar: Sunday = 1 .. Saturday = 7
            while (cal.get(Calendar.DAY_OF_WEEK) != target) cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }
        cal.set(Calendar.DAY_OF_MONTH, anchorDay.coerceIn(1, cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
        var due = cal.timeInMillis
        while (due < from) due = advance(due, BillFrequency.MONTHLY, anchorDay)
        return due
    }

    /** Whole days from today until [dueDate]; negative when overdue. */
    fun daysUntil(dueDate: Long, now: Long = System.currentTimeMillis()): Int =
        Math.round((startOfDay(dueDate) - startOfDay(now)).toDouble() / DAY_MS).toInt()

    fun status(dueDate: Long, reminderDaysBefore: Int, isActive: Boolean, now: Long = System.currentTimeMillis()): BillStatus {
        if (!isActive) return BillStatus.PAUSED
        val days = daysUntil(dueDate, now)
        return when {
            days < 0 -> BillStatus.OVERDUE
            days <= reminderDaysBefore -> BillStatus.DUE_SOON
            else -> BillStatus.UPCOMING
        }
    }

    /**
     * Whether a bill belongs on the "to pay" list: overdue, due within its reminder window, or
     * due before the end of the current month. A paid bill has moved to its next cycle, so it
     * drops off until that cycle comes round.
     */
    fun isDueForPayment(dueDate: Long, reminderDaysBefore: Int, isActive: Boolean, now: Long = System.currentTimeMillis()): Boolean {
        if (!isActive) return false
        if (daysUntil(dueDate, now) <= maxOf(reminderDaysBefore, 7)) return true
        val endOfMonth = Calendar.getInstance().apply {
            timeInMillis = startOfDay(now)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, 1)
        }.timeInMillis
        return dueDate < endOfMonth
    }
}
