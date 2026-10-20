package com.example.ui.screens

import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.R
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.ui.components.AccountBadge
import com.example.ui.components.BentoTile
import com.example.ui.components.CategoryAvatar
import com.example.ui.components.formatMoney
import com.example.ui.components.heroColor
import com.example.ui.dialogs.ConfirmDeleteDialog
import com.example.ui.dialogs.EditSavingsGoalDialog
import com.example.ui.dialogs.LowBalanceThresholdDialog
import com.example.ui.dialogs.TotalBudgetDialog
import com.example.ui.theme.accentOf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CurrencyOption(val symbol: String, val code: String, val name: String)

/**
 * Settings as a bento grid: accounts as coloured tiles, SMS sync, theme and currency side by
 * side, the three money targets as small tiles, categories as a compact grid, then backup,
 * storage and privacy.
 */
@Composable
fun SettingsScreen(
    accounts: List<AccountEntity> = emptyList(),
    importStatusMessage: String? = null,
    currencySymbol: String,
    themeMode: String = "SYSTEM",
    savingsGoalTarget: Double,
    lowBalanceThreshold: Double = 200.0,
    categories: List<CategoryEntity>,
    onSelectCurrency: (String) -> Unit,
    onSelectThemeMode: (String) -> Unit = {},
    onUpdateSavingsGoalTarget: (Double) -> Unit,
    onSetTotalMonthlyBudget: (Double) -> Unit,
    onSetLowBalanceThreshold: (Double) -> Unit = {},
    onEditCategoryBudget: (CategoryEntity) -> Unit,
    onAddCustomCategoryClick: () -> Unit,
    onOpenBackupDialog: () -> Unit,
    onScanInbox: () -> Unit = {},
    smsSyncFromDate: Long? = null,
    onSetSmsSyncFromDate: (Long?) -> Unit = {},
    onAddAccountClick: () -> Unit = {},
    onTransferClick: (AccountEntity?) -> Unit = {},
    onEditAccount: (AccountEntity) -> Unit = {},
    onDeleteAccount: (AccountEntity) -> Unit = {},
    onClearCache: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSavingsGoalDialog by remember { mutableStateOf(false) }
    var showTotalBudgetDialog by remember { mutableStateOf(false) }
    var showLowBalanceDialog by remember { mutableStateOf(false) }
    var deletingAccount by remember { mutableStateOf<AccountEntity?>(null) }

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    // READ_SMS (inbox scan) and RECEIVE_SMS (live messages) are separate permissions; ask for both.
    val smsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val granted = results.values.all { it }
        hasSmsPermission = granted
        if (granted) onScanInbox()
    }

    val currencies = listOf(
        CurrencyOption("₹", "INR", stringResource(R.string.settings_currency_name_inr)),
        CurrencyOption("$", "USD", stringResource(R.string.settings_currency_name_usd)),
        CurrencyOption("€", "EUR", stringResource(R.string.settings_currency_name_eur)),
        CurrencyOption("£", "GBP", stringResource(R.string.settings_currency_name_gbp)),
        CurrencyOption("¥", "JPY", stringResource(R.string.settings_currency_name_jpy)),
        CurrencyOption("A$", "AUD", stringResource(R.string.settings_currency_name_aud)),
        CurrencyOption("C$", "CAD", stringResource(R.string.settings_currency_name_cad)),
        CurrencyOption("S$", "SGD", stringResource(R.string.settings_currency_name_sgd))
    )
    val totalBudgetLimit = categories.filter { !it.isIncomeCategory }.sumOf { it.monthlyBudgetLimit }
    val pad = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
    // Accounts, then Add and Transfer tiles, two to a row.
    val accountRows = (accounts.map { AccountCell.Existing(it) } + AccountCell.Add + (if (accounts.size > 1) listOf(AccountCell.Transfer) else emptyList())).chunked(2)
    val categoryRows = (categories.map<CategoryEntity, CategoryEntity?> { it } + null).chunked(3)

    LazyColumn(modifier = modifier.fillMaxSize().testTag("settings_screen"), contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)) {
        item {
            Text(
                stringResource(R.string.settings_bento_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        if (!importStatusMessage.isNullOrBlank()) {
            item {
                BentoTile(modifier = pad.fillMaxWidth(), contentPadding = 12) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.settings_screen_status_icon_desc), tint = accentOf("#3730A3").ink)
                        Spacer(Modifier.width(10.dp))
                        Text(importStatusMessage, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    }
                }
            }
        }

        // --- Accounts ---
        item { SectionLabel(stringResource(R.string.settings_bento_accounts)) }
        items(accountRows.size) { index ->
            Row(modifier = pad.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                accountRows[index].forEach { cell ->
                    val cellModifier = Modifier.weight(1f).fillMaxHeight()
                    when (cell) {
                        is AccountCell.Existing -> AccountSettingsTile(
                            account = cell.account,
                            currencySymbol = currencySymbol,
                            isLow = cell.account.type != "CREDIT_CARD" && cell.account.balance < lowBalanceThreshold,
                            onEdit = { onEditAccount(cell.account) },
                            onTransfer = { onTransferClick(cell.account) },
                            onDelete = { deletingAccount = cell.account },
                            modifier = cellModifier
                        )
                        AccountCell.Add -> ActionTile(Icons.Default.Add, stringResource(R.string.settings_accounts_add_account_button), "#3730A3", onAddAccountClick, cellModifier.testTag("settings_add_account"))
                        AccountCell.Transfer -> ActionTile(Icons.Default.SwapHoriz, stringResource(R.string.settings_accounts_transfer_button), "#155E75", { onTransferClick(null) }, cellModifier.testTag("settings_transfer"))
                    }
                }
                if (accountRows[index].size == 1) Spacer(Modifier.weight(1f))
            }
        }

        // --- SMS sync ---
        item {
            val accent = accentOf("#115E59")
            BentoTile(modifier = pad.fillMaxWidth().testTag("settings_sms_tile"), contentPadding = 16) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sms, contentDescription = null, tint = accent.ink)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.settings_bento_sms_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                }
                Text(stringResource(R.string.settings_bento_sms_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_sms_sync_from_date_label), style = MaterialTheme.typography.labelMedium, color = accent.ink)
                        Text(
                            if (smsSyncFromDate != null && smsSyncFromDate > 0L) {
                                stringResource(R.string.settings_sms_sync_ignoring_before_format, SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(smsSyncFromDate))
                            } else {
                                stringResource(R.string.settings_sms_sync_no_date_limit)
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    if (smsSyncFromDate != null && smsSyncFromDate > 0L) {
                        IconButton(onClick = { onSetSmsSyncFromDate(null) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.settings_sms_sync_clear_date_desc), tint = accent.ink)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            if (smsSyncFromDate != null && smsSyncFromDate > 0L) cal.timeInMillis = smsSyncFromDate
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val picked = Calendar.getInstance()
                                    picked.set(year, month, day, 0, 0, 0)
                                    picked.set(Calendar.MILLISECOND, 0)
                                    onSetSmsSyncFromDate(picked.timeInMillis)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.testTag("set_sms_sync_date_btn")
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(if (smsSyncFromDate != null && smsSyncFromDate > 0L) R.string.settings_sms_sync_change_date_label else R.string.settings_sms_sync_set_date_label))
                    }
                }
                Button(
                    onClick = {
                        if (hasSmsPermission) onScanInbox() else smsPermissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent.solid, contentColor = androidx.compose.ui.graphics.Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("sync_sms_inbox_button")
                ) {
                    Icon(if (hasSmsPermission) Icons.Default.Refresh else Icons.Default.Message, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (hasSmsPermission) R.string.settings_sms_sync_button_scan else R.string.settings_sms_sync_button_grant_permission))
                }
            }
        }

        // --- Appearance: theme and currency side by side ---
        item { SectionLabel(stringResource(R.string.settings_bento_appearance)) }
        item {
            Row(modifier = pad.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BentoTile(modifier = Modifier.weight(1f).fillMaxHeight().testTag("settings_theme_tile"), contentPadding = 12) {
                    Text(stringResource(R.string.settings_bento_theme), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    listOf(
                        Triple("SYSTEM", R.string.settings_theme_option_system, Icons.Default.SettingsBrightness),
                        Triple("LIGHT", R.string.settings_theme_option_light, Icons.Default.LightMode),
                        Triple("DARK", R.string.settings_theme_option_dark, Icons.Default.DarkMode)
                    ).forEach { (mode, label, icon) ->
                        val selected = themeMode == mode
                        Surface(
                            onClick = { onSelectThemeMode(mode) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) heroColor() else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth().testTag("theme_option_$mode")
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(label), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                            }
                        }
                    }
                }
                CurrencyTile(currencySymbol, currencies, onSelectCurrency, Modifier.weight(1f).fillMaxHeight())
            }
        }

        // --- Money targets: three small tiles ---
        item { SectionLabel(stringResource(R.string.settings_bento_targets)) }
        item {
            Row(modifier = pad.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TargetTile(Icons.Default.Savings, stringResource(R.string.settings_bento_savings_goal), shortMoney(currencySymbol, savingsGoalTarget), "#166534", { showSavingsGoalDialog = true }, Modifier.weight(1f).fillMaxHeight().testTag("settings_goal_tile"))
                TargetTile(
                    Icons.Default.Tune, stringResource(R.string.settings_bento_budget),
                    if (totalBudgetLimit > 0) shortMoney(currencySymbol, totalBudgetLimit) else stringResource(R.string.settings_bento_not_set),
                    "#1E40AF", { showTotalBudgetDialog = true }, Modifier.weight(1f).fillMaxHeight().testTag("settings_budget_tile")
                )
                TargetTile(Icons.Default.NotificationsActive, stringResource(R.string.settings_bento_low_alert), shortMoney(currencySymbol, lowBalanceThreshold), "#991B1B", { showLowBalanceDialog = true }, Modifier.weight(1f).fillMaxHeight().testTag("settings_low_alert_tile"))
            }
        }

        // --- Categories: compact three-column grid ---
        item { SectionLabel(stringResource(R.string.settings_bento_categories, categories.size)) }
        items(categoryRows.size) { index ->
            Row(modifier = pad.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categoryRows[index].forEach { category ->
                    val cellModifier = Modifier.weight(1f).fillMaxHeight()
                    if (category == null) {
                        ActionTile(Icons.Default.Category, stringResource(R.string.settings_bento_new_category), "#3730A3", onAddCustomCategoryClick, cellModifier.testTag("add_custom_category_button"))
                    } else {
                        CategoryMiniTile(category, currencySymbol, { onEditCategoryBudget(category) }, cellModifier)
                    }
                }
                repeat(3 - categoryRows[index].size) { Spacer(Modifier.weight(1f)) }
            }
        }

        // --- Data: backup and storage ---
        item { SectionLabel(stringResource(R.string.settings_bento_data)) }
        item {
            Row(modifier = pad.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val backup = accentOf("#3730A3")
                BentoTile(modifier = Modifier.weight(1f).fillMaxHeight().testTag("settings_backup_tile"), onClick = onOpenBackupDialog, contentPadding = 14) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = backup.ink, modifier = Modifier.size(26.dp))
                    Text(stringResource(R.string.settings_bento_backup), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold))
                    Text(stringResource(R.string.settings_bento_backup_body), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val storage = accentOf("#334155")
                BentoTile(modifier = Modifier.weight(1f).fillMaxHeight().testTag("settings_storage_tile"), onClick = onClearCache, contentPadding = 14) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = storage.ink, modifier = Modifier.size(26.dp))
                    Text(stringResource(R.string.settings_bento_clear_cache), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold))
                    Text(stringResource(R.string.settings_bento_clear_cache_body), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            BentoTile(modifier = pad.fillMaxWidth(), contentPadding = 14) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_privacy_title), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
                Text(stringResource(R.string.settings_privacy_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // Credits at the very bottom.
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp).testTag("settings_credits"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.app_logo),
                    contentDescription = stringResource(R.string.settings_logo_desc),
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_made_with), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = stringResource(R.string.settings_love),
                        tint = accentOf("#9D174D").ink,
                        modifier = Modifier.padding(horizontal = 4.dp).size(18.dp)
                    )
                    Text(stringResource(R.string.settings_by_vijay), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
                Text(
                    stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }

    if (showSavingsGoalDialog) {
        EditSavingsGoalDialog(
            currentGoalTarget = savingsGoalTarget,
            currencySymbol = currencySymbol,
            onDismiss = { showSavingsGoalDialog = false },
            onSaveGoalTarget = { onUpdateSavingsGoalTarget(it) }
        )
    }
    if (showTotalBudgetDialog) {
        TotalBudgetDialog(
            currentTotalBudget = totalBudgetLimit,
            currencySymbol = currencySymbol,
            onDismiss = { showTotalBudgetDialog = false },
            onSaveTotalBudget = { onSetTotalMonthlyBudget(it) }
        )
    }
    if (showLowBalanceDialog) {
        LowBalanceThresholdDialog(
            currentThreshold = lowBalanceThreshold,
            currencySymbol = currencySymbol,
            onDismiss = { showLowBalanceDialog = false },
            onSaveThreshold = { onSetLowBalanceThreshold(it) }
        )
    }
    deletingAccount?.let { account ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.settings_bento_delete_account_title, account.name),
            message = stringResource(R.string.settings_bento_delete_account_message),
            onConfirm = {
                onDeleteAccount(account)
                deletingAccount = null
            },
            onDismiss = { deletingAccount = null }
        )
    }
}

/** A cell of the accounts grid: an account, or the Add / Transfer action tiles. */
private sealed class AccountCell {
    data class Existing(val account: AccountEntity) : AccountCell()
    object Add : AccountCell()
    object Transfer : AccountCell()
}

private fun shortMoney(currencySymbol: String, amount: Double): String = formatMoney(currencySymbol, amount).substringBeforeLast(".00")

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 2.dp)
    )
}

/** An account in its own colour: badge, name, type, SMS digits and balance; tap to edit. */
@Composable
private fun AccountSettingsTile(
    account: AccountEntity,
    currencySymbol: String,
    isLow: Boolean,
    onEdit: () -> Unit,
    onTransfer: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = accentOf(account.colorHex)
    BentoTile(modifier = modifier.testTag("settings_account_${account.id}"), outline = if (isLow) accentOf("#991B1B").ink else null, onClick = onEdit, contentPadding = 12) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AccountBadge(account, size = 36.dp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onTransfer, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.SwapHoriz, contentDescription = stringResource(R.string.settings_accounts_transfer_funds_desc), tint = accent.ink, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.settings_accounts_delete_account_desc), tint = accentOf("#991B1B").ink, modifier = Modifier.size(20.dp))
            }
        }
        Text(account.name, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            listOfNotNull(
                (account.customTypeLabel ?: account.type).replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                account.accountNumberLast4?.takeIf { it.isNotBlank() }?.let { "SMS ••$it" }
            ).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = accent.ink
        )
        Text(
            shortMoney(currencySymbol, account.balance),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = if (isLow) accentOf("#991B1B").ink else MaterialTheme.colorScheme.onSurface
        )
        if (isLow) Text(stringResource(R.string.settings_accounts_low_balance_label), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accentOf("#991B1B").ink)
    }
}

/** A tile that does one thing: Add account, Transfer, New category. */
@Composable
private fun ActionTile(icon: ImageVector, label: String, hex: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = accentOf(hex)
    BentoTile(modifier = modifier, onClick = onClick, contentPadding = 12) {
        Icon(icon, contentDescription = null, tint = accent.ink, modifier = Modifier.size(26.dp))
        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), maxLines = 2)
    }
}

/** The app's currency: the symbol large, tap to pick another from a list. */
@Composable
private fun CurrencyTile(currencySymbol: String, currencies: List<CurrencyOption>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val current = currencies.firstOrNull { it.symbol == currencySymbol }
    val accent = accentOf("#854D0E")
    Box(modifier = modifier) {
        BentoTile(modifier = Modifier.fillMaxSize().testTag("settings_currency_tile"), onClick = { open = true }, contentPadding = 12) {
            Text(stringResource(R.string.settings_bento_currency), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Text(currencySymbol, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold), color = accent.ink)
            Text(current?.let { "${it.code} · ${it.name}" } ?: currencySymbol, style = MaterialTheme.typography.labelMedium, maxLines = 2)
            Text(stringResource(R.string.settings_bento_tap_to_change), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            currencies.forEach { option ->
                DropdownMenuItem(
                    text = { Text("${option.symbol}  ${option.code} · ${option.name}") },
                    onClick = {
                        onSelect(option.symbol)
                        open = false
                    }
                )
            }
        }
    }
}

/** One money target (savings goal, budget, low-balance alert): its value, tap to change. */
@Composable
private fun TargetTile(icon: ImageVector, label: String, value: String, hex: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = accentOf(hex)
    BentoTile(modifier = modifier, onClick = onClick, contentPadding = 12) {
        Icon(icon, contentDescription = null, tint = accent.ink, modifier = Modifier.size(22.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = accent.ink, maxLines = 2)
    }
}

/** A category in a small tile: icon, name and its monthly limit; tap to edit. */
@Composable
private fun CategoryMiniTile(category: CategoryEntity, currencySymbol: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = accentOf(category.colorHex)
    BentoTile(modifier = modifier.testTag("settings_category_${category.name}"), onClick = onClick, contentPadding = 10) {
        CategoryAvatar(category.iconName, category.name, category.colorHex, size = 32.dp)
        Text(category.name, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            if (category.monthlyBudgetLimit > 0) shortMoney(currencySymbol, category.monthlyBudgetLimit) else stringResource(R.string.settings_bento_no_limit),
            style = MaterialTheme.typography.labelSmall,
            color = if (category.monthlyBudgetLimit > 0) accent.ink else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
