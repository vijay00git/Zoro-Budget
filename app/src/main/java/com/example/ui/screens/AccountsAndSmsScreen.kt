package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.Warning
import com.example.data.local.entity.AccountEntity
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PendingAmber
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountsAndSmsScreen(
    accounts: List<AccountEntity>,
    importStatusMessage: String?,
    currencySymbol: String = "$",
    lowBalanceThreshold: Double = 200.0,
    onScanInbox: () -> Unit,
    smsSyncFromDate: Long? = null,
    onSetSmsSyncFromDate: (Long?) -> Unit = {},
    onProcessSimulatedSms: (String) -> Unit,
    onAddAccountClick: () -> Unit,
    onTransferClick: (AccountEntity?) -> Unit = {},
    onEditAccount: (AccountEntity) -> Unit = {},
    onDeleteAccount: (AccountEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var simSmsInput by remember { mutableStateOf("") }

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
        if (isGranted) {
            onScanInbox()
        }
    }

    val sampleSmsPresets = listOf(
        "VD-HDFCBK-T: Rs. 1,500.00 debited on A/C ending 4321 at Starbucks Cafe." to "VD-HDFCBK-T Debit",
        "AD-HDFCBK-S: Your A/C XX4321 credited with Rs. 45,000.00 from Payroll." to "AD-HDFCBK-S Credit",
        "BV-INDBNK-S: Rs. 2,200.00 debited from A/C XX9876 to Amazon UPI." to "BV-INDBNK-S Debit",
        "BZ-INDBNK-S: Rs. 3,500.00 credited to INDBNK account XX9876." to "BZ-INDBNK-S Credit",
        "USD 89.50 debited on A/C ending 4321 at Target Superstore on 31-Jul-26." to "Target $89.50"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("accounts_sms_screen"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Accounts Section Header
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Accounts & Payment Sources",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (accounts.size >= 2) {
                            OutlinedButton(
                                onClick = { onTransferClick(null) },
                                modifier = Modifier.testTag("accounts_screen_transfer_btn")
                            ) {
                                Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = "Transfer", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Transfer")
                            }
                        }

                        Button(
                            onClick = onAddAccountClick,
                            modifier = Modifier.testTag("accounts_screen_add_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                }
            }
        }

        // Account Items
        items(accounts, key = { it.id }) { acc ->
            val accColor = try {
                Color(android.graphics.Color.parseColor(acc.colorHex))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .testTag("account_manage_item_${acc.id}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(accColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = acc.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = accColor
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = acc.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (!acc.customTypeLabel.isNullOrBlank()) acc.customTypeLabel else acc.type,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                                if (!acc.accountNumberLast4.isNullOrBlank()) {
                                    Surface(
                                        color = accColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "•${acc.accountNumberLast4}",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = accColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                            if (!acc.senderKeywords.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "SMS Senders: ${acc.senderKeywords}",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isLowBalance = acc.balance < lowBalanceThreshold
                        if (isLowBalance) {
                            Surface(
                                color = ExpenseRed.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Low Balance Alert",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Low Balance",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = ExpenseRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        Text(
                            text = String.format(Locale.getDefault(), "%s%,.2f", currencySymbol, acc.balance),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isLowBalance) ExpenseRed else MaterialTheme.colorScheme.onSurface
                            )
                        )

                        if (accounts.size > 1) {
                            IconButton(onClick = { onTransferClick(acc) }) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Transfer Funds",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(onClick = { onEditAccount(acc) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Account",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (accounts.size > 1) {
                            IconButton(onClick = { onDeleteAccount(acc) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Account",
                                    tint = ExpenseRed.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Status Toast / Message Banner
        item {
            AnimatedVisibility(visible = !importStatusMessage.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Status",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = importStatusMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // SMS Auto-Reader Section
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = "SMS Auto-Reader",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Automated Bank SMS Sync",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Smart Budget reads incoming and inbox bank messages from your financial institutions to automatically record debits & credits into your accounts.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sync Starting Date Control
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sync From Date",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val dateText = if (smsSyncFromDate != null && smsSyncFromDate > 0L) {
                                    val formatted = java.text.SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(java.util.Date(smsSyncFromDate))
                                    "Ignoring SMS before $formatted"
                                } else {
                                    "Reading all SMS (No date limit)"
                                }
                                Text(
                                    text = dateText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (smsSyncFromDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (smsSyncFromDate != null && smsSyncFromDate > 0L) {
                                    IconButton(
                                        onClick = { onSetSmsSyncFromDate(null) },
                                        modifier = Modifier.testTag("accounts_clear_sms_date_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear Date Limit",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        if (smsSyncFromDate != null && smsSyncFromDate > 0L) {
                                            cal.timeInMillis = smsSyncFromDate
                                        }
                                        android.app.DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                val selectedCal = Calendar.getInstance()
                                                selectedCal.set(year, month, dayOfMonth, 0, 0, 0)
                                                selectedCal.set(Calendar.MILLISECOND, 0)
                                                onSetSmsSyncFromDate(selectedCal.timeInMillis)
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    modifier = Modifier.testTag("accounts_set_sms_date_btn"),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Set Date",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (smsSyncFromDate != null && smsSyncFromDate > 0L) "Change" else "Set Date",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (hasSmsPermission) {
                                    onScanInbox()
                                } else {
                                    smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sync_sms_inbox_button")
                        ) {
                            Icon(
                                imageVector = if (hasSmsPermission) Icons.Default.Refresh else Icons.Default.Message,
                                contentDescription = "Sync"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasSmsPermission) "Scan & Sync SMS Inbox" else "Grant SMS Permission & Sync"
                            )
                        }
                    }
                }
            }
        }

        // Interactive SMS Simulator / Tester
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Test Message Parser & Simulator",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Tap a preset sample SMS or paste any custom bank text below to verify automatic credit/debit amount extraction & account matching:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sample Preset Buttons
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sampleSmsPresets.forEach { (fullSms, chipLabel) ->
                                OutlinedButton(
                                    onClick = { simSmsInput = fullSms },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(chipLabel, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = simSmsInput,
                            onValueChange = { simSmsInput = it },
                            label = { Text("SMS Message Text") },
                            placeholder = { Text("e.g. USD 45.00 debited on A/C ending 4321 at Starbucks Cafe") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sim_sms_input"),
                            minLines = 2,
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                onProcessSimulatedSms(simSmsInput)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("parse_sim_sms_btn")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Parse")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Parse & Add SMS Transaction")
                        }
                    }
                }
            }
        }
    }
}
