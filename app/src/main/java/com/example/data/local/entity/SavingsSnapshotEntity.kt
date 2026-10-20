package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A value recorded at a point in time, for the Savings trend lines: a record's current value
 * ([itemId] set, key ITEM_VALUE) or a shared market rate ([itemId] null, key GOLD_24K,
 * GOLD_22K or SILVER_KG). The latest rate row is the rate in use.
 */
@Entity(
    tableName = "savings_snapshots",
    foreignKeys = [
        ForeignKey(entity = SavingsItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("itemId"), Index("key")]
)
data class SavingsSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long? = null,
    val key: String,
    val value: Double,
    val date: Long
)
