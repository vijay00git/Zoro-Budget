package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String, // Material icon identifier
    val colorHex: String,
    val isPredefined: Boolean = true,
    val monthlyBudgetLimit: Double = 0.0,
    // True for income/savings categories (e.g. "Salary & Income", "Investment & Savings") that
    // shouldn't receive a share when the user sets an overall monthly expense budget.
    val isIncomeCategory: Boolean = false
)
