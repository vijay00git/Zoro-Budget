package com.example.data.utils

import com.example.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class RecurringBillUtilsTest {

    private fun fixedNow(year: Int, month: Int, day: Int): Long {
        // month is 1-indexed here for readability; Calendar.MONTH is 0-indexed.
        val cal = Calendar.getInstance().apply {
            set(year, month - 1, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun monthlyBill(dueDate: Int) = TransactionEntity(
        title = "Rent",
        amount = 1000.0,
        type = "DEBIT",
        accountId = 1L,
        categoryTag = "Bills & Utilities",
        isRecurring = true,
        recurringFrequency = "MONTHLY",
        recurringDueDate = dueDate
    )

    @Test
    fun monthlyBillDueTodayIsUrgent() {
        val now = fixedNow(2026, 3, 15)
        val info = RecurringBillUtils.calculateAlert(monthlyBill(dueDate = 15), nowMillis = now)

        assertEquals(0L, info.daysDifference)
        assertEquals("Due Today", info.alertMessage)
        assertEquals(BillAlertSeverity.URGENT, info.severity)
    }

    @Test
    fun monthlyBillDueTomorrowIsWarning() {
        val now = fixedNow(2026, 3, 15)
        val info = RecurringBillUtils.calculateAlert(monthlyBill(dueDate = 16), nowMillis = now)

        assertEquals(1L, info.daysDifference)
        assertEquals("Due Tomorrow", info.alertMessage)
        assertEquals(BillAlertSeverity.WARNING, info.severity)
    }

    @Test
    fun monthlyBillOverdueByAFewDaysIsUrgent() {
        val now = fixedNow(2026, 3, 15)
        val info = RecurringBillUtils.calculateAlert(monthlyBill(dueDate = 12), nowMillis = now)

        assertEquals(-3L, info.daysDifference)
        assertEquals("Overdue by 3 days", info.alertMessage)
        assertEquals(BillAlertSeverity.URGENT, info.severity)
    }

    @Test
    fun monthlyBillOverdueByMoreThanAWeekRollsToNextMonth() {
        // Due date was the 1st, we're on the 15th - more than 7 days late, so the next
        // occurrence (April 1st) should be shown instead of "overdue".
        val now = fixedNow(2026, 3, 15)
        val info = RecurringBillUtils.calculateAlert(monthlyBill(dueDate = 1), nowMillis = now)

        assertEquals(BillAlertSeverity.INFO, info.severity)
        assertEquals("Apr 1", info.formattedDueDate)
    }

    @Test
    fun monthlyBillClampsDueDateToShorterMonth() {
        // Due date 31 in February (28 days in 2026, not a leap year) should clamp to Feb 28.
        val now = fixedNow(2026, 2, 20)
        val info = RecurringBillUtils.calculateAlert(monthlyBill(dueDate = 31), nowMillis = now)

        assertEquals("Feb 28", info.formattedDueDate)
    }

    @Test
    fun weeklyBillLabelsCorrectDayName() {
        val weeklyBill = TransactionEntity(
            title = "Grocery run",
            amount = 60.0,
            type = "DEBIT",
            accountId = 1L,
            categoryTag = "Groceries",
            isRecurring = true,
            recurringFrequency = "WEEKLY",
            recurringDueDate = 5 // Friday
        )
        val now = fixedNow(2026, 3, 15) // a Sunday
        val info = RecurringBillUtils.calculateAlert(weeklyBill, nowMillis = now)

        assertEquals("Every week on Friday", info.frequencyLabel)
    }

    @Test
    fun yearlyBillMatchesOriginalMonthAndDay() {
        val originalTimestamp = fixedNow(2020, 6, 10)
        val yearlyBill = TransactionEntity(
            title = "Insurance renewal",
            amount = 500.0,
            type = "DEBIT",
            accountId = 1L,
            categoryTag = "Bills & Utilities",
            dateTimestamp = originalTimestamp,
            isRecurring = true,
            recurringFrequency = "YEARLY",
            recurringDueDate = null
        )
        val now = fixedNow(2026, 6, 1)
        val info = RecurringBillUtils.calculateAlert(yearlyBill, nowMillis = now)

        assertEquals("Yearly", info.frequencyLabel)
        assertEquals("Jun 10", info.formattedDueDate)
    }
}
