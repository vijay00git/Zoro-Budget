package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringBillEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.utils.CategoryTotals
import com.example.data.utils.FinancialHealth
import com.example.ui.components.BalanceHeroCard
import com.example.ui.components.BillsDueCard
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.CategorySpendingCard
import com.example.ui.components.FinancialHealthCard
import com.example.ui.components.FinancialHealthDialog
import com.example.ui.components.LowBalanceCard
import com.example.ui.components.MonthSelector
import com.example.ui.components.MonthSummaryCard
import com.example.ui.components.MonthlySpendingTrendChart
import com.example.ui.components.PendingReviewCard
import com.example.ui.components.SpendingHeatmapCard
import com.example.ui.components.lowBalanceAccounts
import com.example.ui.components.monthWindow
import com.example.ui.components.startOfCurrentMonth

/**
 * Overview, top to bottom: what you have (balance, accounts, quick actions), what needs doing
 * (SMS to review, low balances, bills to pay), then the selected month - income vs spending,
 * spending by category, a health score and charts. The month defaults to the current one.
 */
@Composable
fun HomeScreen(
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    pendingTransactions: List<TransactionEntity>,
    monthOffset: Int = 0,
    onMonthOffsetChange: (Int) -> Unit = {},
    categories: List<CategoryEntity> = emptyList(),
    currencySymbol: String = "₹",
    lowBalanceThreshold: Double = 200.0,
    selectedAccountId: Long? = null,
    onSelectAccount: (Long?) -> Unit = {},
    onAddAccountClick: () -> Unit = {},
    onAddRecurringBillClick: () -> Unit = {},
    onTransferClick: (Long?) -> Unit = {},
    onAddMoneyClick: (AccountEntity) -> Unit = {},
    onChangeLowBalanceAlert: () -> Unit = {},
    onPendingReviewClick: () -> Unit,
    onCategoryClick: (String) -> Unit = {},
    onTransactionClick: (TransactionEntity) -> Unit = {},
    onAddTransactionClick: () -> Unit = {},
    bills: List<RecurringBillEntity> = emptyList(),
    onBillClick: (RecurringBillEntity) -> Unit = {},
    onMarkBillPaid: (RecurringBillEntity) -> Unit = {},
    onSeeAllBills: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val displayTransactions = remember(transactions, selectedAccountId) {
        if (selectedAccountId != null) transactions.filter { it.accountId == selectedAccountId } else transactions
    }
    val monthTransactions = remember(displayTransactions, monthOffset) {
        val (start, end) = monthWindow(monthOffset)
        displayTransactions.filter { it.dateTimestamp in start until end }
    }
    // Netted per category, transfers excluded - see CategoryTotals
    val monthTotals = remember(monthTransactions) { CategoryTotals.of(monthTransactions) }
    val lowAccounts = remember(accounts, lowBalanceThreshold) { lowBalanceAccounts(accounts, lowBalanceThreshold) }
    val monthlyBudget = remember(categories) { categories.filter { !it.isIncomeCategory }.sumOf { it.monthlyBudgetLimit } }
    val health = remember(monthTotals, monthlyBudget, lowAccounts) {
        FinancialHealth.of(monthTotals.expense, monthTotals.income, monthlyBudget, lowAccounts.size)
    }
    val paidThisMonth = remember(transactions) {
        val monthStart = startOfCurrentMonth()
        transactions.filter { it.recurringBillId != null && it.type == "DEBIT" && it.dateTimestamp >= monthStart }
    }
    var showHealthDialog by rememberSaveable { mutableStateOf(false) }
    val cardPadding = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("home_screen"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
    ) {
        item {
            BalanceHeroCard(
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                currencySymbol = currencySymbol,
                onSelectAccount = onSelectAccount,
                onAddTransaction = onAddTransactionClick,
                onTransfer = { onTransferClick(selectedAccountId) },
                onAddAccount = onAddAccountClick,
                modifier = cardPadding
            )
        }

        // --- Needs attention ---
        if (pendingTransactions.isNotEmpty()) {
            item {
                PendingReviewCard(count = pendingTransactions.size, onReview = onPendingReviewClick, modifier = cardPadding)
            }
        }
        if (lowAccounts.isNotEmpty()) {
            item {
                LowBalanceCard(
                    lowAccounts = lowAccounts,
                    threshold = lowBalanceThreshold,
                    currencySymbol = currencySymbol,
                    onAddMoney = onAddMoneyClick,
                    onChangeAlertLevel = onChangeLowBalanceAlert,
                    modifier = cardPadding
                )
            }
        }
        item {
            BillsDueCard(
                bills = bills,
                accounts = accounts,
                paidThisMonthCount = paidThisMonth.size,
                paidThisMonthAmount = paidThisMonth.sumOf { it.amount },
                currencySymbol = currencySymbol,
                onBillClick = onBillClick,
                onMarkPaid = onMarkBillPaid,
                onAddBill = onAddRecurringBillClick,
                onSeeAllBills = onSeeAllBills,
                modifier = cardPadding
            )
        }

        // --- The selected month ---
        item {
            MonthSelector(
                monthOffset = monthOffset,
                onMonthOffsetChange = onMonthOffsetChange,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 14.dp, bottom = 6.dp)
            )
        }
        item { MonthSummaryCard(totals = monthTotals, currencySymbol = currencySymbol, modifier = cardPadding) }
        item {
            CategorySpendingCard(
                categories = categories,
                totals = monthTotals,
                currencySymbol = currencySymbol,
                onCategoryClick = onCategoryClick,
                modifier = cardPadding
            )
        }
        item { FinancialHealthCard(health = health, onClick = { showHealthDialog = true }, modifier = cardPadding) }

        item {
            var selectedChartTab by rememberSaveable { mutableIntStateOf(0) } // 0: Trends, 1: Categories
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = stringResource(R.string.home_screen_visual_analytics_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    listOf(
                        R.string.home_screen_chart_tab_trends,
                        R.string.home_screen_chart_tab_categories
                    ).forEachIndexed { index, label ->
                        FilterChip(
                            selected = selectedChartTab == index,
                            onClick = { selectedChartTab = index },
                            label = { Text(stringResource(label)) }
                        )
                    }
                }
                when (selectedChartTab) {
                    0 -> MonthlySpendingTrendChart(
                        transactions = displayTransactions.filter { it.categoryTag != CategoryTotals.TRANSFER_CATEGORY },
                        categories = categories,
                        currencySymbol = currencySymbol
                    )
                    else -> CategoryDonutChart(transactions = monthTransactions, categories = categories, currencySymbol = currencySymbol)
                }
            }
        }

        // Daily spending calendar for the selected month (replaces the old daily bar chart)
        item {
            SpendingHeatmapCard(
                monthOffset = monthOffset,
                monthTransactions = monthTransactions,
                currencySymbol = currencySymbol,
                onTransactionClick = onTransactionClick,
                modifier = cardPadding
            )
        }
    }

    if (showHealthDialog) {
        FinancialHealthDialog(health = health, currencySymbol = currencySymbol, onDismiss = { showHealthDialog = false })
    }
}
