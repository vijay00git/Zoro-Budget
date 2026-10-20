package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One saving or asset on the Savings page (an FD, a stock holding, a plot...). It is a record
 * only: it never moves money in the ledger. Fields every section shares are columns; the
 * section-specific inputs live in [detailsJson] (see SavingsDetails) so new fields don't need a
 * schema migration. [investedAmount] and [currentValue] are cached for list totals and sorting.
 */
@Entity(tableName = "savings_items", indices = [Index("section"), Index("holder")])
data class SavingsItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val section: String, // SavingsSection: FD, RD, STOCK, MUTUAL_FUND, GOLD, SILVER, LAND, OTHER
    val subType: String? = null, // OtherKind for OTHER, GoldForm for GOLD...
    val name: String,
    val holder: String,
    val institution: String? = null,
    val refNo: String? = null,
    val nominee: String? = null,
    val startDate: Long,
    val maturityDate: Long? = null,
    val status: String = "ACTIVE", // SavingsStatus: ACTIVE, MATURED, CLOSED, SOLD
    val notes: String? = null,
    val investedAmount: Double = 0.0,
    val currentValue: Double = 0.0,
    val detailsJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
