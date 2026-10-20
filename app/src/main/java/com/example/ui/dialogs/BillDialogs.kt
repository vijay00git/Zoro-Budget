package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.RecurringBillEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.utils.BillFrequency
import com.example.data.utils.BillSchedule
import com.example.ui.components.billStatusText
import com.example.ui.components.formatMoney
import com.example.ui.components.frequencyName
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Marks a bill's current cycle paid. If the payment already reached the ledger (usually as a
 * bank SMS), the user links that entry so it isn't counted twice; otherwise a new payment is
 * recorded. Either way the bill moves to its next due date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkBillPaidDialog(
    bill: RecurringBillEntity,
    accounts: List<AccountEntity>,
    currencySymbol: String,
    loadCandidates: suspend (RecurringBillEntity) -> List<TransactionEntity>,
    onDismiss: () -> Unit,
    onLinkExisting: (TransactionEntity) -> Unit,
    onRecordNew: (amount: Double, accountId: Long, paidAt: Long) -> Unit
) {
    var candidates by remember { mutableStateOf<List<TransactionEntity>?>(null) }
    LaunchedEffect(bill.id) { candidates = loadCandidates(bill) }

    // null = record a new payment; otherwise the id of the ledger entry to link
    var selectedCandidateId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(candidates) {
        // Pre-select the best match only when it's the exact amount
        selectedCandidateId = candidates?.firstOrNull { kotlin.math.abs(it.amount - bill.amount) < 0.01 }?.id
    }

    var amountText by remember { mutableStateOf(String.format(Locale.US, "%.2f", bill.amount)) }
    var accountId by remember { mutableStateOf(bill.accountId) }
    var paidOnDueDate by remember { mutableStateOf(false) }
    var accountMenuOpen by remember { mutableStateOf(false) }

    val amount = amountText.replace(",", "").toDoubleOrNull()
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val shortDate = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    val nextDue = BillSchedule.advance(bill.nextDueDate, BillFrequency.from(bill.frequency), bill.dueDay)
    val recordingNew = selectedCandidateId == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    stringResource(if (bill.type == "CREDIT") R.string.mark_paid_title_income else R.string.mark_paid_title, bill.name),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    stringResource(R.string.mark_paid_due_line, formatMoney(currencySymbol, bill.amount), dateFormat.format(bill.nextDueDate)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                val found = candidates
                if (found == null) {
                    Text(stringResource(R.string.mark_paid_looking), style = MaterialTheme.typography.bodySmall)
                } else if (found.isNotEmpty()) {
                    Text(
                        stringResource(R.string.mark_paid_already_in_ledger),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        stringResource(R.string.mark_paid_already_in_ledger_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    found.take(5).forEach { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = selectedCandidateId == tx.id, onClick = { selectedCandidateId = tx.id })
                                .padding(vertical = 4.dp)
                                .testTag("mark_paid_candidate_${tx.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedCandidateId == tx.id, onClick = { selectedCandidateId = tx.id })
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    tx.merchantOrSender ?: tx.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    listOfNotNull(shortDate.format(tx.dateTimestamp), accounts.firstOrNull { it.id == tx.accountId }?.name).joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(formatMoney(currencySymbol, tx.amount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = recordingNew, onClick = { selectedCandidateId = null })
                        .padding(vertical = 4.dp)
                        .testTag("mark_paid_record_new"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = recordingNew, onClick = { selectedCandidateId = null })
                    Text(stringResource(R.string.mark_paid_record_new), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                }

                if (recordingNew) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 12.dp)) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { input -> amountText = input.filter { it.isDigit() || it == '.' || it == ',' } },
                            label = { Text(stringResource(R.string.mark_paid_amount)) },
                            prefix = { Text(currencySymbol) },
                            isError = amount == null || amount <= 0.0,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("mark_paid_amount")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !paidOnDueDate,
                                onClick = { paidOnDueDate = false },
                                label = { Text(stringResource(R.string.mark_paid_today)) }
                            )
                            FilterChip(
                                selected = paidOnDueDate,
                                onClick = { paidOnDueDate = true },
                                label = { Text(stringResource(R.string.mark_paid_on_due_date, shortDate.format(bill.nextDueDate))) }
                            )
                        }
                        ExposedDropdownMenuBox(expanded = accountMenuOpen, onExpandedChange = { accountMenuOpen = it }) {
                            OutlinedTextField(
                                value = accounts.firstOrNull { it.id == accountId }?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.mark_paid_account)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountMenuOpen) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = accountMenuOpen, onDismissRequest = { accountMenuOpen = false }) {
                                accounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text(acc.name) },
                                        onClick = {
                                            accountId = acc.id
                                            accountMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    stringResource(R.string.mark_paid_moves_to, dateFormat.format(nextDue)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !recordingNew || (amount != null && amount > 0.0),
                onClick = {
                    val linked = candidates?.firstOrNull { it.id == selectedCandidateId }
                    if (linked != null) {
                        onLinkExisting(linked)
                    } else if (amount != null) {
                        val paidAt = if (paidOnDueDate) bill.nextDueDate + 12L * 60 * 60 * 1000 else System.currentTimeMillis()
                        onRecordNew(amount, accountId, paidAt)
                    }
                },
                modifier = Modifier.testTag("mark_paid_confirm")
            ) {
                Text(stringResource(if (recordingNew) R.string.mark_paid_confirm_record else R.string.mark_paid_confirm_link))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.bill_form_cancel)) } }
    )
}

/** A bill's details: status, schedule and payment history, with pay / skip / pause / edit. */
@Composable
fun BillDetailsDialog(
    bill: RecurringBillEntity,
    accounts: List<AccountEntity>,
    currencySymbol: String,
    payments: Flow<List<TransactionEntity>>,
    onDismiss: () -> Unit,
    onMarkPaid: () -> Unit,
    onSkip: () -> Unit,
    onSetActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onPaymentClick: (TransactionEntity) -> Unit
) {
    val history by payments.collectAsState(initial = emptyList())
    var confirmSkip by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val frequency = BillFrequency.from(bill.frequency)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(bill.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Text(
                    listOfNotNull(
                        formatMoney(currencySymbol, bill.amount),
                        frequencyName(frequency),
                        accounts.firstOrNull { it.id == bill.accountId }?.name
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(billStatusText(bill), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(
                    stringResource(R.string.bill_details_next_due, dateFormat.format(bill.nextDueDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                bill.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 10.dp)) {
                    if (bill.isActive) {
                        Button(onClick = onMarkPaid, modifier = Modifier.testTag("bill_details_mark_paid")) {
                            Text(stringResource(if (bill.type == "CREDIT") R.string.bill_mark_received else R.string.bill_mark_paid))
                        }
                        OutlinedButton(onClick = { confirmSkip = true }, modifier = Modifier.testTag("bill_details_skip")) {
                            Text(stringResource(R.string.bill_details_skip))
                        }
                    }
                }

                Text(
                    stringResource(R.string.bill_details_history, history.size),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                if (history.isEmpty()) {
                    Text(
                        stringResource(R.string.bill_details_no_history),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                history.take(12).forEach { payment ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onPaymentClick(payment) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(dateFormat.format(payment.dateTimestamp), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(formatMoney(currencySymbol, payment.amount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
                if (history.size > 1) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.bill_details_average, formatMoney(currencySymbol, history.map { it.amount }.average())),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onEdit, modifier = Modifier.testTag("bill_details_edit")) { Text(stringResource(R.string.bill_details_edit)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onSetActive(!bill.isActive) }, modifier = Modifier.testTag("bill_details_pause")) {
                    Text(stringResource(if (bill.isActive) R.string.bill_details_pause else R.string.bill_details_resume))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.bill_details_close)) }
            }
        }
    )

    if (confirmSkip) {
        val next = BillSchedule.advance(bill.nextDueDate, frequency, bill.dueDay)
        AlertDialog(
            onDismissRequest = { confirmSkip = false },
            title = { Text(stringResource(R.string.bill_skip_title)) },
            text = { Text(stringResource(R.string.bill_skip_message, dateFormat.format(bill.nextDueDate), dateFormat.format(next))) },
            confirmButton = {
                Button(onClick = {
                    confirmSkip = false
                    onSkip()
                }) { Text(stringResource(R.string.bill_details_skip)) }
            },
            dismissButton = { TextButton(onClick = { confirmSkip = false }) { Text(stringResource(R.string.bill_form_cancel)) } }
        )
    }
}
