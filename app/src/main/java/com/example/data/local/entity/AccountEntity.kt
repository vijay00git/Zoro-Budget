package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // CHECKING, SAVINGS, CREDIT_CARD, CASH, WALLET, INVESTMENT, CUSTOM, or custom string
    val balance: Double,
    val accountNumberLast4: String? = null,
    val colorHex: String = "#2563EB", // Hex color string
    val isDefault: Boolean = false,
    val senderKeywords: String? = null, // e.g. "VD-HDFCBK-T, AD-HDFCBK-S, BV-INDBNK-S, BZ-INDBNK-S"
    val customTypeLabel: String? = null // e.g. "Fixed Deposit", "Crypto Wallet", "PayPal"
)
