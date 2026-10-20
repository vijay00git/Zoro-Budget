package com.example.data.utils

import com.example.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryTotalsTest {

    private fun tx(amount: Double, type: String, category: String) =
        TransactionEntity(title = "t", amount = amount, type = type, accountId = 1, categoryTag = category)

    @Test
    fun incomeInAnExpenseCategoryReducesThatCategorysSpending() {
        val totals = CategoryTotals.of(listOf(tx(1000.0, "DEBIT", "Food & Dining"), tx(800.0, "CREDIT", "Food & Dining")))
        assertEquals(200.0, totals.spentIn("Food & Dining"), 0.001)
        assertEquals(200.0, totals.expense, 0.001)
        assertEquals(0.0, totals.income, 0.001)
    }

    @Test
    fun moreReceivedThanSpentCountsAsIncome() {
        val totals = CategoryTotals.of(listOf(tx(300.0, "DEBIT", "Vijay"), tx(500.0, "CREDIT", "Vijay")))
        assertEquals(0.0, totals.spentIn("Vijay"), 0.001)
        assertEquals(200.0, totals.income, 0.001)
    }

    @Test
    fun netCashFlowIsUnchangedAndTransfersAreIgnored() {
        val txs = listOf(
            tx(50_000.0, "CREDIT", "Salary & Income"),
            tx(1_000.0, "DEBIT", "Groceries"),
            tx(400.0, "CREDIT", "Groceries"),
            tx(5_000.0, "DEBIT", "Transfer"),
            tx(5_000.0, "CREDIT", "Transfer")
        )
        val totals = CategoryTotals.of(txs)
        assertEquals(50_000.0, totals.income, 0.001)
        assertEquals(600.0, totals.expense, 0.001)
        assertEquals(50_000.0 - 1_000.0 + 400.0, totals.net, 0.001)
    }

    @Test
    fun categoryNamesMatchRegardlessOfCase() {
        val totals = CategoryTotals.of(listOf(tx(100.0, "DEBIT", "snacks"), tx(50.0, "DEBIT", "Snacks")))
        assertEquals(150.0, totals.spentIn("SNACKS"), 0.001)
    }
}
