package com.example.ui.dialogs
import com.example.R

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.ui.theme.ExpenseRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditRecurringBillDialog(
    billToEdit: TransactionEntity? = null,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        type: String,
        accountId: Long,
        categoryTag: String,
        frequency: String,
        dueDate: Int,
        notes: String?,
        receiptUri: String?
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val defaultCategoryTag = stringResource(R.string.recurring_bill_dialog_default_category)
    var title by remember { mutableStateOf(billToEdit?.title ?: "") }
    var amountText by remember { mutableStateOf(billToEdit?.amount?.toString() ?: "") }
    var selectedType by remember { mutableStateOf(billToEdit?.type ?: "DEBIT") } // "DEBIT" or "CREDIT"
    var selectedAccountId by remember { mutableStateOf(billToEdit?.accountId ?: accounts.firstOrNull()?.id ?: 1L) }
    var selectedCategoryTag by remember { mutableStateOf(billToEdit?.categoryTag ?: defaultCategoryTag) }
    var frequency by remember { mutableStateOf(billToEdit?.recurringFrequency ?: "MONTHLY") } // "MONTHLY", "WEEKLY", "YEARLY"
    var dueDate by remember { mutableIntStateOf(billToEdit?.recurringDueDate ?: 4) } // Default day 4 for EMI
    var dueDateText by remember { mutableStateOf(billToEdit?.recurringDueDate?.toString() ?: "4") }
    var notes by remember { mutableStateOf(billToEdit?.notes ?: "") }
    var receiptUri by remember { mutableStateOf<String?>(billToEdit?.receiptUri) }

    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            receiptUri = uri.toString()
        }
    }

    val daysOfWeek = listOf(
        1 to stringResource(R.string.recurring_bill_dialog_day_monday),
        2 to stringResource(R.string.recurring_bill_dialog_day_tuesday),
        3 to stringResource(R.string.recurring_bill_dialog_day_wednesday),
        4 to stringResource(R.string.recurring_bill_dialog_day_thursday),
        5 to stringResource(R.string.recurring_bill_dialog_day_friday),
        6 to stringResource(R.string.recurring_bill_dialog_day_saturday),
        7 to stringResource(R.string.recurring_bill_dialog_day_sunday)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (billToEdit != null) stringResource(R.string.recurring_bill_dialog_title_edit) else stringResource(R.string.recurring_bill_dialog_title_add),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (billToEdit != null && onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.recurring_bill_dialog_delete_cd), tint = ExpenseRed)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.recurring_bill_dialog_name_label)) },
                    placeholder = { Text(stringResource(R.string.recurring_bill_dialog_name_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recurring_bill_title_input"),
                    singleLine = true
                )

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.recurring_bill_dialog_amount_label, currencySymbol)) },
                    placeholder = { Text(stringResource(R.string.recurring_bill_dialog_amount_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recurring_bill_amount_input"),
                    singleLine = true
                )

                // Recurring Schedule / Frequency Selector
                Text(
                    text = stringResource(R.string.recurring_bill_dialog_frequency_label),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("MONTHLY", "WEEKLY", "YEARLY").forEach { freqKey ->
                        FilterChip(
                            selected = frequency == freqKey,
                            onClick = {
                                frequency = freqKey
                                if (freqKey == "WEEKLY" && dueDate > 7) {
                                    dueDate = 1
                                    dueDateText = "1"
                                }
                            },
                            label = {
                                Text(
                                    text = when (freqKey) {
                                        "MONTHLY" -> stringResource(R.string.recurring_bill_dialog_freq_monthly)
                                        "WEEKLY" -> stringResource(R.string.recurring_bill_dialog_freq_weekly)
                                        else -> stringResource(R.string.recurring_bill_dialog_freq_yearly)
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Custom Due Date / Day Picker
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (frequency) {
                                    "WEEKLY" -> stringResource(R.string.recurring_bill_dialog_due_label_weekly)
                                    "YEARLY" -> stringResource(R.string.recurring_bill_dialog_due_label_yearly)
                                    else -> stringResource(R.string.recurring_bill_dialog_due_label_monthly)
                                },
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (frequency == "WEEKLY") {
                            // Day of week selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                daysOfWeek.forEach { (dayNum, dayName) ->
                                    val isSelected = dueDate == dayNum
                                    Surface(
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { dueDate = dayNum }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayName.take(3),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Monthly Day of Month TextField & Quick Presets
                            OutlinedTextField(
                                value = dueDateText,
                                onValueChange = { input ->
                                    if (input.isBlank()) {
                                        dueDateText = ""
                                    } else {
                                        val parsed = input.toIntOrNull()
                                        if (parsed != null && parsed in 1..31) {
                                            dueDate = parsed
                                            dueDateText = parsed.toString()
                                        }
                                    }
                                },
                                label = { Text(stringResource(R.string.recurring_bill_dialog_due_day_field_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                supportingText = {
                                    Text(stringResource(R.string.recurring_bill_dialog_alerts_supporting_text, dueDate))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recurring_bill_duedate_input"),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = stringResource(R.string.recurring_bill_dialog_quick_date_presets_label),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(1, 4, 5, 10, 15, 25, 30).forEach { day ->
                                    FilterChip(
                                        selected = dueDate == day,
                                        onClick = {
                                            dueDate = day
                                            dueDateText = day.toString()
                                        },
                                        label = { Text(stringResource(R.string.recurring_bill_dialog_day_preset, day), style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Account Selection
                ExposedDropdownMenuBox(
                    expanded = accountDropdownExpanded,
                    onExpandedChange = { accountDropdownExpanded = !accountDropdownExpanded }
                ) {
                    val currentAccountName = accounts.find { it.id == selectedAccountId }?.name ?: stringResource(R.string.recurring_bill_dialog_select_account)
                    OutlinedTextField(
                        value = currentAccountName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.recurring_bill_dialog_pay_from_account_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = accountDropdownExpanded,
                        onDismissRequest = { accountDropdownExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.recurring_bill_dialog_account_item, acc.name, acc.type)) },
                                onClick = {
                                    selectedAccountId = acc.id
                                    accountDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Category Selection
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategoryTag,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.recurring_bill_dialog_category_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryTag = cat.name
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.recurring_bill_dialog_notes_label)) },
                    placeholder = { Text(stringResource(R.string.recurring_bill_dialog_notes_placeholder)) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Capture & Attach Bill Image Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.recurring_bill_dialog_receipt_section_title),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (receiptUri == null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("attach_recurring_bill_image_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = stringResource(R.string.recurring_bill_dialog_capture_image_cd),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.recurring_bill_dialog_attach_image_button), style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = { receiptUri = "sample_receipt_utility_bill" },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("attach_recurring_sample_bill_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = stringResource(R.string.recurring_bill_dialog_sample_bill),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.recurring_bill_dialog_sample_bill), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val uriStr = receiptUri ?: ""
                                    if (uriStr.startsWith("content://") || uriStr.startsWith("file://") || uriStr.startsWith("http")) {
                                        AsyncImage(
                                            model = uriStr,
                                            contentDescription = stringResource(R.string.recurring_bill_dialog_receipt_preview_cd),
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Surface(
                                            color = com.example.ui.theme.IncomeGreenContainer,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Receipt,
                                                    contentDescription = stringResource(R.string.recurring_bill_dialog_receipt_icon_cd),
                                                    tint = com.example.ui.theme.IncomeGreen,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = stringResource(R.string.recurring_bill_dialog_receipt_attached),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        )
                                        Text(
                                            text = if (uriStr == "sample_receipt_utility_bill") stringResource(R.string.recurring_bill_dialog_demo_invoice) else stringResource(R.string.recurring_bill_dialog_image_attached),
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { receiptUri = null },
                                    modifier = Modifier.testTag("remove_recurring_receipt_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.recurring_bill_dialog_remove_receipt_cd),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amountText.toDoubleOrNull()
                    val resolvedDueDate = dueDateText.toIntOrNull()?.coerceIn(1, 31) ?: dueDate
                    if (title.isNotBlank() && parsedAmount != null && parsedAmount > 0) {
                        onSave(
                            title,
                            parsedAmount,
                            selectedType,
                            selectedAccountId,
                            selectedCategoryTag,
                            frequency,
                            resolvedDueDate,
                            notes.ifBlank { null },
                            receiptUri
                        )
                    }
                },
                modifier = Modifier.testTag("save_recurring_bill_button")
            ) {
                Text(if (billToEdit != null) stringResource(R.string.recurring_bill_dialog_save_button) else stringResource(R.string.recurring_bill_dialog_add_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.recurring_bill_dialog_cancel_button))
            }
        }
    )
}
