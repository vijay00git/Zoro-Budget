package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringBillEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.utils.BillFrequency
import com.example.data.utils.BillSchedule
import com.example.ui.components.frequencyName
import com.example.ui.theme.ExpenseRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Create or edit a recurring bill. With [fromTransaction] the form starts from that ledger entry
 * ("make this recurring"): same name, amount, account and category, first due date one period on.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddOrEditRecurringBillDialog(
    billToEdit: RecurringBillEntity? = null,
    fromTransaction: TransactionEntity? = null,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String = "₹",
    onDismiss: () -> Unit,
    onSave: (RecurringBillEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val defaultCategory = stringResource(R.string.recurring_bill_dialog_default_category)
    val initialFrequency = BillFrequency.from(billToEdit?.frequency)
    var name by remember { mutableStateOf(billToEdit?.name ?: fromTransaction?.merchantOrSender ?: fromTransaction?.title ?: "") }
    var amountText by remember { mutableStateOf((billToEdit?.amount ?: fromTransaction?.amount)?.let { formatPlain(it) } ?: "") }
    var type by remember { mutableStateOf(billToEdit?.type ?: fromTransaction?.type ?: "DEBIT") }
    var accountId by remember {
        mutableStateOf(billToEdit?.accountId ?: fromTransaction?.accountId ?: accounts.firstOrNull { it.isDefault }?.id ?: accounts.firstOrNull()?.id)
    }
    var categoryTag by remember { mutableStateOf(billToEdit?.categoryTag ?: fromTransaction?.categoryTag ?: defaultCategory) }
    var frequency by remember { mutableStateOf(initialFrequency) }
    var nextDueDate by remember {
        mutableStateOf(
            billToEdit?.nextDueDate
                ?: fromTransaction?.let {
                    val day = Calendar.getInstance().apply { timeInMillis = it.dateTimestamp }.get(Calendar.DAY_OF_MONTH)
                    BillSchedule.advance(it.dateTimestamp, BillFrequency.MONTHLY, day)
                }
                ?: BillSchedule.startOfDay(System.currentTimeMillis())
        )
    }
    var reminderDays by remember { mutableStateOf(billToEdit?.reminderDaysBefore ?: 3) }
    var notes by remember { mutableStateOf(billToEdit?.notes ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var accountMenuOpen by remember { mutableStateOf(false) }
    var categoryMenuOpen by remember { mutableStateOf(false) }

    val amount = amountText.replace(",", "").toDoubleOrNull()
    val amountError = amountText.isNotBlank() && (amount == null || amount <= 0.0)
    val canSave = name.isNotBlank() && amount != null && amount > 0.0 && accountId != null
    val dateFormat = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(if (billToEdit != null) R.string.bill_form_title_edit else R.string.bill_form_title_add),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                if (billToEdit != null && onDelete != null) {
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.testTag("bill_form_delete")) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.bill_form_delete), tint = ExpenseRed)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "DEBIT",
                        onClick = { type = "DEBIT" },
                        label = { Text(stringResource(R.string.bill_form_type_bill)) }
                    )
                    FilterChip(
                        selected = type == "CREDIT",
                        onClick = { type = "CREDIT" },
                        label = { Text(stringResource(R.string.bill_form_type_income)) }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.bill_form_name)) },
                    placeholder = { Text(stringResource(R.string.bill_form_name_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().testTag("bill_form_name")
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> amountText = input.filter { it.isDigit() || it == '.' || it == ',' } },
                    label = { Text(stringResource(R.string.bill_form_amount)) },
                    prefix = { Text(currencySymbol) },
                    isError = amountError,
                    supportingText = if (amountError) {
                        { Text(stringResource(R.string.bill_form_amount_error)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().testTag("bill_form_amount")
                )

                Text(stringResource(R.string.bill_form_frequency), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BillFrequency.entries.forEach { option ->
                        FilterChip(
                            selected = frequency == option,
                            onClick = { frequency = option },
                            label = { Text(frequencyName(option)) },
                            modifier = Modifier.testTag("bill_form_frequency_${option.name}")
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth().testTag("bill_form_due_date")
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.bill_form_next_due, dateFormat.format(nextDueDate)))
                }

                Text(stringResource(R.string.bill_form_reminder), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 1, 3, 7).forEach { days ->
                        FilterChip(
                            selected = reminderDays == days,
                            onClick = { reminderDays = days },
                            label = {
                                Text(
                                    if (days == 0) {
                                        stringResource(R.string.bill_form_reminder_same_day)
                                    } else {
                                        stringResource(R.string.bill_form_reminder_days, days)
                                    }
                                )
                            }
                        )
                    }
                }

                ExposedDropdownMenuBox(expanded = accountMenuOpen, onExpandedChange = { accountMenuOpen = it }) {
                    OutlinedTextField(
                        value = accounts.firstOrNull { it.id == accountId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.bill_form_account)) },
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

                ExposedDropdownMenuBox(expanded = categoryMenuOpen, onExpandedChange = { categoryMenuOpen = it }) {
                    OutlinedTextField(
                        value = categoryTag,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.bill_form_category)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuOpen) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = categoryMenuOpen, onDismissRequest = { categoryMenuOpen = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    categoryTag = cat.name
                                    categoryMenuOpen = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.bill_form_notes)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    val dueDay = Calendar.getInstance().apply { timeInMillis = nextDueDate }.get(Calendar.DAY_OF_MONTH)
                    val base = billToEdit ?: RecurringBillEntity(
                        name = "",
                        amount = 0.0,
                        accountId = 0L,
                        categoryTag = "",
                        nextDueDate = nextDueDate,
                        dueDay = dueDay,
                        matchPayee = fromTransaction?.merchantOrSender
                    )
                    onSave(
                        base.copy(
                            name = name.trim(),
                            amount = amount ?: 0.0,
                            type = type,
                            accountId = accountId ?: 0L,
                            categoryTag = categoryTag,
                            frequency = frequency.name,
                            nextDueDate = BillSchedule.startOfDay(nextDueDate),
                            dueDay = dueDay,
                            reminderDaysBefore = reminderDays,
                            notes = notes.ifBlank { null }
                        )
                    )
                },
                modifier = Modifier.testTag("bill_form_save")
            ) { Text(stringResource(R.string.bill_form_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.bill_form_cancel)) }
        }
    )

    if (showDatePicker) {
        // The Material date picker works in UTC midnights; convert to and from local days.
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = localDayToUtc(nextDueDate))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { nextDueDate = utcDayToLocal(it) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.bill_form_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.bill_form_cancel)) } }
        ) { DatePicker(state = pickerState) }
    }

    if (showDeleteConfirm && onDelete != null) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.bill_form_delete_title),
            message = stringResource(R.string.bill_form_delete_message),
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

private fun formatPlain(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value)

private fun localDayToUtc(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun utcDayToLocal(utcMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
