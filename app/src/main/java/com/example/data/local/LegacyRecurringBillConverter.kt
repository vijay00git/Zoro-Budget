package com.example.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.utils.BillFrequency
import com.example.data.utils.BillSchedule
import java.util.Calendar

/**
 * Part of migration 8 -> 9. Bills used to be transactions flagged isRecurring; this turns each
 * one into a recurring_bills row. Balances are left alone: the flagged row already moved money
 * when it was created, so it stays in the ledger as the bill's first payment, together with
 * earlier/later payments to the same payee for the same amount.
 */
object LegacyRecurringBillConverter {

    private data class LegacyRow(
        val id: Long,
        val title: String,
        val amount: Double,
        val type: String,
        val accountId: Long,
        val categoryTag: String,
        val frequency: String?,
        val dueDay: Int?,
        val dateTimestamp: Long,
        val lastPaidTimestamp: Long?,
        val notes: String?,
        val merchant: String?
    )

    fun convert(db: SupportSQLiteDatabase, now: Long = System.currentTimeMillis()) {
        val rows = mutableListOf<LegacyRow>()
        db.query(
            "SELECT id, title, amount, type, accountId, categoryTag, recurringFrequency, recurringDueDate, " +
                "dateTimestamp, lastPaidTimestamp, notes, merchantOrSender FROM transactions " +
                "WHERE isRecurring = 1 AND isDismissed = 0 ORDER BY dateTimestamp ASC"
        ).use { c ->
            while (c.moveToNext()) {
                rows += LegacyRow(
                    id = c.getLong(0),
                    title = c.getString(1),
                    amount = c.getDouble(2),
                    type = c.getString(3),
                    accountId = c.getLong(4),
                    categoryTag = c.getString(5),
                    frequency = if (c.isNull(6)) null else c.getString(6),
                    dueDay = if (c.isNull(7)) null else c.getInt(7),
                    dateTimestamp = c.getLong(8),
                    lastPaidTimestamp = if (c.isNull(9)) null else c.getLong(9),
                    notes = if (c.isNull(10)) null else c.getString(10),
                    merchant = if (c.isNull(11)) null else c.getString(11)
                )
            }
        }

        // The old UI showed one bill per title, so several flagged rows with the same title are
        // one bill with several payments.
        for ((_, group) in rows.groupBy { it.title.trim().lowercase() }) {
            val first = group.first()
            val frequency = BillFrequency.from(first.frequency)
            val dueDay = first.dueDay ?: defaultDueDay(first.dateTimestamp, frequency)
            val payee = first.merchant?.takeIf { it.isNotBlank() && !it.startsWith("SMS:") }

            val billId = db.insert(
                "recurring_bills",
                SQLiteDatabase.CONFLICT_ABORT,
                ContentValues().apply {
                    put("name", first.title.trim())
                    put("amount", first.amount)
                    put("type", first.type)
                    put("accountId", first.accountId)
                    put("categoryTag", first.categoryTag)
                    put("frequency", frequency.name)
                    put("nextDueDate", 0L) // set below, once the payments are linked
                    put("dueDay", dueDay)
                    put("reminderDaysBefore", 3)
                    put("isActive", 1)
                    // The old SMS rows carry an "Auto-imported from SMS" note that says nothing about the bill
                    put("notes", first.notes?.takeUnless { it.isBlank() || it == "Auto-imported from SMS" })
                    put("matchPayee", payee)
                    put("createdTimestamp", first.dateTimestamp)
                }
            )

            group.forEach { row ->
                db.execSQL("UPDATE transactions SET recurringBillId = ? WHERE id = ?", arrayOf<Any>(billId, row.id))
            }
            // Other payments of the exact amount to the same payee, or to a payee containing a word
            // of the bill's name ("AIRTEL BLACK" vs "Airtel Payments Bank") - the same rule live
            // SMS matching uses.
            val payeePatterns = (listOfNotNull(payee) + nameWords(first.title).map { "%$it%" }).distinct()
            payeePatterns.forEach { pattern ->
                db.execSQL(
                    "UPDATE transactions SET recurringBillId = ? WHERE recurringBillId IS NULL AND isDismissed = 0 " +
                        "AND type = ? AND ABS(amount - ?) < 0.005 AND merchantOrSender LIKE ?",
                    arrayOf<Any>(billId, first.type, first.amount, pattern)
                )
            }

            var lastPayment = group.mapNotNull { it.lastPaidTimestamp }.maxOrNull() ?: 0L
            db.query("SELECT MAX(dateTimestamp) FROM transactions WHERE recurringBillId = ?", arrayOf<Any>(billId)).use { c ->
                if (c.moveToFirst() && !c.isNull(0)) lastPayment = maxOf(lastPayment, c.getLong(0))
            }
            var nextDue = if (lastPayment > 0L) {
                BillSchedule.nextDueAfterPayment(lastPayment, frequency, dueDay)
            } else {
                BillSchedule.firstOccurrenceOnOrAfter(now, frequency, dueDay)
            }
            // The old model never recorded skipped months, so start no more than one cycle behind:
            // the most recent missed due date stays overdue, older ones are dropped.
            while (BillSchedule.advance(nextDue, frequency, dueDay) <= now) {
                nextDue = BillSchedule.advance(nextDue, frequency, dueDay)
            }
            db.execSQL(
                "UPDATE recurring_bills SET nextDueDate = ?, lastPaidTimestamp = ? WHERE id = ?",
                arrayOf<Any?>(nextDue, lastPayment.takeIf { it > 0L }, billId)
            )
        }

        db.execSQL("UPDATE transactions SET isRecurring = 0, recurringFrequency = NULL, recurringDueDate = NULL WHERE isRecurring = 1")
    }

    private fun nameWords(name: String): List<String> =
        name.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length >= 4 }

    private fun defaultDueDay(timestamp: Long, frequency: BillFrequency): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return if (frequency == BillFrequency.WEEKLY) {
            // ISO day of week: Monday = 1 .. Sunday = 7
            (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
        } else {
            cal.get(Calendar.DAY_OF_MONTH)
        }
    }
}
