package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.utils.CategoryTotals
import com.example.data.utils.FinancialHealth
import com.example.ui.theme.accentOf
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PendingAmber
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(20.dp)

/** Accounts that trigger a low-balance alert. Credit cards carry a negative balance by design. */
fun lowBalanceAccounts(
    accounts: List<AccountEntity>,
    threshold: Double,
    transactions: List<TransactionEntity>
): List<AccountEntity> {
    // A new account at 0 with nothing recorded yet isn't low on money; it just hasn't been set up.
    val used = transactions.mapTo(HashSet()) { it.accountId }
    return accounts.filter { it.type != "CREDIT_CARD" && it.balance < threshold && (it.balance != 0.0 || it.id in used) }
}

/** Total balance (all accounts or the selected one), account switcher and quick actions. */
@Composable
fun BalanceHeroCard(
    accounts: List<AccountEntity>,
    selectedAccountId: Long?,
    currencySymbol: String,
    onSelectAccount: (Long?) -> Unit,
    onAddTransaction: () -> Unit,
    onTransfer: () -> Unit,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = accounts.firstOrNull { it.id == selectedAccountId }
    val balance = selected?.balance ?: accounts.sumOf { it.balance }

    Card(
        modifier = modifier.fillMaxWidth().testTag("total_balance_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(top = 18.dp, bottom = 14.dp)) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = selected?.name ?: stringResource(R.string.overview_total_balance),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = formatMoney(currencySymbol, balance),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (balance < 0) ExpenseRed else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag("total_balance_amount")
                )
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedAccountId == null,
                        onClick = { onSelectAccount(null) },
                        label = { Text(stringResource(R.string.overview_all_accounts)) }
                    )
                }
                items(accounts, key = { it.id }) { account ->
                    FilterChip(
                        selected = selectedAccountId == account.id,
                        onClick = { onSelectAccount(account.id) },
                        label = {
                            Text(
                                "${account.name} · ${formatMoney(currencySymbol, account.balance).substringBefore('.')}",
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.testTag("account_chip_${account.id}")
                    )
                }
                item {
                    TextButton(onClick = onAddAccount, modifier = Modifier.testTag("home_add_account")) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.overview_add_account))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(onClick = onAddTransaction, modifier = Modifier.weight(1f).testTag("quick_add_expense_btn")) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.overview_add_transaction))
                }
                FilledTonalButton(onClick = onTransfer, modifier = Modifier.weight(1f).testTag("quick_transfer_btn")) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.overview_transfer))
                }
            }
        }
    }
}

/** "5 SMS transactions to review" with a Review button, shown at the top while any are pending. */
@Composable
fun PendingReviewCard(count: Int, onReview: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onReview).testTag("pending_review_card"),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FactCheck, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.overview_pending_title, count),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(stringResource(R.string.overview_pending_subtitle), style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onReview, modifier = Modifier.testTag("pending_review_button")) {
                Text(stringResource(R.string.overview_pending_review))
            }
        }
    }
}

/**
 * One card for every account under the low-balance alert level: how far below it is, a bar
 * towards the alert level, and actions to top it up or change the alert level.
 */
@Composable
fun LowBalanceCard(
    lowAccounts: List<AccountEntity>,
    threshold: Double,
    currencySymbol: String,
    onAddMoney: (AccountEntity) -> Unit,
    onChangeAlertLevel: () -> Unit,
    modifier: Modifier = Modifier,
    hideAmounts: Boolean = false
) {
    Card(
        modifier = modifier.fillMaxWidth().testTag("low_balance_card"),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.overview_low_balance_title, lowAccounts.size),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        stringResource(R.string.overview_low_balance_subtitle, formatMoney(currencySymbol, threshold)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            lowAccounts.forEach { account ->
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(account.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                        Text(
                            if (hideAmounts) {
                                stringResource(R.string.overview_low_balance_below_hidden)
                            } else {
                                stringResource(
                                    R.string.overview_low_balance_below,
                                    formatMoney(currencySymbol, account.balance),
                                    formatMoney(currencySymbol, threshold - account.balance)
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = { onAddMoney(account) }, modifier = Modifier.testTag("low_balance_add_money_${account.id}")) {
                        Text(stringResource(R.string.overview_low_balance_add_money))
                    }
                }
                // The bar would hint at the balance, so it is left out while balances are hidden.
                if (!hideAmounts) LinearProgressIndicator(
                    progress = { if (threshold > 0) (account.balance / threshold).toFloat().coerceIn(0f, 1f) else 0f },
                    color = MaterialTheme.colorScheme.error,
                    trackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp)
                )
            }
            TextButton(onClick = onChangeAlertLevel, modifier = Modifier.align(Alignment.End).testTag("low_balance_change_alert")) {
                Text(stringResource(R.string.overview_low_balance_change_alert))
            }
        }
    }
}

/** Income, spending and what's left for the month, with the share of income kept. */
@Composable
fun MonthSummaryCard(totals: CategoryTotals, currencySymbol: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().testTag("month_summary_card"),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                SummaryFigure(stringResource(R.string.overview_income), formatMoney(currencySymbol, totals.income), IncomeGreen, Modifier.weight(1f))
                SummaryFigure(stringResource(R.string.overview_spent), formatMoney(currencySymbol, totals.expense), ExpenseRed, Modifier.weight(1f))
                SummaryFigure(
                    stringResource(if (totals.net >= 0) R.string.overview_saved else R.string.overview_overspent),
                    formatMoney(currencySymbol, kotlin.math.abs(totals.net)),
                    if (totals.net >= 0) MaterialTheme.colorScheme.primary else ExpenseRed,
                    Modifier.weight(1f)
                )
            }
            if (totals.income > 0) {
                val spentShare = (totals.expense / totals.income).toFloat()
                LinearProgressIndicator(
                    progress = { spentShare.coerceIn(0f, 1f) },
                    color = if (spentShare > 1f) ExpenseRed else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(8.dp)
                )
                Text(
                    stringResource(R.string.overview_spent_share_of_income, (spentShare * 100).roundToInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryFigure(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class CategoryRowData(val category: CategoryEntity?, val name: String, val spent: Double, val limit: Double)

/**
 * Net spending per category for the month against its budget. The top five show first; "Show
 * all" lists the rest, including budgeted categories with nothing spent. Tapping a row opens the
 * Ledger filtered to that category and month.
 */
@Composable
fun CategorySpendingCard(
    categories: List<CategoryEntity>,
    totals: CategoryTotals,
    currencySymbol: String,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val rows = remember(categories, totals) {
        val spentRows = totals.spendingByCategory.map { (name, spent) ->
            val category = categories.firstOrNull { it.name.equals(name, ignoreCase = true) }
            CategoryRowData(category, category?.name ?: name, spent, category?.monthlyBudgetLimit ?: 0.0)
        }.sortedByDescending { it.spent }
        val unspentBudgeted = categories
            .filter { !it.isIncomeCategory && it.monthlyBudgetLimit > 0 && totals.spentIn(it.name) <= 0.0 }
            .map { CategoryRowData(it, it.name, 0.0, it.monthlyBudgetLimit) }
        spentRows + unspentBudgeted
    }
    val visible = if (expanded) rows else rows.take(5)

    // One bento tile holding every category as a full-width row with its progress bar.
    BentoTile(modifier = modifier.fillMaxWidth().testTag("category_spending_progress_card"), contentPadding = 16) {
        Text(stringResource(R.string.overview_categories_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(
            stringResource(R.string.overview_categories_subtitle),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (rows.isEmpty()) {
            Text(stringResource(R.string.overview_categories_empty), style = MaterialTheme.typography.bodyMedium)
        }
        visible.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            CategoryBarRow(row, currencySymbol, onClick = { onCategoryClick(row.name) })
        }
        if (rows.size > 5) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag("category_show_all")) {
                Text(
                    if (expanded) {
                        stringResource(R.string.overview_categories_show_less)
                    } else {
                        stringResource(R.string.overview_categories_show_all, rows.size)
                    }
                )
            }
        }
    }
}

/**
 * One category as a row: its icon in its colour, name, spent against budget, then the
 * horizontal progress bar (green, amber near the limit, red past it) and what is left or over.
 */
@Composable
private fun CategoryBarRow(row: CategoryRowData, currencySymbol: String, onClick: () -> Unit) {
    val hasLimit = row.limit > 0
    val ratio = if (hasLimit) row.spent / row.limit else 0.0
    val barColor = when {
        ratio > 1.0 -> accentOf("#991B1B").ink
        ratio > 0.8 -> accentOf("#854D0E").ink
        else -> accentOf("#166534").ink
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag("category_row_${row.name}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(row.category?.iconName, row.name, row.category?.colorHex, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Text(row.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(
                if (hasLimit) {
                    "${formatMoney(currencySymbol, row.spent).substringBefore('.')} / ${formatMoney(currencySymbol, row.limit).substringBefore('.')}"
                } else {
                    formatMoney(currencySymbol, row.spent).substringBefore('.')
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (hasLimit && ratio > 1.0) barColor else MaterialTheme.colorScheme.onSurface
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.overview_categories_open, row.name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (hasLimit) {
            LinearProgressIndicator(
                progress = { ratio.toFloat().coerceIn(0f, 1f) },
                color = barColor,
                trackColor = barColor.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth().padding(start = 48.dp, top = 8.dp).height(6.dp)
            )
            Text(
                if (ratio > 1.0) {
                    stringResource(R.string.overview_category_over_by, formatMoney(currencySymbol, row.spent - row.limit).substringBefore('.'))
                } else {
                    stringResource(R.string.overview_category_left, formatMoney(currencySymbol, row.limit - row.spent).substringBefore('.'))
                },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (ratio > 0.8) barColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 48.dp, top = 4.dp)
            )
        } else {
            Text(
                stringResource(R.string.overview_category_no_budget),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 48.dp, top = 2.dp)
            )
        }
    }
}

/** The score card; tapping it explains how the score was worked out. */
@Composable
fun FinancialHealthCard(health: FinancialHealth, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (label, color) = healthLabel(health)
    Card(
        modifier = modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).testTag("budget_health_gauge_card"),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("${health.score}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = color)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.overview_health_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(label, style = MaterialTheme.typography.bodyMedium, color = color)
                Text(
                    stringResource(R.string.overview_health_tap_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.Info, contentDescription = stringResource(R.string.overview_health_explain), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun healthLabel(health: FinancialHealth): Pair<String, Color> = when (health.level) {
    FinancialHealth.Level.EXCELLENT -> stringResource(R.string.overview_health_excellent) to IncomeGreen
    FinancialHealth.Level.GOOD -> stringResource(R.string.overview_health_good) to MaterialTheme.colorScheme.primary
    FinancialHealth.Level.CAUTION -> stringResource(R.string.overview_health_caution) to PendingAmber
    FinancialHealth.Level.CRITICAL -> stringResource(R.string.overview_health_critical) to ExpenseRed
}

/** Explains the score with the user's own numbers: what it measures and where points went. */
@Composable
fun FinancialHealthDialog(health: FinancialHealth, currencySymbol: String, onDismiss: () -> Unit) {
    val (label, color) = healthLabel(health)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(stringResource(R.string.overview_health_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Text("${health.score} / 100 · $label", color = color, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.health_explain_intro), style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider()
                ScoreLine(stringResource(R.string.health_explain_start), "100")
                ScoreLine(
                    text = when {
                        health.comparedWith <= 0 -> stringResource(R.string.health_explain_spending_no_base)
                        health.comparedWithBudget -> stringResource(
                            R.string.health_explain_spending_budget,
                            formatMoney(currencySymbol, health.spent),
                            (health.spendRatio * 100).roundToInt(),
                            formatMoney(currencySymbol, health.comparedWith)
                        )
                        else -> stringResource(
                            R.string.health_explain_spending_income,
                            formatMoney(currencySymbol, health.spent),
                            (health.spendRatio * 100).roundToInt(),
                            formatMoney(currencySymbol, health.comparedWith)
                        )
                    },
                    points = if (health.spendingPenalty > 0) "−${health.spendingPenalty}" else "0"
                )
                ScoreLine(
                    text = stringResource(R.string.health_explain_low_balance, health.lowBalanceAccounts, FinancialHealth.LOW_BALANCE_PENALTY),
                    points = if (health.lowBalancePenalty > 0) "−${health.lowBalancePenalty}" else "0"
                )
                HorizontalDivider()
                Text(stringResource(R.string.health_explain_rules), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.health_explain_levels), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.health_explain_improve), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.testTag("health_dialog_close")) { Text(stringResource(R.string.bill_details_close)) } }
    )
}

@Composable
private fun ScoreLine(text: String, points: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(points, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
    }
}
