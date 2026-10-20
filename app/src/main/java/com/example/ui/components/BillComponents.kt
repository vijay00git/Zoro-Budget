package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.RecurringBillEntity
import com.example.data.utils.BillFrequency
import com.example.data.utils.BillSchedule
import com.example.data.utils.BillStatus
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun formatMoney(currencySymbol: String, amount: Double): String =
    currencySymbol + String.format(Locale.getDefault(), "%,.2f", amount)

/** "Overdue by 3 days", "Due today", "Due in 2 days", "Due 15 Nov", "Paused". */
@Composable
fun billStatusText(bill: RecurringBillEntity): String {
    val days = BillSchedule.daysUntil(bill.nextDueDate)
    return when {
        !bill.isActive -> stringResource(R.string.bill_status_paused)
        days < -1 -> stringResource(R.string.bill_status_overdue_days, -days)
        days == -1 -> stringResource(R.string.bill_status_overdue_yesterday)
        days == 0 -> stringResource(R.string.bill_status_due_today)
        days == 1 -> stringResource(R.string.bill_status_due_tomorrow)
        days <= 7 -> stringResource(R.string.bill_status_due_in_days, days)
        else -> stringResource(R.string.bill_status_due_on, SimpleDateFormat("d MMM", Locale.getDefault()).format(bill.nextDueDate))
    }
}

@Composable
private fun statusColors(bill: RecurringBillEntity): Pair<Color, Color> =
    when (BillSchedule.status(bill.nextDueDate, bill.reminderDaysBefore, bill.isActive)) {
        BillStatus.OVERDUE -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        BillStatus.DUE_SOON -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        BillStatus.UPCOMING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        BillStatus.PAUSED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

/** Monthly cost of all active bills (weekly x 52/12, quarterly / 3, yearly / 12). */
fun monthlyBillTotal(bills: List<RecurringBillEntity>): Double =
    bills.filter { it.isActive && it.type == "DEBIT" }.sumOf { it.amount * BillFrequency.from(it.frequency).monthlyFactor }

@Composable
fun BillRow(
    bill: RecurringBillEntity,
    accountName: String?,
    currencySymbol: String,
    onClick: () -> Unit,
    onMarkPaid: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val (badgeBg, badgeFg) = statusColors(bill)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .testTag("bill_row_${bill.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bill.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bill.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = billStatusText(bill),
                    style = MaterialTheme.typography.labelMedium,
                    color = badgeFg,
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = listOfNotNull(frequencyName(BillFrequency.from(bill.frequency)), accountName).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatMoney(currencySymbol, bill.amount),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (bill.type == "CREDIT") IncomeGreen else MaterialTheme.colorScheme.onSurface
            )
            if (onMarkPaid != null) {
                FilledTonalButton(
                    onClick = onMarkPaid,
                    modifier = Modifier.height(36.dp).testTag("bill_mark_paid_${bill.id}")
                ) {
                    Text(
                        stringResource(if (bill.type == "CREDIT") R.string.bill_mark_received else R.string.bill_mark_paid),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
fun frequencyName(frequency: BillFrequency): String = stringResource(
    when (frequency) {
        BillFrequency.WEEKLY -> R.string.bill_frequency_weekly
        BillFrequency.MONTHLY -> R.string.bill_frequency_monthly
        BillFrequency.QUARTERLY -> R.string.bill_frequency_quarterly
        BillFrequency.YEARLY -> R.string.bill_frequency_yearly
    }
)

/**
 * Overview card listing the bills that still need paying (overdue, due soon or due this month).
 * A bill marked paid moves to its next cycle and leaves this list until that cycle comes round.
 */
@Composable
fun BillsDueCard(
    bills: List<RecurringBillEntity>,
    accounts: List<AccountEntity>,
    paidThisMonthCount: Int,
    paidThisMonthAmount: Double,
    currencySymbol: String,
    onBillClick: (RecurringBillEntity) -> Unit,
    onMarkPaid: (RecurringBillEntity) -> Unit,
    onAddBill: () -> Unit,
    onSeeAllBills: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dueBills = remember(bills) {
        bills.filter { BillSchedule.isDueForPayment(it.nextDueDate, it.reminderDaysBefore, it.isActive) }
    }
    val nextUpcoming = remember(bills) {
        bills.filter { it.isActive && it !in dueBills }.minByOrNull { it.nextDueDate }
    }

    Card(
        modifier = modifier.fillMaxWidth().testTag("bills_due_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.bills_due_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                if (dueBills.isNotEmpty()) {
                    Text(
                        text = formatMoney(currencySymbol, dueBills.filter { it.type == "DEBIT" }.sumOf { it.amount }),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = ExpenseRed
                    )
                }
            }

            when {
                bills.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.bills_empty_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                dueBills.isEmpty() -> {
                    Row(modifier = Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IncomeGreen)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = nextUpcoming?.let {
                                stringResource(
                                    R.string.bills_all_paid_next,
                                    it.name,
                                    SimpleDateFormat("d MMM", Locale.getDefault()).format(it.nextDueDate)
                                )
                            } ?: stringResource(R.string.bills_all_paid),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                else -> {
                    dueBills.forEachIndexed { index, bill ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        BillRow(
                            bill = bill,
                            accountName = accounts.firstOrNull { it.id == bill.accountId }?.name,
                            currencySymbol = currencySymbol,
                            onClick = { onBillClick(bill) },
                            onMarkPaid = { onMarkPaid(bill) }
                        )
                    }
                }
            }

            if (paidThisMonthCount > 0) {
                Text(
                    text = stringResource(R.string.bills_paid_this_month, paidThisMonthCount, formatMoney(currencySymbol, paidThisMonthAmount)),
                    style = MaterialTheme.typography.labelMedium,
                    color = IncomeGreen,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onAddBill, modifier = Modifier.testTag("bills_add_button")) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.bills_add))
                }
                if (bills.isNotEmpty()) {
                    TextButton(onClick = onSeeAllBills, modifier = Modifier.testTag("bills_see_all_button")) {
                        Text(stringResource(R.string.bills_see_all, bills.size))
                    }
                }
            }
        }
    }
}

/** Start of the current calendar month, for "paid this month" counts. */
fun startOfCurrentMonth(): Long = Calendar.getInstance().apply {
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** Every bill, active ones by due date then paused ones, for the Planning tab. */
@Composable
fun AllBillsSection(
    bills: List<RecurringBillEntity>,
    accounts: List<AccountEntity>,
    currencySymbol: String,
    onBillClick: (RecurringBillEntity) -> Unit,
    onMarkPaid: (RecurringBillEntity) -> Unit,
    onAddBill: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = bills.filter { it.isActive }.sortedBy { it.nextDueDate }
    val paused = bills.filterNot { it.isActive }

    Column(modifier = modifier.fillMaxWidth().testTag("all_bills_section")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.bills_section_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    stringResource(R.string.bills_section_subtitle, formatMoney(currencySymbol, monthlyBillTotal(bills)), active.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = onAddBill, modifier = Modifier.testTag("all_bills_add")) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.bills_add))
            }
        }
        Spacer(Modifier.height(8.dp))
        if (bills.isEmpty()) {
            Text(
                stringResource(R.string.bills_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        active.forEach { bill ->
            BillRow(
                bill = bill,
                accountName = accounts.firstOrNull { it.id == bill.accountId }?.name,
                currencySymbol = currencySymbol,
                onClick = { onBillClick(bill) },
                onMarkPaid = if (BillSchedule.isDueForPayment(bill.nextDueDate, bill.reminderDaysBefore, bill.isActive)) {
                    { onMarkPaid(bill) }
                } else {
                    null
                }
            )
        }
        if (paused.isNotEmpty()) {
            Text(
                stringResource(R.string.bills_section_paused),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            paused.forEach { bill ->
                BillRow(
                    bill = bill,
                    accountName = accounts.firstOrNull { it.id == bill.accountId }?.name,
                    currencySymbol = currencySymbol,
                    onClick = { onBillClick(bill) },
                    onMarkPaid = null
                )
            }
        }
    }
}
