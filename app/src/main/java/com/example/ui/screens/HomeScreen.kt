package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringBillEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.utils.CategoryTotals
import com.example.data.utils.BillSchedule
import com.example.data.utils.FinancialHealth
import com.example.ui.components.AccountTile
import com.example.ui.components.AddAccountTile
import com.example.ui.components.BillsDueCard
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.CategorySpendingCard
import com.example.ui.components.HealthTile
import com.example.ui.components.LowBalanceTile
import com.example.ui.components.FinancialHealthDialog
import com.example.ui.components.LowBalanceCard
import com.example.ui.components.MonthSelector
import com.example.ui.components.MonthRingTile
import com.example.ui.components.BillsTile
import com.example.ui.components.ReviewTile
import com.example.ui.components.HeroBalanceTile
import com.example.ui.components.MonthlySpendingTrendChart
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
    balancesHidden: Boolean = true,
    onToggleBalances: () -> Unit = {},
    accountsExpanded: Boolean = false,
    onToggleAccounts: () -> Unit = {},
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
    var showLowBalance by rememberSaveable { mutableStateOf(false) }
    val cardPadding = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("home_screen"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
    ) {
        // --- Bento grid: balance, accounts, this month at a glance ---
        item {
            HeroBalanceTile(
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                currencySymbol = currencySymbol,
                onShowAll = { onSelectAccount(null) },
                onAddTransaction = onAddTransactionClick,
                onTransfer = { onTransferClick(selectedAccountId) },
                balancesHidden = balancesHidden,
                onToggleBalances = onToggleBalances,
                accountsExpanded = accountsExpanded,
                onToggleAccounts = onToggleAccounts,
                modifier = cardPadding
            )
        }
        // Account tiles only when opened from the balance tile. Accounts below their alert
        // level appear in the low-balance card instead.
        val tileAccounts = accounts.filter { it !in lowAccounts }
        val accountRows = if (accountsExpanded) (tileAccounts.map<AccountEntity, AccountEntity?> { it } + null).chunked(2) else emptyList()
        items(accountRows.size) { index ->
            Row(modifier = cardPadding.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                accountRows[index].forEach { account ->
                    if (account == null) {
                        AddAccountTile(onClick = onAddAccountClick, modifier = Modifier.weight(1f))
                    } else {
                        AccountTile(
                            account = account,
                            selected = selectedAccountId == account.id,
                            currencySymbol = currencySymbol,
                            onClick = { onSelectAccount(if (selectedAccountId == account.id) null else account.id) },
                            modifier = Modifier.weight(1f),
                            hidden = balancesHidden
                        )
                    }
                }
                if (accountRows[index].size == 1) Spacer(Modifier.weight(1f))
            }
        }
        // Three small tiles: low balance, SMS review, financial health.
        item {
            Row(modifier = cardPadding.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LowBalanceTile(lowAccounts = lowAccounts, onClick = { showLowBalance = true }, modifier = Modifier.weight(1f).fillMaxHeight())
                ReviewTile(pendingCount = pendingTransactions.size, onClick = onPendingReviewClick, modifier = Modifier.weight(1f).fillMaxHeight())
                HealthTile(health = health, onClick = { showHealthDialog = true }, modifier = Modifier.weight(1f).fillMaxHeight())
            }
        }
        // Bills that need paying get the full card with its Mark paid buttons.
        if (bills.any { BillSchedule.isDueForPayment(it.nextDueDate, it.reminderDaysBefore, it.isActive) }) {
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
        }

        // --- The selected month ---
        item {
            MonthSelector(
                monthOffset = monthOffset,
                onMonthOffsetChange = onMonthOffsetChange,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 10.dp, bottom = 2.dp)
            )
        }
        item {
            Row(modifier = cardPadding.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MonthRingTile(
                    monthOffset = monthOffset,
                    totals = monthTotals,
                    monthlyBudget = monthlyBudget,
                    currencySymbol = currencySymbol,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                BillsTile(
                    bills = bills,
                    paidThisMonthCount = paidThisMonth.size,
                    paidThisMonthAmount = paidThisMonth.sumOf { it.amount },
                    currencySymbol = currencySymbol,
                    onClick = onSeeAllBills,
                    onAddBill = onAddRecurringBillClick,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
        item {
            CategorySpendingCard(
                categories = categories,
                totals = monthTotals,
                currencySymbol = currencySymbol,
                onCategoryClick = onCategoryClick,
                modifier = cardPadding
            )
        }

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

    if (showLowBalance && lowAccounts.isNotEmpty()) {
        Dialog(onDismissRequest = { showLowBalance = false }) {
            // The card's own colour is see-through, so give it a solid backing in the pop-up.
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            LowBalanceCard(
                lowAccounts = lowAccounts,
                threshold = lowBalanceThreshold,
                currencySymbol = currencySymbol,
                onAddMoney = {
                    showLowBalance = false
                    onAddMoneyClick(it)
                },
                onChangeAlertLevel = {
                    showLowBalance = false
                    onChangeLowBalanceAlert()
                },
                hideAmounts = balancesHidden
            )
            }
        }
    }

    if (showHealthDialog) {
        FinancialHealthDialog(health = health, currencySymbol = currencySymbol, onDismiss = { showHealthDialog = false })
    }
}
