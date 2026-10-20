package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.components.AllBillsSection
import com.example.ui.components.BudgetRingTile
import com.example.ui.components.CashFlowHeroTile
import com.example.ui.components.CategoryBudgetTile
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.CountTile
import com.example.ui.components.MonthSelector
import com.example.ui.components.MonthlyBarChart
import com.example.ui.components.MonthlySpendingTrendChart
import com.example.ui.components.NewCategoryTile
import com.example.ui.components.SavingsGoalTile
import com.example.ui.components.monthWindow
import com.example.ui.dialogs.EditSavingsGoalDialog
import com.example.ui.dialogs.TotalBudgetDialog

/** Which categories the Planning grid shows. */
private enum class BudgetFilter { ALL, OVER, NEAR, NO_LIMIT }

/**
 * The Planning tab as a bento grid: the month's cash flow, the overall budget and the savings
 * goal at a glance, then every category as a coloured tile (problems first), then bills and
 * charts. Spending is netted per category and transfers are left out, as on the Overview.
 */
@Composable
fun ReportsScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    selectedMonthOffset: Int,
    currencySymbol: String = "$",
    savingsGoalTarget: Double = 500.0,
    onMonthOffsetChange: (Int) -> Unit,
    onEditCategoryBudget: (CategoryEntity) -> Unit,
    onAddCustomCategoryClick: () -> Unit,
    bills: List<RecurringBillEntity> = emptyList(),
    accounts: List<AccountEntity> = emptyList(),
    onBillClick: (RecurringBillEntity) -> Unit = {},
    onMarkBillPaid: (RecurringBillEntity) -> Unit = {},
    onAddRecurringBillClick: () -> Unit = {},
    onTransactionClick: (TransactionEntity) -> Unit,
    onUpdateSavingsGoalTarget: (Double) -> Unit,
    onSetTotalMonthlyBudget: (Double) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var filter by rememberSaveable { mutableStateOf(BudgetFilter.ALL) }
    var editingGoal by rememberSaveable { mutableStateOf(false) }
    var editingTotalBudget by rememberSaveable { mutableStateOf(false) }

    val monthTx = remember(transactions, selectedMonthOffset) {
        val (start, end) = monthWindow(selectedMonthOffset)
        transactions.filter { it.dateTimestamp in start until end }
    }
    val totals = remember(monthTx) { CategoryTotals.of(monthTx) }
    // Income and savings categories (e.g. "Salary & Income") are not spending budgets.
    val spendingCategories = remember(categories) { categories.filter { !it.isIncomeCategory } }
    val spentIn = remember(totals, categories) { categories.associate { it.name to totals.spentIn(it.name) } }
    val budgetLimit = spendingCategories.sumOf { it.monthlyBudgetLimit }
    val budgetSpent = spendingCategories.filter { it.monthlyBudgetLimit > 0 }.sumOf { spentIn[it.name] ?: 0.0 }

    fun ratioOf(c: CategoryEntity) = if (c.monthlyBudgetLimit > 0) (spentIn[c.name] ?: 0.0) / c.monthlyBudgetLimit else 0.0
    val over = spendingCategories.filter { it.monthlyBudgetLimit > 0 && ratioOf(it) > 1.0 }
    val near = spendingCategories.filter { it.monthlyBudgetLimit > 0 && ratioOf(it) in 0.8..1.0 }
    val noLimit = spendingCategories.filter { it.monthlyBudgetLimit <= 0 }

    // Problems first: over budget by how far over, then near the limit, then by amount spent.
    val shown = when (filter) {
        BudgetFilter.ALL -> spendingCategories
        BudgetFilter.OVER -> over
        BudgetFilter.NEAR -> near
        BudgetFilter.NO_LIMIT -> noLimit
    }.sortedWith(
        compareByDescending<CategoryEntity> { if (it.monthlyBudgetLimit > 0 && ratioOf(it) > 1.0) 2 else if (it.monthlyBudgetLimit > 0 && ratioOf(it) >= 0.8) 1 else 0 }
            .thenByDescending { ratioOf(it) }
            .thenByDescending { spentIn[it.name] ?: 0.0 }
    )
    val tileRows = (shown.map<CategoryEntity, CategoryEntity?> { it } + (if (filter == BudgetFilter.ALL) listOf(null) else emptyList())).chunked(2)
    val pad = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("reports_screen"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)
    ) {
        item {
            Text(
                stringResource(R.string.reports_screen_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        item { MonthSelector(monthOffset = selectedMonthOffset, onMonthOffsetChange = onMonthOffsetChange, modifier = pad) }
        item { CashFlowHeroTile(income = totals.income, expense = totals.expense, currencySymbol = currencySymbol, modifier = pad) }
        item {
            Row(modifier = pad.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BudgetRingTile(
                    spent = budgetSpent,
                    limit = budgetLimit,
                    currencySymbol = currencySymbol,
                    onSetBudget = { editingTotalBudget = true },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                SavingsGoalTile(
                    saved = totals.income - totals.expense,
                    target = savingsGoalTarget,
                    currencySymbol = currencySymbol,
                    onEdit = { editingGoal = true },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
        // Counts that also filter the grid below; tapping the active one shows everything again.
        item {
            Row(modifier = pad.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple(BudgetFilter.OVER, over.size, R.string.planning_count_over) to "#991B1B",
                    Triple(BudgetFilter.NEAR, near.size, R.string.planning_count_near) to "#854D0E",
                    Triple(BudgetFilter.NO_LIMIT, noLimit.size, R.string.planning_count_no_limit) to "#334155"
                ).forEach { (spec, hex) ->
                    CountTile(
                        count = spec.second,
                        label = stringResource(spec.third),
                        hex = hex,
                        selected = filter == spec.first,
                        onClick = { filter = if (filter == spec.first) BudgetFilter.ALL else spec.first },
                        modifier = Modifier.weight(1f).fillMaxHeight().testTag("planning_filter_${spec.first.name}")
                    )
                }
            }
        }
        item {
            Text(
                stringResource(
                    when (filter) {
                        BudgetFilter.ALL -> R.string.planning_categories_all
                        BudgetFilter.OVER -> R.string.planning_categories_over
                        BudgetFilter.NEAR -> R.string.planning_categories_near
                        BudgetFilter.NO_LIMIT -> R.string.planning_categories_no_limit
                    },
                    shown.size
                ),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
            )
        }
        if (shown.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.reports_screen_no_categories_match_filter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = pad
                )
            }
        }
        items(tileRows.size) { index ->
            Row(modifier = pad.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                tileRows[index].forEach { category ->
                    if (category == null) {
                        NewCategoryTile(onClick = onAddCustomCategoryClick, modifier = Modifier.weight(1f).fillMaxHeight())
                    } else {
                        CategoryBudgetTile(
                            category = category,
                            spent = spentIn[category.name] ?: 0.0,
                            currencySymbol = currencySymbol,
                            onClick = { onEditCategoryBudget(category) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
                if (tileRows[index].size == 1) Spacer(Modifier.weight(1f))
            }
        }

        item {
            AllBillsSection(
                bills = bills,
                accounts = accounts,
                currencySymbol = currencySymbol,
                onBillClick = onBillClick,
                onMarkPaid = onMarkBillPaid,
                onAddBill = onAddRecurringBillClick,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
        item {
            MonthlySpendingTrendChart(transactions = transactions, categories = categories, currencySymbol = currencySymbol, modifier = pad)
        }
        item { CategoryDonutChart(transactions = monthTx, categories = categories, currencySymbol = currencySymbol, modifier = pad) }
        item { MonthlyBarChart(transactions = monthTx, currencySymbol = currencySymbol, modifier = pad) }
    }

    if (editingGoal) {
        EditSavingsGoalDialog(
            currentGoalTarget = savingsGoalTarget,
            onDismiss = { editingGoal = false },
            onSaveGoalTarget = {
                onUpdateSavingsGoalTarget(it)
                editingGoal = false
            },
            currencySymbol = currencySymbol
        )
    }
    if (editingTotalBudget) {
        TotalBudgetDialog(
            currentTotalBudget = budgetLimit,
            onDismiss = { editingTotalBudget = false },
            onSaveTotalBudget = {
                onSetTotalMonthlyBudget(it)
                editingTotalBudget = false
            },
            currencySymbol = currencySymbol
        )
    }
}
