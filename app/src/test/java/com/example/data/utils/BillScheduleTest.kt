package com.example.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BillScheduleTest {

    private fun date(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day)
    }.timeInMillis

    @Test
    fun monthlyAdvanceKeepsTheAnchorDayAcrossShortMonths() {
        val jan31 = date(2026, 1, 31)
        val feb = BillSchedule.advance(jan31, BillFrequency.MONTHLY, 31)
        assertEquals(date(2026, 2, 28), feb)
        assertEquals(date(2026, 3, 31), BillSchedule.advance(feb, BillFrequency.MONTHLY, 31))
    }

    @Test
    fun quarterlyYearlyAndWeeklyAdvance() {
        assertEquals(date(2027, 1, 15), BillSchedule.advance(date(2026, 10, 15), BillFrequency.QUARTERLY, 15))
        assertEquals(date(2027, 10, 15), BillSchedule.advance(date(2026, 10, 15), BillFrequency.YEARLY, 15))
        assertEquals(date(2026, 10, 27), BillSchedule.advance(date(2026, 10, 20), BillFrequency.WEEKLY, 2))
    }

    @Test
    fun previousUndoesAdvance() {
        val due = date(2026, 3, 31)
        assertEquals(date(2026, 2, 28), BillSchedule.previous(due, BillFrequency.MONTHLY, 31))
    }

    @Test
    fun paymentNearADueDateSettlesThatCycle() {
        // Paid early (Oct 3) or late (Oct 25) for the Oct 15 bill: next due is Nov 15 either way.
        assertEquals(date(2026, 11, 15), BillSchedule.nextDueAfterPayment(date(2026, 10, 3), BillFrequency.MONTHLY, 15))
        assertEquals(date(2026, 11, 15), BillSchedule.nextDueAfterPayment(date(2026, 10, 25), BillFrequency.MONTHLY, 15))
        assertEquals(date(2026, 11, 15), BillSchedule.nextDueAfterPayment(date(2026, 10, 15), BillFrequency.MONTHLY, 15))
        // A yearly bill paid ten days late still settles that year's due date.
        assertEquals(date(2027, 10, 15), BillSchedule.nextDueAfterPayment(date(2026, 10, 25), BillFrequency.YEARLY, 15))
    }

    @Test
    fun statusAndDueListMembership() {
        val now = date(2026, 10, 20)
        assertEquals(BillStatus.OVERDUE, BillSchedule.status(date(2026, 10, 15), 3, true, now))
        assertEquals(BillStatus.DUE_SOON, BillSchedule.status(date(2026, 10, 22), 3, true, now))
        assertEquals(BillStatus.UPCOMING, BillSchedule.status(date(2026, 10, 28), 3, true, now))
        assertEquals(BillStatus.PAUSED, BillSchedule.status(date(2026, 10, 15), 3, false, now))

        // Overdue and later this month are on the to-pay list ...
        assertTrue(BillSchedule.isDueForPayment(date(2026, 10, 15), 3, true, now))
        assertTrue(BillSchedule.isDueForPayment(date(2026, 10, 30), 3, true, now))
        // ... next month's cycle (what a paid bill moves to) is not, unless it's within a week.
        assertFalse(BillSchedule.isDueForPayment(date(2026, 11, 15), 3, true, now))
        assertTrue(BillSchedule.isDueForPayment(date(2026, 11, 2), 3, true, date(2026, 10, 28)))
        assertFalse(BillSchedule.isDueForPayment(date(2026, 10, 15), 3, false, now))
    }

    @Test
    fun daysUntilCountsCalendarDays() {
        assertEquals(-5, BillSchedule.daysUntil(date(2026, 10, 15), date(2026, 10, 20) + 23L * 3600_000))
        assertEquals(0, BillSchedule.daysUntil(date(2026, 10, 20), date(2026, 10, 20) + 3600_000))
    }
}
