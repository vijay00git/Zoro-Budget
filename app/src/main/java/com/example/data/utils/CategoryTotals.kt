package com.example.data.utils

import com.example.data.local.entity.TransactionEntity

/**
 * Spending and income for a set of transactions, with money in and out of the same category
 * netted: ₹1,000 spent on Food and ₹800 received back for Food (a friend's share, a refund) is
 * ₹200 of Food spending, not ₹1,000 spent plus ₹800 income.
 *
 * Each category nets to either spending (more went out than came in) or income (more came in,
 * e.g. Salary). Transfers between the user's own accounts are neither and are ignored, so income
 * minus expense always equals the real cash flow.
 */
data class CategoryTotals(
    val income: Double,
    val expense: Double,
    // Net spending per category name (as written on the transactions), only where it's above zero
    val spendingByCategory: Map<String, Double>
) {
    val net: Double get() = income - expense

    fun spentIn(categoryName: String): Double =
        spendingByCategory.entries.firstOrNull { it.key.equals(categoryName, ignoreCase = true) }?.value ?: 0.0

    companion object {
        const val TRANSFER_CATEGORY = "Transfer"

        fun of(transactions: List<TransactionEntity>): CategoryTotals {
            var income = 0.0
            var expense = 0.0
            val spending = mutableMapOf<String, Double>()

            transactions
                .filter { !it.categoryTag.equals(TRANSFER_CATEGORY, ignoreCase = true) }
                .groupBy { it.categoryTag.lowercase() }
                .forEach { (_, txs) ->
                    val debits = txs.filter { it.type == "DEBIT" }.sumOf { it.amount }
                    val credits = txs.filter { it.type == "CREDIT" }.sumOf { it.amount }
                    val outflow = debits - credits
                    if (outflow > 0) {
                        expense += outflow
                        spending[txs.first().categoryTag] = outflow
                    } else {
                        income += -outflow
                    }
                }
            return CategoryTotals(income, expense, spending)
        }
    }
}
