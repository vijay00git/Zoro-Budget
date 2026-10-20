package com.example.ui.savings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.SavingsEntryEntity
import com.example.data.local.entity.SavingsItemEntity
import com.example.data.savings.EntryType
import com.example.data.savings.GoldForm
import com.example.data.savings.MarketRates
import com.example.data.savings.SavingsCalculator
import com.example.data.savings.SavingsDetails
import com.example.data.savings.SavingsSection
import com.example.data.savings.SavingsStatus
import com.example.data.savings.SavingsTotals
import com.example.ui.components.formatMoney
import com.example.ui.dialogs.ConfirmDeleteDialog

/** One savings record: its inputs, the step-by-step calculation and its investment history. */
@Composable
fun SavingsDetailScreen(
    item: SavingsItemEntity,
    entries: List<SavingsEntryEntity>,
    rates: MarketRates,
    currencySymbol: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSaveEntry: (SavingsEntryEntity) -> Unit,
    onDeleteEntry: (SavingsEntryEntity) -> Unit
) {
    BackHandler(onBack = onBack)
    val today = remember { System.currentTimeMillis() }
    val section = SavingsSection.from(item.section)
    val style = section.style()
    val details = remember(item.detailsJson) { SavingsDetails.fromJson(item.detailsJson) }
    val totals = SavingsTotals(item.investedAmount, item.currentValue)
    var confirmDelete by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<SavingsEntryEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("savings_detail"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.savings_back)) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(stringResource(style.title), subTypeLabel(item)?.let { stringResource(it) }, item.holder).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = style.color)
                }
                IconButton(onClick = onEdit, modifier = Modifier.testTag("savings_detail_edit")) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.savings_edit)) }
                IconButton(onClick = { confirmDelete = true }, modifier = Modifier.testTag("savings_detail_delete")) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.savings_delete), tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, style.color)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.savings_current_value), style = MaterialTheme.typography.labelMedium)
                    Text(
                        if (totals.current > 0 || section.countsInTotal) formatMoney(currencySymbol, totals.current) else stringResource(R.string.savings_not_valued),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(4.dp))
                    if (totals.invested > 0 || section.countsInTotal) {
                        Text(stringResource(R.string.savings_invested_amount, formatMoney(currencySymbol, totals.invested)), style = MaterialTheme.typography.bodyMedium)
                    }
                    // A gain only means something when both what was paid and today's value are known.
                    if (section.countsInTotal || (totals.invested > 0 && totals.current > 0)) GainText(currencySymbol, totals.gain, totals.gainPct)
                    if (SavingsStatus.from(item.status) != SavingsStatus.ACTIVE) {
                        Text(stringResource(SavingsStatus.from(item.status).label()), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item { SavingsCalculationCard(item, details, entries, rates, currencySymbol, today) }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.savings_details_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    detailRows(item, details, currencySymbol).forEach { (label, value) -> LabelValueRow(label, value) }
                }
            }
        }

        item {
            HistoryCard(entries, currencySymbol, onAdd = {
                editingEntry = SavingsEntryEntity(itemId = item.id, date = SavingsCalculator.startOfDay(today), amount = 0.0)
            }, onEntryClick = { editingEntry = it })
        }
    }

    if (confirmDelete) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.savings_delete_title),
            message = stringResource(R.string.savings_delete_message, item.name),
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false }
        )
    }

    editingEntry?.let { entry ->
        EntryDialog(
            entry = entry,
            onDismiss = { editingEntry = null },
            onSave = {
                onSaveEntry(it)
                editingEntry = null
            },
            onDelete = if (entry.id != 0L) {
                {
                    onDeleteEntry(entry)
                    editingEntry = null
                }
            } else {
                null
            }
        )
    }
}

@Composable
private fun detailRows(item: SavingsItemEntity, details: SavingsDetails?, cs: String): List<Pair<String, String>> = buildList {
    when (details) {
        is SavingsDetails.Fd -> {
            add(stringResource(R.string.savings_field_principal) to formatMoney(cs, details.principal))
            add(stringResource(R.string.savings_field_rate) to "${formatPlain(details.ratePct)}%")
            add(
                stringResource(R.string.savings_field_interest) to
                    (details.payout?.let { stringResource(R.string.savings_payout_paid_out, stringResource(it.label())) } ?: stringResource(R.string.savings_payout_cumulative_compounded, stringResource(details.compounding.label())))
            )
            if (details.autoRenew) add(stringResource(R.string.savings_field_auto_renew) to stringResource(R.string.savings_yes))
        }
        is SavingsDetails.Rd -> {
            add(stringResource(R.string.savings_field_installment) to formatMoney(cs, details.installment))
            add(stringResource(R.string.savings_field_rate) to "${formatPlain(details.ratePct)}%")
            add(stringResource(R.string.savings_field_tenure_months) to details.tenureMonths.toString())
            add(stringResource(R.string.savings_field_installment_day) to details.installmentDay.toString())
        }
        is SavingsDetails.Stock -> {
            details.symbol?.let { add(stringResource(R.string.savings_field_symbol) to listOfNotNull(it, details.exchange).joinToString(" · ")) }
            add(stringResource(R.string.savings_field_quantity) to formatPlain(details.quantity))
            add(stringResource(R.string.savings_field_avg_price) to formatMoney(cs, details.avgPrice))
            add(stringResource(R.string.savings_field_current_price) to formatMoney(cs, details.currentPrice))
            details.priceDate?.let { add(stringResource(R.string.savings_field_price_date) to formatDay(it)) }
            if (details.dividends > 0) add(stringResource(R.string.savings_field_dividends) to formatMoney(cs, details.dividends))
            details.sector?.let { add(stringResource(R.string.savings_field_sector) to it) }
        }
        is SavingsDetails.MutualFund -> {
            add(stringResource(R.string.savings_field_fund_type) to listOfNotNull(stringResource(mfTypeLabel(details.fundType)), details.plan?.let { stringResource(if (it == "DIRECT") R.string.savings_mf_direct else R.string.savings_mf_regular) }).joinToString(" · "))
            details.sipAmount?.let { add(stringResource(R.string.savings_field_sip_amount) to formatMoney(cs, it) + (details.sipDay?.let { d -> " · $d" } ?: "")) }
            details.sipStoppedDate?.let { add(stringResource(R.string.savings_field_sip_stopped_date) to formatDay(it)) }
            details.lumpSum?.let { add(stringResource(R.string.savings_field_lump_sum) to formatMoney(cs, it)) }
            add(stringResource(R.string.savings_field_units) to formatPlain(details.units))
            add(stringResource(R.string.savings_field_nav) to formatMoney(cs, details.nav))
            details.navDate?.let { add(stringResource(R.string.savings_field_nav_date) to formatDay(it)) }
        }
        is SavingsDetails.Gold -> {
            add(stringResource(R.string.savings_field_form) to stringResource(details.form.label()))
            details.quantity?.let { add(stringResource(R.string.savings_field_quantity_pieces) to it.toString()) }
            if (details.form == GoldForm.SGB) {
                details.grams?.let { add(stringResource(R.string.savings_field_sgb_units) to formatPlain(it)) }
                details.sgbIssuePrice?.let { add(stringResource(R.string.savings_field_sgb_issue_price) to formatMoney(cs, it)) }
            } else {
                details.grams?.let { add(stringResource(R.string.savings_field_grams) to "${formatPlain(it)} g · ${details.purity.carats}K") }
            }
            details.purchaseAmount?.let { add(stringResource(R.string.savings_field_purchase_amount) to formatMoney(cs, it)) }
            if (details.makingCharges > 0) add(stringResource(R.string.savings_detail_making) to formatMoney(cs, details.makingCharges))
            if (details.gst > 0) add(stringResource(R.string.savings_detail_gst) to formatMoney(cs, details.gst))
            details.billNo?.let { add(stringResource(R.string.savings_field_bill_no) to it) }
        }
        is SavingsDetails.Silver -> {
            add(stringResource(R.string.savings_field_form) to stringResource(details.form.label()))
            details.quantity?.let { add(stringResource(R.string.savings_field_quantity_pieces) to it.toString()) }
            details.grams?.let { add(stringResource(R.string.savings_field_grams) to "${formatPlain(it)} g · ${formatPlain(details.purityPct)}%") }
            details.purchaseAmount?.let { add(stringResource(R.string.savings_field_purchase_amount) to formatMoney(cs, it)) }
        }
        is SavingsDetails.Land -> {
            add(stringResource(R.string.savings_field_property_type) to stringResource(details.propertyType.label()))
            details.area?.let { add(stringResource(R.string.savings_field_area) to "${formatPlain(it)} ${stringResource(details.areaUnit.label())}") }
            details.address?.let { add(stringResource(R.string.savings_field_address) to it) }
            details.surveyNo?.let { add(stringResource(R.string.savings_field_survey_no) to it) }
            details.pattaNo?.let { add(stringResource(R.string.savings_field_patta_no) to it) }
            details.purchasePrice?.let { add(stringResource(R.string.savings_field_land_price) to formatMoney(cs, it)) }
            if (details.stampDutyAndRegistration > 0) add(stringResource(R.string.savings_field_stamp_duty) to formatMoney(cs, details.stampDutyAndRegistration))
            if (details.brokerage > 0) add(stringResource(R.string.savings_field_brokerage) to formatMoney(cs, details.brokerage))
            if (details.otherCosts > 0) add(stringResource(R.string.savings_field_other_costs) to formatMoney(cs, details.otherCosts))
            details.currentValue?.let { add(stringResource(R.string.savings_field_land_value) to formatMoney(cs, it)) }
            if (details.monthlyRent > 0) add(stringResource(R.string.savings_field_monthly_rent) to formatMoney(cs, details.monthlyRent))
            if (details.loanOutstanding > 0) add(stringResource(R.string.savings_field_loan_outstanding) to formatMoney(cs, details.loanOutstanding))
        }
        is SavingsDetails.Scheme -> {
            add(stringResource(R.string.savings_field_balance) to formatMoney(cs, details.balance))
            if (details.yearlyContribution > 0) add(stringResource(R.string.savings_detail_yearly_contribution) to formatMoney(cs, details.yearlyContribution))
            if (details.ratePct > 0) add(stringResource(R.string.savings_field_rate) to "${formatPlain(details.ratePct)}%")
        }
        is SavingsDetails.Insurance -> {
            add(stringResource(R.string.savings_field_premium) to formatMoney(cs, details.premium) + " · " + stringResource(details.premiumFrequency.label()))
            add(stringResource(R.string.savings_field_term_years) to details.termYears.toString())
            if (details.sumAssured > 0) add(stringResource(R.string.savings_field_sum_assured) to formatMoney(cs, details.sumAssured))
        }
        is SavingsDetails.Chit -> {
            add(stringResource(R.string.savings_field_chit_value) to formatMoney(cs, details.chitValue))
            add(stringResource(R.string.savings_field_installment) to formatMoney(cs, details.monthlyInstallment))
            add(stringResource(R.string.savings_field_chit_paid) to "${details.monthsPaid} / ${details.totalMonths}")
        }
        is SavingsDetails.Bond -> {
            add(stringResource(R.string.savings_field_face_value) to formatMoney(cs, details.faceValue))
            add(stringResource(R.string.savings_field_coupon) to "${formatPlain(details.couponPct)}% · " + stringResource(details.payout.label()))
        }
        is SavingsDetails.LoanGiven -> {
            add(stringResource(R.string.savings_field_person) to details.person)
            if (details.ratePct > 0) add(stringResource(R.string.savings_field_loan_rate) to "${formatPlain(details.ratePct)}%")
        }
        is SavingsDetails.Generic, null -> Unit
    }
    add(stringResource(startDateLabel(details)) to formatDay(item.startDate))
    item.maturityDate?.let { add(stringResource(R.string.savings_field_maturity_date) to formatDay(it)) }
    add(stringResource(R.string.savings_field_holder) to item.holder)
    item.institution?.let { add(stringResource(R.string.savings_field_institution) to it) }
    item.refNo?.let { add(stringResource(R.string.savings_field_ref_no) to maskRef(it)) }
    item.nominee?.let { add(stringResource(R.string.savings_field_nominee) to it) }
    item.notes?.let { add(stringResource(R.string.savings_field_notes) to it) }
}

/** What the record's start date means for its kind of saving. */
private fun startDateLabel(details: SavingsDetails?): Int = when (details) {
    is SavingsDetails.Gold -> if (details.form == GoldForm.SGB) R.string.savings_field_issue_date else R.string.savings_field_purchase_date
    is SavingsDetails.Silver, is SavingsDetails.Land -> R.string.savings_field_purchase_date
    is SavingsDetails.Stock -> R.string.savings_field_buy_date
    is SavingsDetails.Scheme -> R.string.savings_field_open_date
    is SavingsDetails.Insurance -> R.string.savings_field_policy_start
    is SavingsDetails.LoanGiven -> R.string.savings_field_lent_on
    else -> R.string.savings_field_start_date
}

/** Shows only the last 4 characters of an account or folio number: ••••4821. */
fun maskRef(ref: String): String = if (ref.length <= 4) ref else "••••" + ref.takeLast(4)

@Composable
private fun HistoryCard(entries: List<SavingsEntryEntity>, cs: String, onAdd: () -> Unit, onEntryClick: (SavingsEntryEntity) -> Unit) {
    val summary = remember(entries) { SavingsCalculator.summarizeEntries(entries) }
    // Running net invested after each entry, oldest first, shown next to the newest-first list.
    val running = remember(entries) {
        var total = 0.0
        entries.sortedWith(compareBy({ it.date }, { it.id })).associate { e ->
            total += when (EntryType.from(e.type)) {
                EntryType.INVEST -> e.amount
                EntryType.WITHDRAW -> -e.amount
                EntryType.INCOME -> 0.0
            }
            e.id to total
        }
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.savings_history_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                TextButton(onClick = onAdd, modifier = Modifier.testTag("savings_history_add")) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.savings_history_add))
                }
            }
            LabelValueRow(stringResource(R.string.savings_history_total_invested), formatMoney(cs, summary.netInvested), emphasize = true)
            if (summary.totalWithdrawn > 0) LabelValueRow(stringResource(R.string.savings_history_total_withdrawn), formatMoney(cs, summary.totalWithdrawn))
            if (summary.totalIncome > 0) LabelValueRow(stringResource(R.string.savings_history_total_income), formatMoney(cs, summary.totalIncome))
            if (summary.investedByFinancialYear.size > 1) {
                Text(stringResource(R.string.savings_history_by_fy), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                summary.investedByFinancialYear.entries.reversed().forEach { (fy, amount) ->
                    LabelValueRow(stringResource(R.string.savings_history_fy, fy, (fy + 1) % 100), formatMoney(cs, amount))
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (entries.isEmpty()) {
                Text(stringResource(R.string.savings_history_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            entries.forEach { e ->
                val type = EntryType.from(e.type)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onEntryClick(e) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(formatDay(e.date), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(type.label()) + (e.note?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            (if (type == EntryType.WITHDRAW) "−" else "") + formatMoney(cs, e.amount),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (type == EntryType.INCOME) gainColor(1.0) else MaterialTheme.colorScheme.onSurface
                        )
                        running[e.id]?.let {
                            Text(stringResource(R.string.savings_history_running, formatMoney(cs, it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

fun EntryType.label(): Int = when (this) {
    EntryType.INVEST -> R.string.savings_entry_invest
    EntryType.WITHDRAW -> R.string.savings_entry_withdraw
    EntryType.INCOME -> R.string.savings_entry_income
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntryDialog(entry: SavingsEntryEntity, onDismiss: () -> Unit, onSave: (SavingsEntryEntity) -> Unit, onDelete: (() -> Unit)?) {
    var date by remember { mutableStateOf(entry.date) }
    var type by remember { mutableStateOf(EntryType.from(entry.type)) }
    var amount by remember { mutableStateOf(if (entry.amount > 0) formatPlain(entry.amount) else "") }
    var note by remember { mutableStateOf(entry.note ?: "") }
    val amountValue = parseAmount(amount)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (entry.id == 0L) R.string.savings_entry_add_title else R.string.savings_entry_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EntryType.entries.forEach { t -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(stringResource(t.label())) }) }
                }
                DateField(R.string.savings_entry_date, date, { date = it })
                NumberField(amount, { amount = it }, R.string.savings_entry_amount, isError = amount.isNotBlank() && (amountValue == null || amountValue <= 0))
                OutlinedTextField(note, { note = it }, singleLine = true, label = { Text(stringResource(R.string.savings_entry_note)) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = amountValue != null && amountValue > 0,
                onClick = { onSave(entry.copy(date = date, type = type.name, amount = amountValue!!, note = note.trim().ifBlank { null })) }
            ) { Text(stringResource(R.string.savings_save)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text(stringResource(R.string.savings_delete), color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.savings_cancel)) }
            }
        }
    )
}
