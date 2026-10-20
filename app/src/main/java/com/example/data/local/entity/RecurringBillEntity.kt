package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A bill or subscription that repeats (rent, EMI, phone, Netflix...). Unlike a transaction it
 * doesn't move money: each payment is a normal ledger transaction linked back through
 * [TransactionEntity.recurringBillId], and paying or skipping moves [nextDueDate] on a cycle.
 */
@Entity(tableName = "recurring_bills", indices = [Index("accountId")])
data class RecurringBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val type: String = "DEBIT", // "DEBIT" for bills you pay, "CREDIT" for regular income like rent received
    val accountId: Long,
    val categoryTag: String,
    val frequency: String = "MONTHLY", // BillFrequency: WEEKLY, MONTHLY, QUARTERLY, YEARLY
    val nextDueDate: Long, // start-of-day millis of the next unpaid cycle
    val dueDay: Int, // anchor day of month (1..31) the due date returns to after short months
    val reminderDaysBefore: Int = 3,
    val isActive: Boolean = true, // false = paused: kept with its history but not shown as due
    val notes: String? = null,
    // Payee as it appears in bank SMS ("Airtel"), learned when a payment is linked, so later SMS
    // payments to the same payee for the same amount mark the bill paid on their own.
    val matchPayee: String? = null,
    val lastPaidTimestamp: Long? = null,
    val createdTimestamp: Long = System.currentTimeMillis()
)
