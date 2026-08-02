package com.example.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.ui.theme.ExpenseRed
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTransactionDialog(
    transaction: TransactionEntity,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, accountId: Long, notes: String?, isRecurring: Boolean, frequency: String?, dueDate: Int?, receiptUri: String?) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(transaction.title) }
    var selectedCategory by remember { mutableStateOf(transaction.categoryTag) }
    var selectedAccountId by remember { mutableStateOf(transaction.accountId) }
    var notes by remember { mutableStateOf(transaction.notes ?: "") }
    var receiptUri by remember { mutableStateOf<String?>(transaction.receiptUri) }
    var isRecurring by remember { mutableStateOf(transaction.isRecurring) }
    var recurringFrequency by remember { mutableStateOf(transaction.recurringFrequency ?: "MONTHLY") }
    var recurringDueDate by remember { androidx.compose.runtime.mutableIntStateOf(transaction.recurringDueDate ?: 4) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            receiptUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Transaction",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (transaction.isPendingReview) "Set Title & Category" else "Edit Transaction",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_transaction_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ExpenseRed
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Show raw SMS message preview if imported via SMS
                if (!transaction.rawSmsMessage.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Message,
                                    contentDescription = "SMS Source",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Auto-imported SMS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = transaction.rawSmsMessage,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                // Amount banner & Transaction Metadata
                val formattedDate = try {
                    java.text.SimpleDateFormat("EEEE, MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(java.util.Date(transaction.dateTimestamp))
                } catch (e: Exception) {
                    ""
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (transaction.type == "CREDIT") com.example.ui.theme.IncomeGreenContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Amount: ${if (transaction.type == "CREDIT") "+" else "-"}${String.format(Locale.getDefault(), "$%,.2f", transaction.amount)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (transaction.type == "CREDIT") com.example.ui.theme.IncomeGreen else MaterialTheme.colorScheme.primary
                            )
                        )
                        if (formattedDate.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Date: $formattedDate",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                // Transaction Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Transaction Title / Description") },
                    placeholder = { Text("e.g. Starbucks Morning Coffee, Grocery Shopping") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_title_input"),
                    singleLine = true
                )

                // Category Selection Chips
                Text(
                    text = "Category Tag",
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
                    text = "Account",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                ExposedDropdownMenuBox(
                    expanded = accountDropdownExpanded,
                    onExpandedChange = { accountDropdownExpanded = !accountDropdownExpanded }
                ) {
                    val currentAccountName = accounts.find { it.id == selectedAccountId }?.name ?: "Select Account"
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
                                text = { Text("${acc.name} (${String.format(Locale.getDefault(), "$%,.2f", acc.balance)})") },
                                onClick = {
                                    selectedAccountId = acc.id
                                    accountDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Notes Optional Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
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
                        text = "Capture / Attach Bill Receipt Image",
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
                                    .testTag("attach_bill_image_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Capture / Select Image",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Attach Image", style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = { receiptUri = "sample_receipt_utility_bill" },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("attach_sample_bill_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = "Sample Bill",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sample Bill", style = MaterialTheme.typography.labelMedium)
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
                                            contentDescription = "Bill Receipt Preview",
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
                                                    contentDescription = "Receipt Icon",
                                                    tint = com.example.ui.theme.IncomeGreen,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = "Bill Receipt Attached",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        )
                                        Text(
                                            text = if (uriStr == "sample_receipt_utility_bill") "Demo Bill Invoice #4812" else "Image attached",
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
                                        contentDescription = "Remove Receipt",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // Recurring Subscription Toggle
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Recurring Subscription / Bill",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Track fixed monthly/weekly recurring bill",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = isRecurring,
                            onCheckedChange = { isRecurring = it },
                            modifier = Modifier.testTag("recurring_subscription_switch")
                        )
                    }

                    if (isRecurring) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("MONTHLY" to "Monthly", "WEEKLY" to "Weekly", "YEARLY" to "Yearly").forEach { (fKey, fLabel) ->
                                androidx.compose.material3.FilterChip(
                                    selected = recurringFrequency == fKey,
                                    onClick = { recurringFrequency = fKey },
                                    label = { Text(fLabel, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = recurringDueDate.toString(),
                            onValueChange = { input ->
                                input.toIntOrNull()?.let { if (it in 1..31) recurringDueDate = it }
                            },
                            label = { Text("Due Day (e.g. Day 4 for EMI)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title,
                        selectedCategory,
                        selectedAccountId,
                        notes.ifBlank { null },
                        isRecurring,
                        if (isRecurring) recurringFrequency else null,
                        if (isRecurring) recurringDueDate else null,
                        receiptUri
                    )
                },
                modifier = Modifier.testTag("save_transaction_button")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
