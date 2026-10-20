package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A dated line in a savings record's investment history: money put in, taken out, or earned
 * (interest, dividend, rent). The record's invested amount is the sum of these.
 */
@Entity(
    tableName = "savings_entries",
    foreignKeys = [
        ForeignKey(entity = SavingsItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("itemId")]
)
data class SavingsEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val date: Long,
    val type: String = "INVEST", // EntryType: INVEST, WITHDRAW, INCOME
    val amount: Double,
    val units: Double? = null, // shares, MF units or grams bought/sold in this entry
    val unitPrice: Double? = null,
    val note: String? = null
)
