package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringBillEntity
import com.example.data.local.entity.TransactionEntity
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.MonthlyBarChart
import com.example.ui.components.MonthlySpendingTrendChart
import com.example.ui.components.categoryIconFor
import com.example.ui.dialogs.EditSavingsGoalDialog
import com.example.data.utils.CategoryTotals
import com.example.ui.components.AllBillsSection
import com.example.ui.dialogs.TotalBudgetDialog
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PendingAmber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
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
    // Selected Tab State (0: All, 1: Budget Planning, 2: Savings Goals, 3: Subscriptions)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var budgetFilterMode by remember { mutableStateOf("ALL") } // ALL, OVER_BUDGET, BUDGETED

    // Month Calculations
    val (monthTitle, monthTx) = remember(transactions, selectedMonthOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, selectedMonthOffset)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startMs = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        val endMs = cal.timeInMillis

        val format = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        cal.add(Calendar.MONTH, -1)
        val title = format.format(cal.time)

        val filtered = transactions.filter { it.dateTimestamp in startMs until endMs }
        Pair(title, filtered)
    }

    // Netted per category (money received back in a category reduces its spending) and with
    // transfers between own accounts left out - the same rules as the Overview tab.
    val monthTotals = remember(monthTx) { CategoryTotals.of(monthTx) }
    val totalIncome = monthTotals.income
    val totalExpense = monthTotals.expense
    val netSavings = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0.0) ((netSavings / totalIncome) * 100).coerceIn(-100.0, 100.0) else 0.0

    // Group expenses by category name
    val categoryExpensesMap = remember(monthTotals, categories) {
        categories.associate { it.name to monthTotals.spentIn(it.name) }
    }

    // Total Budgeted vs Spent across all expense categories (income/savings categories like
    // "Salary & Income" don't count toward a spending budget).
    val totalBudgetLimit = remember(categories) {
        categories.filter { !it.isIncomeCategory }.sumOf { it.monthlyBudgetLimit }
    }
    val totalSpentInBudgetedCategories = remember(categoryExpensesMap, categories) {
        categories.filter { it.monthlyBudgetLimit > 0 }.sumOf { categoryExpensesMap[it.name] ?: 0.0 }
    }
    val overallBudgetProgress = if (totalBudgetLimit > 0) (totalSpentInBudgetedCategories / totalBudgetLimit).toFloat() else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Top Header & Month Selector
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(
                    text = stringResource(R.string.reports_screen_title),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onMonthOffsetChange(selectedMonthOffset - 1) },
                            modifier = Modifier.testTag("prev_month_button")
                        ) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.reports_screen_previous_month_desc))
                        }

                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        IconButton(
                            onClick = { onMonthOffsetChange(selectedMonthOffset + 1) },
                            enabled = selectedMonthOffset < 0,
                            modifier = Modifier.testTag("next_month_button")
                        ) {
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = stringResource(R.string.reports_screen_next_month_desc))
                        }
                    }
                }
            }
        }

        // Section Tabs Row
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 20.dp,
                divider = {},
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                listOf(
                    stringResource(R.string.reports_screen_tab_all_overview),
                    stringResource(R.string.reports_screen_tab_smart_budget),
                    stringResource(R.string.reports_screen_tab_savings_goals),
                    stringResource(R.string.reports_screen_tab_subscriptions)
                ).forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    )
                }
            }
        }

        // --- OVERVIEW SECTION ---
        if (selectedTabIndex == 0 || selectedTabIndex == 1) {
            // Net Cash Flow Summary Card
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.reports_screen_net_monthly_cash_flow),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    )
                                    Text(
                                        text = stringResource(R.string.reports_screen_currency_amount, currencySymbol, netSavings),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (netSavings >= 0) IncomeGreen else ExpenseRed
                                        )
                                    )
                                }

                                Surface(
                                    color = (if (savingsRate >= 0) IncomeGreen else ExpenseRed).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.reports_screen_savings_rate_label, savingsRate),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (savingsRate >= 0) IncomeGreen else ExpenseRed
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.reports_screen_total_income_label),
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    )
                                    Text(
                                        text = stringResource(R.string.reports_screen_income_amount, currencySymbol, totalIncome),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = IncomeGreen
                                        )
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = stringResource(R.string.reports_screen_total_expenses_label),
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    )
                                    Text(
                                        text = stringResource(R.string.reports_screen_expense_amount, currencySymbol, totalExpense),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ExpenseRed
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SMART BUDGET PLANNING SECTION ---
        if (selectedTabIndex == 0 || selectedTabIndex == 1) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.reports_screen_smart_budget_planning_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = onAddCustomCategoryClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_custom_category_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.reports_screen_new_category_button), style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Overall Budget Health Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.reports_screen_total_monthly_budget_health_title),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (totalBudgetLimit > 0) {
                                            stringResource(
                                                R.string.reports_screen_budget_spent_of_total,
                                                currencySymbol,
                                                totalSpentInBudgetedCategories,
                                                currencySymbol,
                                                totalBudgetLimit
                                            )
                                        } else {
                                            stringResource(R.string.reports_screen_no_budget_limits_message)
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }

                                if (totalBudgetLimit > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))

                                    val statusColor = when {
                                        overallBudgetProgress > 1.0f -> ExpenseRed
                                        overallBudgetProgress > 0.85f -> PendingAmber
                                        else -> IncomeGreen
                                    }
                                    val statusText = when {
                                        overallBudgetProgress > 1.0f -> stringResource(R.string.reports_screen_status_over_budget)
                                        overallBudgetProgress > 0.85f -> stringResource(R.string.reports_screen_status_near_limit)
                                        else -> stringResource(R.string.reports_screen_status_on_track)
                                    }

                                    Surface(
                                        color = statusColor.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = statusText,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor
                                            )
                                        )
                                    }
                                }
                            }

                            if (totalBudgetLimit > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { overallBudgetProgress.coerceAtMost(1.0f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp)),
                                    color = when {
                                        overallBudgetProgress > 1.0f -> ExpenseRed
                                        overallBudgetProgress > 0.85f -> PendingAmber
                                        else -> IncomeGreen
                                    },
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                val remaining = totalBudgetLimit - totalSpentInBudgetedCategories
                                Text(
                                    text = if (remaining >= 0) {
                                        stringResource(R.string.reports_screen_budget_remaining, currencySymbol, remaining)
                                    } else {
                                        stringResource(R.string.reports_screen_budget_over_total, currencySymbol, -remaining)
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (remaining >= 0) IncomeGreen else ExpenseRed
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Budget Category Filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = budgetFilterMode == "ALL",
                            onClick = { budgetFilterMode = "ALL" },
                            label = { Text(stringResource(R.string.reports_screen_filter_all_categories, categories.size)) }
                        )
                        FilterChip(
                            selected = budgetFilterMode == "OVER_BUDGET",
                            onClick = { budgetFilterMode = "OVER_BUDGET" },
                            label = {
                                val overCount = categories.count {
                                    val spent = categoryExpensesMap[it.name] ?: 0.0
                                    it.monthlyBudgetLimit > 0 && spent > it.monthlyBudgetLimit
                                }
                                Text(stringResource(R.string.reports_screen_filter_over_budget_count, overCount))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Categories List Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val filteredCategories = categories.filter { cat ->
                                val spent = categoryExpensesMap[cat.name] ?: 0.0
                                when (budgetFilterMode) {
                                    "OVER_BUDGET" -> cat.monthlyBudgetLimit > 0 && spent > cat.monthlyBudgetLimit
                                    else -> true
                                }
                            }

                            if (filteredCategories.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.reports_screen_no_categories_match_filter),
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                filteredCategories.forEach { cat ->
                                    val spent = categoryExpensesMap[cat.name] ?: 0.0
                                    val limit = cat.monthlyBudgetLimit
                                    val hasLimit = limit > 0.0
                                    val progress = if (hasLimit) (spent / limit).toFloat().coerceIn(0f, 1.2f) else 0f

                                    val catColor = try {
                                        Color(android.graphics.Color.parseColor(cat.colorHex))
                                    } catch (e: Exception) {
                                        MaterialTheme.colorScheme.primary
                                    }

                                    val progressColor = when {
                                        !hasLimit -> MaterialTheme.colorScheme.primary
                                        progress > 1.0f -> ExpenseRed
                                        progress > 0.8f -> PendingAmber
                                        else -> IncomeGreen
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onEditCategoryBudget(cat) }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(catColor.copy(alpha = 0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = categoryIconFor(cat.iconName),
                                                        contentDescription = null,
                                                        tint = catColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Column {
                                                    Text(
                                                        text = cat.name,
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                    if (!cat.isPredefined) {
                                                        Text(
                                                            text = stringResource(R.string.reports_screen_custom_category_label),
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                fontSize = 10.sp
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = stringResource(R.string.reports_screen_amount_spent, currencySymbol, spent),
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (hasLimit && spent > limit) ExpenseRed else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    )
                                                    Text(
                                                        text = if (hasLimit) stringResource(R.string.reports_screen_limit_amount, currencySymbol, limit) else stringResource(R.string.reports_screen_tap_to_set_limit),
                                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                IconButton(
                                                    onClick = { onEditCategoryBudget(cat) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = stringResource(R.string.reports_screen_edit_budget_limit_desc),
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (hasLimit) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LinearProgressIndicator(
                                                progress = { progress.coerceAtMost(1.0f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                color = progressColor,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            // The bar itself always caps at 100% width, so being
                                            // 45% over budget looks identical to being 300% over -
                                            // spell out the actual overage instead.
                                            if (progress > 1.0f) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = stringResource(R.string.reports_screen_over_budget_by_percent, ((spent / limit).toFloat() - 1.0f) * 100f),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = ExpenseRed
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SAVINGS TARGET & GOALS SECTION ---
        if (selectedTabIndex == 0 || selectedTabIndex == 2) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.reports_screen_smart_savings_target_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val goalProgress = if (savingsGoalTarget > 0) (netSavings / savingsGoalTarget).toFloat().coerceIn(0f, 1f) else 0f
                    val goalAchieved = netSavings >= savingsGoalTarget && savingsGoalTarget > 0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(IncomeGreen.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Savings,
                                            contentDescription = null,
                                            tint = IncomeGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.reports_screen_monthly_savings_goal_title),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = stringResource(R.string.reports_screen_savings_target_label, currencySymbol, savingsGoalTarget),
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    color = if (goalAchieved) IncomeGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.reports_screen_goal_achieved_percent, goalProgress * 100),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (goalAchieved) IncomeGreen else MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = stringResource(R.string.reports_screen_current_savings, currencySymbol, netSavings.coerceAtLeast(0.0)),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (netSavings >= 0) IncomeGreen else ExpenseRed
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { goalProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = IncomeGreen,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Motivation & Smart Insight Tip
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (goalAchieved) Icons.Default.Savings else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (goalAchieved) IncomeGreen else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = when {
                                            goalAchieved -> stringResource(R.string.reports_screen_goal_reached_message, currencySymbol, netSavings)
                                            netSavings > 0 -> stringResource(R.string.reports_screen_goal_remaining_message, currencySymbol, savingsGoalTarget - netSavings)
                                            else -> stringResource(R.string.reports_screen_net_spending_exceeds_income_message)
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- RECURRING BILLS SECTION ---
        if (selectedTabIndex == 0 || selectedTabIndex == 3) {
            item {
                AllBillsSection(
                    bills = bills,
                    accounts = accounts,
                    currencySymbol = currencySymbol,
                    onBillClick = onBillClick,
                    onMarkPaid = onMarkBillPaid,
                    onAddBill = onAddRecurringBillClick,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }

        // --- VISUAL CHARTS SECTION ---
        if (selectedTabIndex == 0) {
            // Categorized Monthly Spending Trend Chart
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    MonthlySpendingTrendChart(
                        transactions = transactions,
                        categories = categories,
                        currencySymbol = currencySymbol
                    )
                }
            }

            // Category Expenses Donut Chart
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    CategoryDonutChart(
                        transactions = monthTx,
                        categories = categories,
                        currencySymbol = currencySymbol
                    )
                }
            }

            // Daily Spending Bar Graph
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    MonthlyBarChart(
                        transactions = monthTx,
                        currencySymbol = currencySymbol
                    )
                }
            }
        }
    }
}
