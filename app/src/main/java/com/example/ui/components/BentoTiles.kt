package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.RecurringBillEntity
import com.example.data.utils.BillSchedule
import com.example.data.utils.BillStatus
import com.example.data.utils.CategoryTotals
import com.example.data.utils.FinancialHealth
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.accentOf
import java.text.SimpleDateFormat
import java.util.Locale

/** The indigo used for the large "hero" tile and the selected tab, in light and dark mode. */
@Composable
fun heroColor(): Color = if (LocalIsDarkTheme.current) Color(0xFF4338CA) else Color(0xFF3730A3)

/**
 * One rounded tile of a bento grid. Tiles stay neutral (white or dark) and the colour lives in
 * their icons, numbers, bars and text. Pass [outline] to ring a tile in colour: a warning, or the
 * selected filter or account.
 */
@Composable
fun BentoTile(
    modifier: Modifier = Modifier,
    outline: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: Int = 14,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val color = MaterialTheme.colorScheme.surface
    val border = if (outline != null) BorderStroke(1.5.dp, outline) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, contentColor = MaterialTheme.colorScheme.onSurface, border = border) {
            Column(modifier = Modifier.padding(contentPadding.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = color, contentColor = MaterialTheme.colorScheme.onSurface, border = border) {
            Column(modifier = Modifier.padding(contentPadding.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

/** How a hidden amount is shown: the currency symbol and dots. */
fun maskedMoney(currencySymbol: String): String = "$currencySymbol ••••••"

/**
 * The large indigo tile: total (or one account's) balance with Add and Transfer. The balance
 * stays hidden until the eye button is tapped, and the Accounts button opens the account tiles.
 */
@Composable
fun HeroBalanceTile(
    accounts: List<AccountEntity>,
    selectedAccountId: Long?,
    currencySymbol: String,
    onShowAll: () -> Unit,
    onAddTransaction: () -> Unit,
    onTransfer: () -> Unit,
    balancesHidden: Boolean,
    onToggleBalances: () -> Unit,
    accountsExpanded: Boolean,
    onToggleAccounts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = accounts.firstOrNull { it.id == selectedAccountId }
    val balance = selected?.balance ?: accounts.sumOf { it.balance }
    val money = if (balancesHidden) maskedMoney(currencySymbol) else formatMoney(currencySymbol, balance)
    Surface(modifier = modifier.fillMaxWidth().testTag("total_balance_card"), shape = RoundedCornerShape(28.dp), color = heroColor(), contentColor = Color.White) {
        // Balance and the Accounts button on the left, Add and Transfer stacked on the right.
        Row(modifier = Modifier.padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        selected?.name ?: stringResource(R.string.overview_total_balance),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    IconButton(onClick = onToggleBalances, modifier = Modifier.size(40.dp).testTag("toggle_balances")) {
                        Icon(
                            if (balancesHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = stringResource(if (balancesHidden) R.string.bento_show_balances else R.string.bento_hide_balances),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        money.substringBeforeLast('.'),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        maxLines = 1,
                        modifier = Modifier.testTag("total_balance_amount")
                    )
                    if (!balancesHidden && money.contains('.')) {
                        Text("." + money.substringAfterLast('.'), style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(bottom = 3.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = onToggleAccounts,
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.16f),
                        contentColor = Color.White,
                        modifier = Modifier.testTag("toggle_accounts")
                    ) {
                        Row(modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                pluralStringResource(R.plurals.bento_account_count, accounts.size, accounts.size),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Icon(
                                if (accountsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = stringResource(if (accountsExpanded) R.string.bento_hide_accounts else R.string.bento_show_accounts),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (selected != null) {
                        TextButton(onClick = onShowAll, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                            Text(stringResource(R.string.overview_all_accounts), maxLines = 1)
                        }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.width(124.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAddTransaction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = heroColor()),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("quick_add_expense_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.bento_add), maxLines = 1)
                }
                Button(
                    onClick = onTransfer,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.16f), contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("quick_transfer_btn")
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.overview_transfer), maxLines = 1)
                }
            }
        }
    }
}

/** An account in its own colour; tapping it shows just that account on the Overview. */
@Composable
fun AccountTile(account: AccountEntity, selected: Boolean, currencySymbol: String, onClick: () -> Unit, modifier: Modifier = Modifier, hidden: Boolean = false) {
    val accent = accentOf(account.colorHex)
    BentoTile(modifier = modifier.testTag("account_chip_${account.id}"), outline = if (selected) accent.ink else null, onClick = onClick) {
        AccountBadge(account)
        Text(account.name, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = accent.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (hidden) maskedMoney(currencySymbol) else formatMoney(currencySymbol, account.balance).substringBeforeLast('.'), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), maxLines = 1)
        account.accountNumberLast4?.takeIf { it.isNotBlank() }?.let {
            Text("•••• $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AddAccountTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    BentoTile(modifier = modifier.testTag("home_add_account"), onClick = onClick) {
        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Text(stringResource(R.string.bento_add_account), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
    }
}

/**
 * This month's spending as a ring: against the monthly budget when one is set, otherwise
 * against income. Green while comfortably inside, amber near the limit, red past it.
 */
@Composable
fun MonthRingTile(monthOffset: Int, totals: CategoryTotals, monthlyBudget: Double, currencySymbol: String, modifier: Modifier = Modifier) {
    val limit = if (monthlyBudget > 0) monthlyBudget else totals.income
    val ratio = if (limit > 0) (totals.expense / limit).toFloat() else if (totals.expense > 0) 1f else 0f
    val ringColor = when {
        ratio >= 1f -> accentOf("#991B1B").ink
        ratio >= 0.8f -> accentOf("#854D0E").ink
        else -> accentOf("#166534").ink
    }
    val track = MaterialTheme.colorScheme.outlineVariant
    val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(monthWindow(monthOffset).first)
    val spent = formatMoney(currencySymbol, totals.expense).substringBeforeLast('.')
    val percent = (ratio * 100).toInt()
    BentoTile(modifier = modifier.testTag("month_ring_tile")) {
        Text(monthName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(124.dp)
                .semantics { contentDescription = "$monthName: spent $spent, $percent%" },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 13.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                drawArc(ringColor, -90f, 360f * ratio.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.bento_spent), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(spent, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), maxLines = 1)
            }
        }
        TileFigure(stringResource(R.string.bento_income), "+" + formatMoney(currencySymbol, totals.income).substringBeforeLast('.'), accentOf("#166534").ink)
        if (limit > 0) {
            val left = limit - totals.expense
            TileFigure(
                stringResource(if (left >= 0) R.string.bento_left else R.string.bento_over),
                (if (left >= 0) "" else "−") + formatMoney(currencySymbol, kotlin.math.abs(left)).substringBeforeLast('.'),
                if (left >= 0) MaterialTheme.colorScheme.onSurface else accentOf("#991B1B").ink
            )
        }
    }
}

@Composable
private fun TileFigure(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

/** SMS waiting for review, or a calm "all reviewed" when there are none. */
@Composable
fun ReviewTile(pendingCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = accentOf(if (pendingCount > 0) "#854D0E" else "#166534")
    BentoTile(modifier = modifier.testTag("review_tile"), outline = if (pendingCount > 0) accent.ink else null, onClick = if (pendingCount > 0) onClick else null, contentPadding = 12) {
        Icon(if (pendingCount > 0) Icons.Default.MarkEmailUnread else Icons.Default.CheckCircle, contentDescription = null, tint = accent.ink, modifier = Modifier.size(26.dp))
        if (pendingCount > 0) {
            Text(pendingCount.toString(), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))
            Text(stringResource(R.string.bento_sms_to_review), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = accent.ink)
        } else {
            Text(stringResource(R.string.bento_all_reviewed), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold))
            Text(stringResource(R.string.bento_no_sms_waiting), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = accent.ink)
        }
    }
}

/**
 * Accounts below their low-balance alert, as a small tile; tapping it opens the full card with
 * Add money. A calm green "Balances OK" when none are low.
 */
@Composable
fun LowBalanceTile(lowAccounts: List<AccountEntity>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val low = lowAccounts.isNotEmpty()
    val accent = accentOf(if (low) "#991B1B" else "#166534")
    BentoTile(modifier = modifier.testTag("low_balance_tile"), outline = if (low) accent.ink else null, onClick = if (low) onClick else null, contentPadding = 12) {
        Icon(if (low) Icons.Default.Warning else Icons.Default.CheckCircle, contentDescription = null, tint = accent.ink, modifier = Modifier.size(24.dp))
        Text(
            stringResource(if (low) R.string.bento_low_balance else R.string.bento_balances_ok),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
        )
        Text(
            if (low) lowAccounts.joinToString(", ") { it.name } else stringResource(R.string.bento_all_above_alert),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = accent.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** The financial health score in its level's colour; tapping it explains the score. */
@Composable
fun HealthTile(health: FinancialHealth, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (labelRes, hex) = when (health.level) {
        FinancialHealth.Level.EXCELLENT -> R.string.overview_health_excellent to "#166534"
        FinancialHealth.Level.GOOD -> R.string.overview_health_good to "#1E40AF"
        FinancialHealth.Level.CAUTION -> R.string.overview_health_caution to "#854D0E"
        FinancialHealth.Level.CRITICAL -> R.string.overview_health_critical to "#991B1B"
    }
    val accent = accentOf(hex)
    BentoTile(modifier = modifier.testTag("budget_health_gauge_card"), onClick = onClick, contentPadding = 12) {
        Text(stringResource(R.string.bento_health), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = accent.ink)
        Text(health.score.toString(), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold))
        Text(stringResource(labelRes), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = accent.ink, maxLines = 2)
    }
}

/** Red when overdue, amber when due soon, otherwise the tile's own colour. */
@Composable
private fun billUrgencyColor(bill: RecurringBillEntity, normal: Color): Color =
    when (BillSchedule.status(bill.nextDueDate, bill.reminderDaysBefore, bill.isActive)) {
        BillStatus.OVERDUE -> accentOf("#991B1B").ink
        BillStatus.DUE_SOON -> accentOf("#854D0E").ink
        else -> normal
    }

/**
 * Upcoming bills, soonest first (up to three), each with its amount and when it is due, then
 * what was paid towards bills this month. The status colour shows urgency.
 */
@Composable
fun BillsTile(
    bills: List<RecurringBillEntity>,
    paidThisMonthCount: Int,
    paidThisMonthAmount: Double,
    currencySymbol: String,
    onClick: () -> Unit,
    onAddBill: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = accentOf("#5B21B6")
    val upcoming = bills.filter { it.isActive }.sortedBy { it.nextDueDate }.take(3)
    BentoTile(modifier = modifier.testTag("next_bill_tile"), onClick = if (bills.isEmpty()) onAddBill else onClick, contentPadding = 12) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = accent.ink, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.bento_bills), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold))
        }
        if (upcoming.isEmpty()) {
            Text(stringResource(R.string.bento_no_bills), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Text(stringResource(R.string.bento_add_bill), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = accent.ink)
        }
        upcoming.firstOrNull()?.let { next ->
            // The next bill is the focus: a big countdown, then what and how much.
            val days = BillSchedule.daysUntil(next.nextDueDate)
            val urgency = billUrgencyColor(next, accent.ink)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    when {
                        days < 0 -> stringResource(R.string.bento_overdue)
                        days == 0 -> stringResource(R.string.bento_today)
                        else -> days.toString()
                    },
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = urgency
                )
                if (days > 0) {
                    Text(
                        " " + pluralStringResource(R.plurals.bento_days_to_go, days),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = urgency,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
            Text(next.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                formatMoney(currencySymbol, next.amount).substringBeforeLast(".00") + " · " + SimpleDateFormat("d MMM", Locale.getDefault()).format(next.nextDueDate),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // The bills after it, smaller.
        upcoming.drop(1).forEach { bill ->
            Column {
                Text(bill.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(billStatusText(bill), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = billUrgencyColor(bill, accent.ink), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatMoney(currencySymbol, bill.amount).substringBeforeLast(".00"), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (paidThisMonthCount > 0) {
            Text(
                stringResource(R.string.bento_paid_this_month, paidThisMonthCount, formatMoney(currencySymbol, paidThisMonthAmount).substringBeforeLast(".00")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (upcoming.isNotEmpty() && upcoming.size < 3) {
            TextButton(onClick = onAddBill, contentPadding = PaddingValues(horizontal = 0.dp), colors = ButtonDefaults.textButtonColors(contentColor = accent.ink)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.bento_add_bill))
            }
        }
    }
}
