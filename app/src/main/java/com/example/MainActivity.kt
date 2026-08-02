package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppDatabase
import com.example.data.local.BudgetRepository
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.ui.ExpenseViewModel
import com.example.ui.dialogs.AddAccountDialog
import com.example.ui.dialogs.AddOrEditAccountDialog
import com.example.ui.dialogs.AddCategoryDialog
import com.example.ui.dialogs.AddTransactionDialog
import com.example.ui.dialogs.EditCategoryBudgetDialog
import com.example.ui.dialogs.EditTransactionDialog
import com.example.ui.dialogs.ExportImportBackupDialog
import com.example.ui.screens.AccountsAndSmsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.SmartBudgetTheme
import com.example.utils.AppDebugLogger

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Overview", Icons.Default.Home)
    object Transactions : Screen("transactions", "Ledger", Icons.Default.ReceiptLong)
    object Reports : Screen("reports", "Planning", Icons.Default.BarChart)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: ExpenseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = BudgetRepository(db.accountDao(), db.transactionDao(), db.categoryDao())
        viewModel = ExpenseViewModel(repository)
        AppDebugLogger.init(applicationContext)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            SmartBudgetTheme(themeMode = themeMode) {
                SmartBudgetApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartBudgetApp(viewModel: ExpenseViewModel) {
    var selectedScreenIndex by remember { mutableIntStateOf(0) }
    val screens = listOf(
        Screen.Home,
        Screen.Transactions,
        Screen.Reports,
        Screen.Settings
    )

    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val pendingTransactions by viewModel.pendingTransactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val selectedAccountId by viewModel.selectedAccountId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val dateRangeFilter by viewModel.dateRangeFilter.collectAsStateWithLifecycle()
    val customStartDate by viewModel.customStartDate.collectAsStateWithLifecycle()
    val customEndDate by viewModel.customEndDate.collectAsStateWithLifecycle()
    val minAmountFilter by viewModel.minAmountFilter.collectAsStateWithLifecycle()
    val maxAmountFilter by viewModel.maxAmountFilter.collectAsStateWithLifecycle()
    val selectedMonthOffset by viewModel.selectedMonthOffset.collectAsStateWithLifecycle()
    val savingsGoalTarget by viewModel.savingsGoalTarget.collectAsStateWithLifecycle()
    val lowBalanceThreshold by viewModel.lowBalanceThreshold.collectAsStateWithLifecycle()
    val importStatusMessage by viewModel.importStatusMessage.collectAsStateWithLifecycle()
    val smsSyncFromDate by viewModel.smsSyncFromDate.collectAsStateWithLifecycle()

    // Dialog state holders
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var editingCategoryBudget by remember { mutableStateOf<CategoryEntity?>(null) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showAddRecurringBillDialog by remember { mutableStateOf(false) }
    var showAccountTransferDialog by remember { mutableStateOf(false) }
    var transferInitialFromAccountId by remember { mutableStateOf<Long?>(null) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadPreferences(context)
    }

    LaunchedEffect(importStatusMessage) {
        importStatusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Smart Budget",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showBackupDialog = true },
                        modifier = Modifier.testTag("topbar_backup_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export & Backup",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (pendingTransactions.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.setTypeFilter("PENDING")
                                selectedScreenIndex = 1 // Go to ledger tab
                            },
                            modifier = Modifier.testTag("pending_notification_topbar")
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = PendingAmber) {
                                        Text("${pendingTransactions.size}")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Needs Title/Tag",
                                    tint = PendingAmber
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                screens.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        selected = selectedScreenIndex == index,
                        onClick = { selectedScreenIndex = index },
                        icon = {
                            if (screen == Screen.Transactions && pendingTransactions.isNotEmpty()) {
                                BadgedBox(
                                    badge = { Badge(containerColor = PendingAmber) { Text("${pendingTransactions.size}") } }
                                ) {
                                    Icon(imageVector = screen.icon, contentDescription = screen.title)
                                }
                            } else {
                                Icon(imageVector = screen.icon, contentDescription = screen.title)
                            }
                        },
                        label = { Text(screen.title) },
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedScreenIndex) {
                0 -> HomeScreen(
                    accounts = accounts,
                    transactions = transactions,
                    pendingTransactions = pendingTransactions,
                    categories = categories,
                    currencySymbol = currencySymbol,
                    lowBalanceThreshold = lowBalanceThreshold,
                    selectedAccountId = selectedAccountId,
                    onSelectAccount = { viewModel.setSelectedAccountId(it) },
                    onAddAccountClick = { showAddAccountDialog = true },
                    onAddRecurringBillClick = { showAddRecurringBillDialog = true },
                    onTransferClick = { initialAccId ->
                        transferInitialFromAccountId = initialAccId
                        showAccountTransferDialog = true
                    },
                    onPendingReviewClick = {
                        viewModel.setTypeFilter("PENDING")
                        selectedScreenIndex = 1
                    },
                    onTransactionClick = { editingTransaction = it },
                    onSeeAllClick = { selectedScreenIndex = 1 },
                    onScanInbox = { viewModel.scanInboxMessages(context) },
                    smsSyncFromDate = smsSyncFromDate,
                    onAddTransactionClick = { showAddTransactionDialog = true },
                    onMarkRecurringAsPaid = { viewModel.markRecurringAsPaid(it) }
                )

                1 -> TransactionsScreen(
                    transactions = transactions,
                    accounts = accounts,
                    categories = categories,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    currencySymbol = currencySymbol,
                    selectedAccountId = selectedAccountId,
                    onSelectAccountId = { viewModel.setSelectedAccountId(it) },
                    selectedCategory = selectedCategoryFilter,
                    onSelectCategory = { viewModel.setCategoryFilter(it) },
                    selectedTypeFilter = typeFilter,
                    onSelectTypeFilter = { viewModel.setTypeFilter(it) },
                    selectedDateRange = dateRangeFilter,
                    onSelectDateRange = { viewModel.setDateRangeFilter(it) },
                    customStartDate = customStartDate,
                    customEndDate = customEndDate,
                    onSetCustomDateRange = { start, end -> viewModel.setCustomDateRange(start, end) },
                    minAmountFilter = minAmountFilter,
                    maxAmountFilter = maxAmountFilter,
                    onSetMinAmountFilter = { viewModel.setMinAmountFilter(it) },
                    onSetMaxAmountFilter = { viewModel.setMaxAmountFilter(it) },
                    onClearAllFilters = { viewModel.clearAllFilters() },
                    onTransactionClick = { editingTransaction = it },
                    onAddClick = { showAddTransactionDialog = true },
                    onTransferClick = {
                        transferInitialFromAccountId = selectedAccountId
                        showAccountTransferDialog = true
                    }
                )

                2 -> ReportsScreen(
                    transactions = transactions,
                    categories = categories,
                    selectedMonthOffset = selectedMonthOffset,
                    currencySymbol = currencySymbol,
                    savingsGoalTarget = savingsGoalTarget,
                    onMonthOffsetChange = { viewModel.setMonthOffset(it) },
                    onEditCategoryBudget = { editingCategoryBudget = it },
                    onAddCustomCategoryClick = { showAddCategoryDialog = true },
                    onAddRecurringBillClick = { showAddRecurringBillDialog = true },
                    onToggleRecurring = { tx, isRec, freq, dueDate -> viewModel.toggleTransactionRecurring(tx, isRec, freq, dueDate) },
                    onTransactionClick = { editingTransaction = it },
                    onUpdateSavingsGoalTarget = { viewModel.setSavingsGoalTarget(it) },
                    onSetTotalMonthlyBudget = { viewModel.setTotalMonthlyBudget(it) }
                )

                3 -> SettingsScreen(
                    accounts = accounts,
                    importStatusMessage = importStatusMessage,
                    currencySymbol = currencySymbol,
                    themeMode = themeMode,
                    savingsGoalTarget = savingsGoalTarget,
                    lowBalanceThreshold = lowBalanceThreshold,
                    categories = categories,
                    onSelectCurrency = { viewModel.setCurrencySymbol(it, context) },
                    onSelectThemeMode = { viewModel.setThemeMode(it, context) },
                    onUpdateSavingsGoalTarget = { viewModel.setSavingsGoalTarget(it, context) },
                    onSetTotalMonthlyBudget = { viewModel.setTotalMonthlyBudget(it) },
                    onSetLowBalanceThreshold = { viewModel.setLowBalanceThreshold(it, context) },
                    onEditCategoryBudget = { editingCategoryBudget = it },
                    onAddCustomCategoryClick = { showAddCategoryDialog = true },
                    onOpenBackupDialog = { showBackupDialog = true },
                    onScanInbox = { viewModel.scanInboxMessages(context) },
                    smsSyncFromDate = smsSyncFromDate,
                    onSetSmsSyncFromDate = { date -> viewModel.setSmsSyncFromDate(context, date) },
                    onProcessSimulatedSms = { viewModel.processSimulatedSms(it) },
                    onAddAccountClick = { showAddAccountDialog = true },
                    onTransferClick = { acc ->
                        transferInitialFromAccountId = acc?.id
                        showAccountTransferDialog = true
                    },
                    onEditAccount = { editingAccount = it },
                    onDeleteAccount = { viewModel.deleteAccount(it) },
                    onClearCache = { viewModel.clearAppCache(context) }
                )
            }

            // Dialogs
            if (showAccountTransferDialog) {
                com.example.ui.dialogs.AccountTransferDialog(
                    accounts = accounts,
                    initialFromAccountId = transferInitialFromAccountId,
                    currencySymbol = currencySymbol,
                    onDismiss = { showAccountTransferDialog = false },
                    onTransfer = { fromAcc, toAcc, amount, notes, dateTimestamp ->
                        viewModel.transferMoney(fromAcc, toAcc, amount, notes, dateTimestamp)
                        showAccountTransferDialog = false
                    }
                )
            }
            editingTransaction?.let { tx ->
                EditTransactionDialog(
                    transaction = tx,
                    accounts = accounts,
                    categories = categories,
                    currencySymbol = currencySymbol,
                    onDismiss = { editingTransaction = null },
                    onSave = { title, category, accountId, notes, isRecurring, frequency, dueDate, receiptUri ->
                        viewModel.updateTransactionTitleAndTag(
                            tx,
                            title,
                            category,
                            accountId,
                            notes,
                            receiptUri,
                            isRecurring,
                            frequency,
                            dueDate
                        )
                        editingTransaction = null
                    },
                    onDelete = {
                        viewModel.deleteTransaction(tx)
                        editingTransaction = null
                    }
                )
            }

            if (showAddRecurringBillDialog) {
                com.example.ui.dialogs.AddOrEditRecurringBillDialog(
                    accounts = accounts,
                    categories = categories,
                    currencySymbol = currencySymbol,
                    onDismiss = { showAddRecurringBillDialog = false },
                    onSave = { title, amount, type, accountId, categoryTag, frequency, dueDate, notes, receiptUri ->
                        viewModel.addOrUpdateRecurringBill(
                            title = title,
                            amount = amount,
                            type = type,
                            accountId = accountId,
                            categoryTag = categoryTag,
                            frequency = frequency,
                            dueDate = dueDate,
                            notes = notes,
                            receiptUri = receiptUri
                        )
                        showAddRecurringBillDialog = false
                    }
                )
            }

            editingCategoryBudget?.let { cat ->
                EditCategoryBudgetDialog(
                    category = cat,
                    currencySymbol = currencySymbol,
                    onDismiss = { editingCategoryBudget = null },
                    onSaveLimit = { limit ->
                        viewModel.updateCategoryBudget(cat, limit)
                        editingCategoryBudget = null
                    },
                    onDeleteCategory = if (!cat.isPredefined) {
                        {
                            viewModel.deleteCategory(cat)
                            editingCategoryBudget = null
                        }
                    } else null
                )
            }

            if (showAddCategoryDialog) {
                AddCategoryDialog(
                    onDismiss = { showAddCategoryDialog = false },
                    onAdd = { name, iconName, colorHex, budgetLimit ->
                        viewModel.addCustomCategory(name, iconName, colorHex, budgetLimit)
                        showAddCategoryDialog = false
                    }
                )
            }

            if (showBackupDialog) {
                ExportImportBackupDialog(
                    onDismiss = { showBackupDialog = false },
                    onExportCsv = { viewModel.exportDataCsv() },
                    onExportJson = { viewModel.exportDataJson() },
                    onImportJson = { jsonStr -> viewModel.importJsonData(jsonStr) }
                )
            }

            if (showAddTransactionDialog) {
                AddTransactionDialog(
                    accounts = accounts,
                    categories = categories,
                    currencySymbol = currencySymbol,
                    onDismiss = { showAddTransactionDialog = false },
                    onAdd = { title, amount, type, accountId, categoryTag, notes, dateTimestamp, receiptUri ->
                        viewModel.addManualTransaction(title, amount, type, accountId, categoryTag, notes, dateTimestamp, receiptUri)
                        showAddTransactionDialog = false
                    }
                )
            }

            val currentAccountToEdit = editingAccount
            if (showAddAccountDialog || currentAccountToEdit != null) {
                AddOrEditAccountDialog(
                    accountToEdit = currentAccountToEdit,
                    onDismiss = {
                        showAddAccountDialog = false
                        editingAccount = null
                    },
                    onSave = { name, type, balance, last4, colorHex, senderKeywords, customTypeLabel ->
                        if (currentAccountToEdit != null) {
                            viewModel.updateAccount(
                                account = currentAccountToEdit,
                                name = name,
                                type = type,
                                balance = balance,
                                last4 = last4,
                                colorHex = colorHex,
                                senderKeywords = senderKeywords,
                                customTypeLabel = customTypeLabel
                            )
                        } else {
                            viewModel.addAccount(
                                name = name,
                                type = type,
                                balance = balance,
                                last4 = last4,
                                colorHex = colorHex,
                                senderKeywords = senderKeywords,
                                customTypeLabel = customTypeLabel
                            )
                        }
                        showAddAccountDialog = false
                        editingAccount = null
                    },
                    onDelete = if (currentAccountToEdit != null) {
                        {
                            viewModel.deleteAccount(currentAccountToEdit)
                            showAddAccountDialog = false
                            editingAccount = null
                        }
                    } else null
                )
            }
        }
    }
}
