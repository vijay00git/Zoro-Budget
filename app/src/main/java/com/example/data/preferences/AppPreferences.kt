package com.example.data.preferences

import android.content.Context
import androidx.core.content.edit

data class AppPreferences(
    val currencySymbol: String = "$",
    val savingsGoalTarget: Double = 500.0,
    val lowBalanceThreshold: Double = 200.0,
    val themeMode: String = "SYSTEM"
) {
    companion object {
        private const val PREFS_NAME = "smart_budget_prefs"
        private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        private const val KEY_SAVINGS_GOAL_TARGET = "savings_goal_target"
        private const val KEY_LOW_BALANCE_THRESHOLD = "low_balance_threshold"
        private const val KEY_THEME_MODE = "theme_mode"

        fun load(context: Context): AppPreferences {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return AppPreferences(
                currencySymbol = prefs.getString(KEY_CURRENCY_SYMBOL, "$") ?: "$",
                savingsGoalTarget = prefs.getFloat(KEY_SAVINGS_GOAL_TARGET, 500f).toDouble(),
                lowBalanceThreshold = prefs.getFloat(KEY_LOW_BALANCE_THRESHOLD, 200f).toDouble(),
                themeMode = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
            )
        }

        fun save(
            context: Context,
            currencySymbol: String? = null,
            savingsGoalTarget: Double? = null,
            lowBalanceThreshold: Double? = null,
            themeMode: String? = null
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit {
                if (currencySymbol != null) putString(KEY_CURRENCY_SYMBOL, currencySymbol)
                if (savingsGoalTarget != null) putFloat(KEY_SAVINGS_GOAL_TARGET, savingsGoalTarget.toFloat())
                if (lowBalanceThreshold != null) putFloat(KEY_LOW_BALANCE_THRESHOLD, lowBalanceThreshold.toFloat())
                if (themeMode != null) putString(KEY_THEME_MODE, themeMode)
            }
        }
    }
}
