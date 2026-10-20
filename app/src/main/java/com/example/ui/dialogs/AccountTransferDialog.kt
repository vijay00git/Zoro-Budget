package com.example.ui.dialogs
import com.example.R

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountTransferDialog(
    accounts: List<AccountEntity>,
    initialFromAccountId: Long? = null,
    initialToAccountId: Long? = null,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onTransfer: (fromAccount: AccountEntity, toAccount: AccountEntity, amount: Double, notes: String?, dateTimestamp: Long) -> Unit
) {
    val context = LocalContext.current
    val defaultAccountName = stringResource(R.string.account_transfer_dialog_default_account_name)

    // Default source account to selected or first account (the richest other one when topping
    // up a given destination account)
    var fromAccount by remember {
        mutableStateOf(
            accounts.find { it.id == initialFromAccountId }
                ?: initialToAccountId?.let { toId -> accounts.filter { it.id != toId }.maxByOrNull { it.balance } }
                ?: accounts.firstOrNull()
                ?: AccountEntity(id = 1, name = defaultAccountName, type = "CHECKING", balance = 0.0)
        )
    }

    // Default destination account to the requested one, or a different account than fromAccount
    var toAccount by remember {
        mutableStateOf(
            accounts.find { it.id == initialToAccountId && it.id != fromAccount.id }
                ?: accounts.find { it.id != fromAccount.id }
                ?: accounts.lastOrNull()
                ?: fromAccount
        )
    }

    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedDateTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    var fromDropdownExpanded by remember { mutableStateOf(false) }
    var toDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val sameAccountErrorText = stringResource(R.string.account_transfer_dialog_error_same_account)
    val invalidAmountErrorText = stringResource(R.string.account_transfer_dialog_error_invalid_amount)

    val calendar = remember { Calendar.getInstance() }
    calendar.timeInMillis = selectedDateTimestamp

    val datePickerDialog = remember(selectedDateTimestamp) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val now = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                cal.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                selectedDateTimestamp = cal.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val fromAccountColor = try {
        Color(android.graphics.Color.parseColor(fromAccount.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }
    val toAccountColor = try {
        Color(android.graphics.Color.parseColor(toAccount.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.secondary
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("account_transfer_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.account_transfer_dialog_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = stringResource(R.string.account_transfer_dialog_subtitle),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Swap Accounts Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.account_transfer_dialog_account_selection_label),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp
                        )
                    )

                    TextButton(
                        onClick = {
                            val temp = fromAccount
                            fromAccount = toAccount
                            toAccount = temp
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = stringResource(R.string.account_transfer_dialog_swap_cd),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.account_transfer_dialog_swap_button), style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Source Account Dropdown
                ExposedDropdownMenuBox(
                    expanded = fromDropdownExpanded,
                    onExpandedChange = { fromDropdownExpanded = !fromDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = stringResource(R.string.account_transfer_dialog_account_value, fromAccount.name, currencySymbol, fromAccount.balance),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.account_transfer_dialog_from_account_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("from_account_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = fromDropdownExpanded,
                        onDismissRequest = { fromDropdownExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = acc.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = stringResource(R.string.account_transfer_dialog_balance_line, currencySymbol, acc.balance),
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                },
                                onClick = {
                                    fromAccount = acc
                                    if (toAccount.id == acc.id) {
                                        toAccount = accounts.find { it.id != acc.id } ?: acc
                                    }
                                    fromDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Destination Account Dropdown
                ExposedDropdownMenuBox(
                    expanded = toDropdownExpanded,
                    onExpandedChange = { toDropdownExpanded = !toDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = stringResource(R.string.account_transfer_dialog_account_value, toAccount.name, currencySymbol, toAccount.balance),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.account_transfer_dialog_to_account_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("to_account_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = toDropdownExpanded,
                        onDismissRequest = { toDropdownExpanded = false }
                    ) {
                        accounts.filter { it.id != fromAccount.id }.forEach { acc ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = acc.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = stringResource(R.string.account_transfer_dialog_balance_line, currencySymbol, acc.balance),
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                },
                                onClick = {
                                    toAccount = acc
                                    toDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.account_transfer_dialog_amount_label, currencySymbol)) },
                    placeholder = { Text(stringResource(R.string.account_transfer_dialog_amount_placeholder)) },
                    leadingIcon = { Text(currencySymbol, style = MaterialTheme.typography.bodyLarge) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transfer_amount_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Date Picker trigger
                OutlinedTextField(
                    value = dateFormat.format(calendar.time),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.account_transfer_dialog_date_label)) },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = stringResource(R.string.account_transfer_dialog_select_date_cd))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() },
                    shape = RoundedCornerShape(12.dp)
                )

                // Optional Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.account_transfer_dialog_notes_label)) },
                    placeholder = { Text(stringResource(R.string.account_transfer_dialog_notes_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transfer_notes_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Live Impact Preview Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.account_transfer_dialog_impact_label),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Source Account Impact
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fromAccount.name,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                                Text(
                                    text = stringResource(R.string.account_transfer_dialog_current_balance, currencySymbol, fromAccount.balance),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = stringResource(R.string.account_transfer_dialog_after_balance, currencySymbol, fromAccount.balance - parsedAmount),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .size(20.dp)
                            )

                            // Destination Account Impact
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text(
                                    text = toAccount.name,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                                Text(
                                    text = stringResource(R.string.account_transfer_dialog_current_balance, currencySymbol, toAccount.balance),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = stringResource(R.string.account_transfer_dialog_after_balance, currencySymbol, toAccount.balance + parsedAmount),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                )
                            }
                        }

                        if (parsedAmount > fromAccount.balance) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.account_transfer_dialog_insufficient_balance_note),
                                    style = MaterialTheme.typography.labelSmall.copy(color = ExpenseRed)
                                )
                            }
                        }
                    }
                }

                // Error message banner
                errorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall.copy(color = ExpenseRed, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fromAccount.id == toAccount.id) {
                        errorMessage = sameAccountErrorText
                        return@Button
                    }
                    if (parsedAmount <= 0) {
                        errorMessage = invalidAmountErrorText
                        return@Button
                    }
                    onTransfer(fromAccount, toAccount, parsedAmount, notes.takeIf { it.isNotBlank() }, selectedDateTimestamp)
                    onDismiss()
                },
                modifier = Modifier.testTag("submit_transfer_btn")
            ) {
                Text(
                    text = if (parsedAmount > 0) {
                        stringResource(R.string.account_transfer_dialog_confirm_with_amount, currencySymbol, parsedAmount)
                    } else stringResource(R.string.account_transfer_dialog_confirm_button)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.account_transfer_dialog_cancel_button))
            }
        }
    )
}
