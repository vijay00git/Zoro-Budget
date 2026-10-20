package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.TransactionEntity
import com.example.data.utils.BillSchedule
import com.example.data.utils.CategoryTotals
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Month calendar where each day is shaded by how much was spent (debits, transfers excluded), so
 * heavy days stand out at a glance. Tapping a day lists that day's transactions underneath.
 */
@Composable
fun SpendingHeatmapCard(
    monthOffset: Int,
    monthTransactions: List<TransactionEntity>,
    currencySymbol: String,
    onTransactionClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val (monthStart, _) = remember(monthOffset) { monthWindow(monthOffset) }
    val spending = remember(monthTransactions) {
        monthTransactions.filter { it.type == "DEBIT" && it.categoryTag != CategoryTotals.TRANSFER_CATEGORY }
    }
    val byDay = remember(spending) {
        spending.groupBy { Calendar.getInstance().apply { timeInMillis = it.dateTimestamp }.get(Calendar.DAY_OF_MONTH) }
    }
    val dayTotals = remember(byDay) { byDay.mapValues { (_, txs) -> txs.sumOf { it.amount } } }
    val maxDay = dayTotals.values.maxOrNull() ?: 0.0

    val calendar = remember(monthStart) { Calendar.getInstance().apply { timeInMillis = monthStart } }
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Monday-first grid: blank cells before the 1st
    val leadingBlanks = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val today = BillSchedule.startOfDay(System.currentTimeMillis())
    val daysElapsed = (1..daysInMonth).count { dayMillis(monthStart, it) <= today }

    var selectedDay by rememberSaveable(monthOffset) { mutableStateOf<Int?>(null) }
    val dayFormat = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()) }
    val shortFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    val heatColor = ExpenseRed

    Card(
        modifier = modifier.fillMaxWidth().testTag("spending_heatmap"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.heatmap_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(
                stringResource(R.string.heatmap_subtitle),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                weekdayInitials().forEach { initial ->
                    Text(
                        initial,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).padding(bottom = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            val cells = List(leadingBlanks) { null } + (1..daysInMonth).toList()
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (0 until 7).forEach { col ->
                        val day = week.getOrNull(col)
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(vertical = 2.dp)) {
                            if (day != null) {
                                val amount = dayTotals[day] ?: 0.0
                                val isFuture = dayMillis(monthStart, day) > today
                                val level = if (maxDay > 0 && amount > 0) (amount / maxDay).toFloat() else 0f
                                val background = when {
                                    amount <= 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isFuture) 0.3f else 0.7f)
                                    else -> heatColor.copy(alpha = 0.18f + 0.82f * level)
                                }
                                val label = "${shortFormat.format(dayMillis(monthStart, day))}, ${formatMoney(currencySymbol, amount)}"
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(background, RoundedCornerShape(8.dp))
                                        .then(
                                            when {
                                                selectedDay == day -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                                dayMillis(monthStart, day) == today -> Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                                else -> Modifier
                                            }
                                        )
                                        .clickable(enabled = !isFuture) { selectedDay = if (selectedDay == day) null else day }
                                        .semantics { contentDescription = label }
                                        .testTag("heatmap_day_$day"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$day",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (level > 0.55f) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.heatmap_less), style = MaterialTheme.typography.labelSmall)
                listOf(0.18f, 0.4f, 0.6f, 0.8f, 1f).forEach { alpha ->
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.size(12.dp).background(heatColor.copy(alpha = alpha), RoundedCornerShape(3.dp)))
                }
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.heatmap_more), style = MaterialTheme.typography.labelSmall)
            }

            val heaviest = dayTotals.maxByOrNull { it.value }
            if (heaviest != null && daysElapsed > 0) {
                Text(
                    stringResource(
                        R.string.heatmap_stats,
                        shortFormat.format(dayMillis(monthStart, heaviest.key)),
                        formatMoney(currencySymbol, heaviest.value).substringBefore('.'),
                        formatMoney(currencySymbol, dayTotals.values.sum() / daysElapsed).substringBefore('.'),
                        (1..daysElapsed).count { (dayTotals[it] ?: 0.0) <= 0.0 }
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

        }
    }

    // The day's transactions open in a dialog, so they're visible wherever the calendar sits on screen
    selectedDay?.let { day ->
        val txs = monthTransactions
            .filter { Calendar.getInstance().apply { timeInMillis = it.dateTimestamp }.get(Calendar.DAY_OF_MONTH) == day }
            .sortedByDescending { it.dateTimestamp }
        AlertDialog(
            onDismissRequest = { selectedDay = null },
            title = {
                Text(
                    stringResource(
                        R.string.heatmap_day_summary,
                        dayFormat.format(dayMillis(monthStart, day)),
                        formatMoney(currencySymbol, dayTotals[day] ?: 0.0)
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.testTag("heatmap_day_summary")
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (txs.isEmpty()) {
                        Text(stringResource(R.string.heatmap_day_empty), style = MaterialTheme.typography.bodySmall)
                    }
                    txs.forEachIndexed { index, tx ->
                        if (index > 0) HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedDay = null
                                    onTransactionClick(tx)
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    tx.merchantOrSender?.takeIf { it.isNotBlank() } ?: tx.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(tx.categoryTag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                (if (tx.type == "CREDIT") "+" else "−") + formatMoney(currencySymbol, tx.amount),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (tx.type == "CREDIT") IncomeGreen else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedDay = null }) { Text(stringResource(R.string.bill_details_close)) } }
        )
    }
}

private fun dayMillis(monthStart: Long, day: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = monthStart
        set(Calendar.DAY_OF_MONTH, day)
    }.timeInMillis

private fun weekdayInitials(): List<String> {
    val format = SimpleDateFormat("EEEEE", Locale.getDefault())
    val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) }
    return List(7) {
        format.format(cal.time).also { cal.add(Calendar.DAY_OF_YEAR, 1) }
    }
}
