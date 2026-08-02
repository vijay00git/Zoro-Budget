package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("accountId"),
        Index("dateTimestamp"),
        Index("categoryTag"),
        Index("isPendingReview")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, // User defined title or auto-generated default
    val amount: Double,
    val type: String, // "DEBIT" or "CREDIT"
    val accountId: Long,
    val categoryTag: String, // Food & Dining, Shopping, Bills & Utilities, Salary, etc.
    val dateTimestamp: Long = System.currentTimeMillis(),
    val source: String = "MANUAL", // "SMS_AUTO", "MANUAL", "SIMULATED_SMS"
    val rawSmsMessage: String? = null,
    val merchantOrSender: String? = null,
    val isPendingReview: Boolean = false, // True if user needs to set title/category
    val notes: String? = null,
    val isRecurring: Boolean = false,
    val recurringFrequency: String? = null, // "MONTHLY", "YEARLY", "WEEKLY"
    val recurringDueDate: Int? = null, // Day of month (e.g. 4 for 4th of month) or day of week (1..7)
    val receiptUri: String? = null, // Attachment URI or image path for bill/receipt
    // True once the user deletes an SMS-sourced transaction they don't want. The row is kept
    // (instead of a hard delete) so rawSmsMessage/dateTimestamp still match on the next inbox
    // scan and checkDuplicateSms rejects it - otherwise the same still-present phone SMS gets
    // re-parsed and the "deleted" transaction comes right back.
    val isDismissed: Boolean = false,
    // For isRecurring rows only: epoch millis of the last time this bill was marked paid, so
    // RecurringBillUtils.calculateAlert can tell the current cycle is already settled instead
    // of continuing to show it as due/overdue right after the user paid it.
    val lastPaidTimestamp: Long? = null
)
