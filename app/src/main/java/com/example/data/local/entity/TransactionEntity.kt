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
        Index("isPendingReview"),
        Index("recurringBillId")
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
    // Legacy (DB v8 and earlier): bills used to be transactions flagged recurring. Migration 8->9
    // moves them into recurring_bills and clears the flag; nothing reads these any more.
    val isRecurring: Boolean = false,
    val recurringFrequency: String? = null,
    val recurringDueDate: Int? = null,
    val receiptUri: String? = null, // Attachment URI or image path for bill/receipt
    // True once the user deletes an SMS-sourced transaction they don't want. The row is kept
    // (instead of a hard delete) so rawSmsMessage still matches on the next inbox scan and
    // checkDuplicateSms rejects it - otherwise the same still-present phone SMS gets re-parsed
    // and the "deleted" transaction comes right back.
    val isDismissed: Boolean = false,
    // Legacy, see isRecurring.
    val lastPaidTimestamp: Long? = null,
    // For SMS-sourced rows: the sender code ("HDFCBK") and account/card digits the SMS named.
    // Kept so the review flow can spot a bank or card that no account is linked to yet, and
    // teach the chosen account to recognise it next time.
    val smsSender: String? = null,
    val smsAccountLast4: String? = null,
    // The recurring bill this transaction paid, if any (RecurringBillEntity.id).
    val recurringBillId: Long? = null
)
