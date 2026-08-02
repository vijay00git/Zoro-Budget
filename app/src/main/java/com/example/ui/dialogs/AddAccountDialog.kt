package com.example.ui.dialogs

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
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AccountEntity
import com.example.ui.theme.ExpenseRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditAccountDialog(
    accountToEdit: AccountEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        type: String,
        balance: Double,
        last4: String?,
        colorHex: String,
        senderKeywords: String?,
        customTypeLabel: String?
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(accountToEdit?.name ?: "") }
    var selectedType by remember {
        mutableStateOf(
            when (accountToEdit?.type) {
                "CHECKING", "SAVINGS", "CREDIT_CARD", "CASH", "INVESTMENT" -> accountToEdit.type
                null -> "CHECKING"
                else -> "CUSTOM"
            }
        )
    }
    var customTypeLabel by remember { mutableStateOf(accountToEdit?.customTypeLabel ?: (if (accountToEdit?.type !in listOf("CHECKING", "SAVINGS", "CREDIT_CARD", "CASH", "INVESTMENT", null)) accountToEdit?.type else "")) }
    var initialBalanceText by remember { mutableStateOf(accountToEdit?.balance?.toString() ?: "0.00") }
    var last4Digits by remember { mutableStateOf(accountToEdit?.accountNumberLast4 ?: "") }
    var senderKeywords by remember { mutableStateOf(accountToEdit?.senderKeywords ?: "") }
    var selectedColorHex by remember { mutableStateOf(accountToEdit?.colorHex ?: "#2563EB") }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    val accountTypes = listOf(
        "CHECKING" to "Checking Account",
        "SAVINGS" to "Savings Account",
        "CREDIT_CARD" to "Credit Card",
        "CASH" to "Cash / Wallet",
        "INVESTMENT" to "Investment Account",
        "CUSTOM" to "Custom Account Type..."
    )

    val colorOptions = listOf(
        "#2563EB", "#059669", "#7C3AED", "#D97706", "#DC2626", "#0284C7", "#059669", "#4F46E5"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (accountToEdit != null) "Edit Account" else "Add New Account",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                if (accountToEdit != null && onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
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
                // Account Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    placeholder = { Text("e.g. HDFC Bank, Chase Checking, INDBNK") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_name_input"),
                    singleLine = true
                )

                // Account Type Dropdown
                Text(
                    text = "Account Type",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                ) {
                    val label = accountTypes.find { it.first == selectedType }?.second ?: "Checking Account"
                    OutlinedTextField(
                        value = label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        accountTypes.forEach { (typeKey, typeLabel) ->
                            DropdownMenuItem(
                                text = { Text(typeLabel) },
                                onClick = {
                                    selectedType = typeKey
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Custom Account Type Name (shown if CUSTOM selected)
                if (selectedType == "CUSTOM") {
                    OutlinedTextField(
                        value = customTypeLabel ?: "",
                        onValueChange = { customTypeLabel = it },
                        label = { Text("Custom Account Type Name") },
                        placeholder = { Text("e.g. Fixed Deposit, PayPal, Chit Fund, Crypto") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_account_type_input"),
                        singleLine = true
                    )
                }

                // Initial / Current Balance
                OutlinedTextField(
                    value = initialBalanceText,
                    onValueChange = { initialBalanceText = it },
                    label = { Text("Current Balance ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // SMS Sender Contacts / Bank Keywords Mapping
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ContactPhone,
                                contentDescription = "Bank Contacts",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bank SMS Sender / Contact IDs",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Link contact headers (e.g. VD-HDFCBK-T, AD-HDFCBK-S, BV-INDBNK-S) so incoming debit/credit messages route directly here.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = senderKeywords,
                    onValueChange = { senderKeywords = it },
                    label = { Text("Bank SMS Sender Contacts / Keywords") },
                    placeholder = { Text("e.g. VD-HDFCBK-T, AD-HDFCBK-S, HDFCBK") },
                    supportingText = { Text("Comma-separated sender IDs or contact headers") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_sender_keywords_input")
                )

                // Last 4 Digits for SMS Auto Matching
                OutlinedTextField(
                    value = last4Digits,
                    onValueChange = { if (it.length <= 4) last4Digits = it },
                    label = { Text("Account / Card Last 4 Digits") },
                    placeholder = { Text("e.g. 4321") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_last4_input"),
                    singleLine = true
                )

                // Color picker
                Text(
                    text = "Account Color Theme",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorOptions.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColorHex = hex }
                                .then(
                                    if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val balance = initialBalanceText.toDoubleOrNull() ?: 0.0
                        val finalType = if (selectedType == "CUSTOM") (customTypeLabel?.ifBlank { "CUSTOM" } ?: "CUSTOM") else selectedType
                        onSave(
                            name,
                            finalType,
                            balance,
                            last4Digits.ifBlank { null },
                            selectedColorHex,
                            senderKeywords.ifBlank { null },
                            customTypeLabel?.ifBlank { null }
                        )
                    }
                },
                modifier = Modifier.testTag("save_account_button")
            ) {
                Text(if (accountToEdit != null) "Save Changes" else "Create Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Backwards compatibility wrapper for AddAccountDialog
@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, type: String, balance: Double, last4: String?, colorHex: String) -> Unit
) {
    AddOrEditAccountDialog(
        accountToEdit = null,
        onDismiss = onDismiss,
        onSave = { name, type, balance, last4, colorHex, _, _ ->
            onAdd(name, type, balance, last4, colorHex)
        }
    )
}
