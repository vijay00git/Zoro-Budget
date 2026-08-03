package com.example.ui.dialogs
import com.example.R

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onAdd: (title: String, amount: Double, type: String, accountId: Long, categoryTag: String, notes: String?, dateTimestamp: Long, receiptUri: String?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("DEBIT") } // DEBIT vs CREDIT
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 1L) }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Uncategorized") }
    var notes by remember { mutableStateOf("") }
    var receiptUri by remember { mutableStateOf<String?>(null) }
    var selectedDateTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            receiptUri = uri.toString()
        }
    }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.add_transaction_dialog_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Type Selector (Debit vs Credit)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = type == "DEBIT",
                        onClick = { type = "DEBIT" },
                        label = { Text(stringResource(R.string.add_transaction_dialog_type_expense)) },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = type == "CREDIT",
                        onClick = { type = "CREDIT" },
                        label = { Text(stringResource(R.string.add_transaction_dialog_type_income)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.add_transaction_dialog_amount_label, currencySymbol)) },
                    placeholder = { Text(stringResource(R.string.add_transaction_dialog_amount_placeholder)) },
                    leadingIcon = { Text(currencySymbol, style = MaterialTheme.typography.bodyLarge) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_amount_input"),
                    singleLine = true
                )

                // Quick Amount Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val presetAmounts = listOf(5, 10, 20, 50, 100, 500)
                    Text(
                        text = stringResource(R.string.add_transaction_dialog_quick_label),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    val quickAmountListState = androidx.compose.foundation.lazy.rememberLazyListState()
                    Box(modifier = Modifier.weight(1f)) {
                        androidx.compose.foundation.lazy.LazyRow(
                            state = quickAmountListState,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(presetAmounts.size) { index ->
                                val preset = presetAmounts[index]
                                OutlinedButton(
                                    onClick = {
                                        val current = amountText.toDoubleOrNull() ?: 0.0
                                        val updated = if (current == 0.0) preset.toDouble() else current + preset
                                        amountText = String.format(Locale.US, "%.2f", updated)
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(stringResource(R.string.add_transaction_dialog_quick_preset, preset), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        com.example.ui.components.ScrollEdgeFade(
                            quickAmountListState,
                            modifier = Modifier.matchParentSize(),
                            // AlertDialog's surface is surfaceContainerHigh, not the default
                            // colorScheme.background this component fades to elsewhere.
                            edgeColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.add_transaction_dialog_title_label)) },
                    placeholder = { Text(stringResource(R.string.add_transaction_dialog_title_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_title_input"),
                    singleLine = true
                )

                // Transaction Date Picker
                Column {
                    Text(
                        text = stringResource(R.string.add_transaction_dialog_date_label),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val isToday = remember(selectedDateTimestamp) { isSameDay(selectedDateTimestamp, System.currentTimeMillis()) }
                    val isYesterday = remember(selectedDateTimestamp) {
                        val yest = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                        isSameDay(selectedDateTimestamp, yest.timeInMillis)
                    }

                    // The formatted date ("EEE, MMM d, yyyy") needs the full dialog width to
                    // avoid truncating - putting it on its own row instead of sharing space with
                    // the Today/Yesterday chips guarantees that room.
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { datePickerDialog.show() }
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .testTag("select_transaction_date_button"),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = stringResource(R.string.add_transaction_dialog_select_date_cd),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = dateFormat.format(selectedDateTimestamp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = isToday,
                            onClick = { selectedDateTimestamp = System.currentTimeMillis() },
                            label = { Text(stringResource(R.string.add_transaction_dialog_today), style = MaterialTheme.typography.labelSmall) }
                        )

                        FilterChip(
                            selected = isYesterday,
                            onClick = {
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.DAY_OF_YEAR, -1)
                                selectedDateTimestamp = cal.timeInMillis
                            },
                            label = { Text(stringResource(R.string.add_transaction_dialog_yesterday), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Category Tag
                Text(
                    text = stringResource(R.string.add_transaction_dialog_category_tag_label),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isCatSelected = selectedCategory.equals(cat.name, ignoreCase = true)
                        val catColor = try {
                            Color(android.graphics.Color.parseColor(cat.colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedCategory = cat.name }
                                .border(
                                    width = if (isCatSelected) 2.dp else 1.dp,
                                    color = if (isCatSelected) catColor else catColor.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            color = if (isCatSelected) catColor else catColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isCatSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isCatSelected) Color.White else catColor
                                    )
                                )
                            }
                        }
                    }
                }

                // Account Selection Dropdown
                Text(
                    text = stringResource(R.string.add_transaction_dialog_account_label),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                ExposedDropdownMenuBox(
                    expanded = accountDropdownExpanded,
                    onExpandedChange = { accountDropdownExpanded = !accountDropdownExpanded }
                ) {
                    val currentAccountName = accounts.find { it.id == selectedAccountId }?.name ?: stringResource(R.string.add_transaction_dialog_select_account)
                    OutlinedTextField(
                        value = currentAccountName,
                        onValueChange = {},
                        readOnly = true,
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
                                text = { Text(stringResource(R.string.add_transaction_dialog_account_item, acc.name, currencySymbol, acc.balance)) },
                                onClick = {
                                    selectedAccountId = acc.id
                                    accountDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Notes Optional
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.add_transaction_dialog_notes_label)) },
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
                        text = stringResource(R.string.add_transaction_dialog_receipt_section_title),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (receiptUri == null) {
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("attach_bill_image_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = stringResource(R.string.add_transaction_dialog_capture_image_cd),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.add_transaction_dialog_attach_image_button), style = MaterialTheme.typography.labelMedium)
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
                                            contentDescription = stringResource(R.string.add_transaction_dialog_receipt_preview_cd),
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
                                                    contentDescription = stringResource(R.string.add_transaction_dialog_receipt_icon_cd),
                                                    tint = com.example.ui.theme.IncomeGreen,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = stringResource(R.string.add_transaction_dialog_receipt_attached),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        )
                                        Text(
                                            text = stringResource(R.string.add_transaction_dialog_image_attached_success),
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { receiptUri = null },
                                    modifier = Modifier.testTag("remove_receipt_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.add_transaction_dialog_remove_receipt_cd),
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
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        onAdd(title, amt, type, selectedAccountId, selectedCategory, notes.ifBlank { null }, selectedDateTimestamp, receiptUri)
                    }
                },
                modifier = Modifier.testTag("confirm_add_transaction_button")
            ) {
                Text(stringResource(R.string.add_transaction_dialog_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.add_transaction_dialog_cancel_button))
            }
        }
    )
}

private fun isSameDay(ts1: Long, ts2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = ts1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = ts2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
