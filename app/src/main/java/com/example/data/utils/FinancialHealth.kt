package com.example.data.utils

/**
 * The Overview "financial health" score (10-100) and the reasons behind it, so the UI can show
 * the user exactly how their number was worked out.
 *
 * Starts at 100. Spending is compared with the monthly budget (or with income when no budget is
 * set): over 60% costs 10 points, over 80% costs 20, over 100% costs 40. Each account below the
 * low-balance alert level costs 15 more. The score never drops below 10.
 */
data class FinancialHealth(
    val score: Int,
    val spent: Double,
    val comparedWith: Double, // the budget or income the spending was measured against; 0 = neither
    val comparedWithBudget: Boolean,
    val spendingPenalty: Int,
    val lowBalanceAccounts: Int,
    val lowBalancePenalty: Int
) {
    val spendRatio: Double get() = if (comparedWith > 0) spent / comparedWith else 0.0

    val level: Level
        get() = when {
            score >= 80 -> Level.EXCELLENT
            score >= 60 -> Level.GOOD
            score >= 40 -> Level.CAUTION
            else -> Level.CRITICAL
        }

    enum class Level { EXCELLENT, GOOD, CAUTION, CRITICAL }

    companion object {
        const val LOW_BALANCE_PENALTY = 15

        fun of(spent: Double, income: Double, monthlyBudget: Double, lowBalanceAccounts: Int): FinancialHealth {
            val comparedWithBudget = monthlyBudget > 0
            val base = if (comparedWithBudget) monthlyBudget else income
            val ratio = if (base > 0) spent / base else 0.0
            val spendingPenalty = when {
                base <= 0 -> 0
                ratio > 1.0 -> 40
                ratio > 0.8 -> 20
                ratio > 0.6 -> 10
                else -> 0
            }
            val lowPenalty = lowBalanceAccounts * LOW_BALANCE_PENALTY
            return FinancialHealth(
                score = (100 - spendingPenalty - lowPenalty).coerceIn(10, 100),
                spent = spent,
                comparedWith = base,
                comparedWithBudget = comparedWithBudget,
                spendingPenalty = spendingPenalty,
                lowBalanceAccounts = lowBalanceAccounts,
                lowBalancePenalty = lowPenalty
            )
        }
    }
}
