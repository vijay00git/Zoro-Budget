package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppDatabase
import com.example.data.local.BudgetRepository
import com.example.data.savings.SavingsRepository
import com.example.data.notifications.BillAlertScheduler
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringBillEntity
import com.example.data.local.entity.TransactionEntity
import com.example.ui.ExpenseViewModel
import com.example.ui.ExpenseViewModelFactory
import com.example.ui.dialogs.AddOrEditAccountDialog
import com.example.ui.dialogs.AddCategoryDialog
import com.example.ui.dialogs.AddTransactionDialog
import com.example.ui.dialogs.EditCategoryBudgetDialog
import com.example.ui.dialogs.EditTransactionDialog
import com.example.ui.dialogs.ExportImportBackupDialog
import com.example.ui.dialogs.QuickReviewPendingDialog
import com.example.ui.components.CompactNavBar
import com.example.ui.components.NavTab
import com.example.ui.savings.SavingsScreen
import com.example.ui.savings.SavingsViewModel
import com.example.ui.savings.SavingsViewModelFactory
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.SmartBudgetTheme

sealed class Screen(val route: String, @StringRes val titleRes: Int, val icon: ImageVector) {
    object Home : Screen("home", R.string.nav_overview, Icons.Default.Home)
    object Transactions : Screen("transactions", R.string.nav_ledger, Icons.Default.ReceiptLong)
    object Reports : Screen("reports", R.string.nav_planning, Icons.Default.BarChart)
    object Savings : Screen("savings", R.string.nav_savings, Icons.Default.Savings)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    // Created via a factory (not `ExpenseViewModel(repository)` directly) so the ViewModelStore
    // retains this instance - and every filter/selection it holds - across configuration changes
    // (rotation, dark-mode toggle, font-scale change) instead of MainActivity.onCreate()
    // rebuilding a fresh one from scratch each time.
    private val viewModel: ExpenseViewModel by viewModels {
        val db = AppDatabase.getDatabase(applicationContext)
        val repository = BudgetRepository(db, db.accountDao(), db.transactionDao(), db.categoryDao())
        ExpenseViewModelFactory(repository)
    }

    private val savingsViewModel: SavingsViewModel by viewModels {
        SavingsViewModelFactory(
            SavingsRepository(AppDatabase.getDatabase(applicationContext)),
            applicationContext.getSharedPreferences(SavingsViewModel.PREFS_NAME, MODE_PRIVATE)
        )
    }

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The debug log capture was removed; delete the log file older versions left behind.
        java.io.File(filesDir, "debug_logs").deleteRecursively()

        BillAlertScheduler.schedulePeriodic(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            SmartBudgetTheme(themeMode = themeMode) {
                SmartBudgetApp(viewModel = viewModel, savingsViewModel = savingsViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartBudgetApp(viewModel: ExpenseViewModel, savingsViewModel: SavingsViewModel) {
    // Saved so rotation or a theme change keeps the tab; Back from any other tab returns to Overview.
    var selectedScreenIndex by rememberSaveable { mutableIntStateOf(0) }
    val screens = listOf(
        Screen.Home,
        Screen.Transactions,
        Screen.Reports,
        Screen.Savings,
        Screen.Settings
    )

    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val billUndo by viewModel.billUndo.collectAsStateWithLifecycle()
    val overviewMonthOffset by viewModel.overviewMonthOffset.collectAsStateWithLifecycle()
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
    val balancesHidden by viewModel.balancesHidden.collectAsStateWithLifecycle()
    val accountsExpanded by viewModel.accountsExpanded.collectAsStateWithLifecycle()

    // Dialog state holders
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var editingCategoryBudget by remember { mutableStateOf<CategoryEntity?>(null) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showAddRecurringBillDialog by remember { mutableStateOf(false) }
    var editingBill by remember { mutableStateOf<RecurringBillEntity?>(null) }
    var billFromTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var payingBill by remember { mutableStateOf<RecurringBillEntity?>(null) }
    var viewingBillId by remember { mutableStateOf<Long?>(null) }
    var showAccountTransferDialog by remember { mutableStateOf(false) }
    var showQuickReviewDialog by remember { mutableStateOf(false) }
    var transferInitialFromAccountId by remember { mutableStateOf<Long?>(null) }
    var transferInitialToAccountId by remember { mutableStateOf<Long?>(null) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadPreferences(context)
    }

    val undoLabel = stringResource(R.string.bills_undo)
    LaunchedEffect(billUndo) {
        // Cleared only after the snackbar closes: clearing first would restart this effect and
        // cancel the snackbar before it is shown.
        val undo = billUndo ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(undo.message, actionLabel = undoLabel, duration = SnackbarDuration.Long)
        viewModel.clearBillUndo()
        if (result == SnackbarResult.ActionPerformed) viewModel.undoBillAction(undo)
    }

    LaunchedEffect(importStatusMessage) {
        importStatusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    BackHandler(enabled = selectedScreenIndex != 0) { selectedScreenIndex = 0 }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            CompactNavBar(
                tabs = screens.map { screen ->
                    NavTab(
                        key = screen.route,
                        label = stringResource(screen.titleRes),
                        icon = screen.icon,
                        badge = if (screen == Screen.Transactions) pendingTransactions.size else 0
                    )
                },
                selectedIndex = selectedScreenIndex,
                onSelect = { selectedScreenIndex = it }
            )
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
                    transactions = allTransactions,
                    monthOffset = overviewMonthOffset,
                    onMonthOffsetChange = { viewModel.setOverviewMonthOffset(it) },
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
                        transferInitialToAccountId = null
                        showAccountTransferDialog = true
                    },
                    onAddMoneyClick = { account ->
                        transferInitialFromAccountId = null
                        transferInitialToAccountId = account.id
                        showAccountTransferDialog = true
                    },
                    onChangeLowBalanceAlert = { selectedScreenIndex = screens.indexOf(Screen.Settings) },
                    onPendingReviewClick = {
                        showQuickReviewDialog = true
                    },
                    onCategoryClick = { category ->
                        viewModel.showCategoryInLedger(category, overviewMonthOffset)
                        selectedScreenIndex = 1
                    },
                    onTransactionClick = { editingTransaction = it },
                    onAddTransactionClick = { showAddTransactionDialog = true },
                    bills = bills,
                    onBillClick = { viewingBillId = it.id },
                    onMarkBillPaid = { payingBill = it },
                    onSeeAllBills = { selectedScreenIndex = 2 },
                    balancesHidden = balancesHidden,
                    onToggleBalances = { viewModel.toggleBalancesHidden() },
                    accountsExpanded = accountsExpanded,
                    onToggleAccounts = { viewModel.toggleAccountsExpanded() }
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
                    transactions = allTransactions,
                    categories = categories,
                    selectedMonthOffset = selectedMonthOffset,
                    currencySymbol = currencySymbol,
                    savingsGoalTarget = savingsGoalTarget,
                    onMonthOffsetChange = { viewModel.setMonthOffset(it) },
                    onEditCategoryBudget = { editingCategoryBudget = it },
                    onAddCustomCategoryClick = { showAddCategoryDialog = true },
                    onAddRecurringBillClick = { showAddRecurringBillDialog = true },
                    bills = bills,
                    accounts = accounts,
                    onBillClick = { viewingBillId = it.id },
                    onMarkBillPaid = { payingBill = it },
                    onTransactionClick = { editingTransaction = it },
                    onUpdateSavingsGoalTarget = { viewModel.setSavingsGoalTarget(it) },
                    onSetTotalMonthlyBudget = { viewModel.setTotalMonthlyBudget(it) }
                )

                3 -> SavingsScreen(
                    viewModel = savingsViewModel,
                    accountBalances = accounts.map { it.balance },
                    currencySymbol = currencySymbol,
                    snackbarHostState = snackbarHostState
                )

                4 -> SettingsScreen(
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
                    initialToAccountId = transferInitialToAccountId,
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
                    onSave = { title, category, accountId, notes, receiptUri ->
                        viewModel.updateTransactionTitleAndTag(tx, title, category, accountId, notes, receiptUri)
                        editingTransaction = null
                    },
                    onMakeRecurring = {
                        billFromTransaction = tx
                        editingTransaction = null
                    },
                    onDelete = {
                        viewModel.deleteTransaction(tx)
                        editingTransaction = null
                    }
                )
            }

            if (showAddRecurringBillDialog || editingBill != null || billFromTransaction != null) {
                val closeBillForm = {
                    showAddRecurringBillDialog = false
                    editingBill = null
                    billFromTransaction = null
                }
                com.example.ui.dialogs.AddOrEditRecurringBillDialog(
                    billToEdit = editingBill,
                    fromTransaction = billFromTransaction,
                    accounts = accounts,
                    categories = categories,
                    currencySymbol = currencySymbol,
                    onDismiss = closeBillForm,
                    onSave = { bill ->
                        viewModel.saveBill(bill, firstPayment = billFromTransaction)
                        closeBillForm()
                    },
                    onDelete = editingBill?.let { bill ->
                        {
                            viewModel.deleteBill(bill)
                            closeBillForm()
                        }
                    }
                )
            }

            payingBill?.let { bill ->
                com.example.ui.dialogs.MarkBillPaidDialog(
                    bill = bill,
                    accounts = accounts,
                    currencySymbol = currencySymbol,
                    loadCandidates = { viewModel.billPaymentCandidates(it) },
                    onDismiss = { payingBill = null },
                    onLinkExisting = { payment ->
                        viewModel.linkBillPayment(bill, payment)
                        payingBill = null
                        viewingBillId = null
                    },
                    onRecordNew = { amount, accountId, paidAt ->
                        viewModel.recordBillPayment(bill, amount, accountId, paidAt)
                        payingBill = null
                        viewingBillId = null
                    }
                )
            }

            bills.firstOrNull { it.id == viewingBillId }?.let { bill ->
                if (payingBill == null && editingBill == null) {
                    com.example.ui.dialogs.BillDetailsDialog(
                        bill = bill,
                        accounts = accounts,
                        currencySymbol = currencySymbol,
                        payments = remember(bill.id) { viewModel.paymentsForBill(bill.id) },
                        onDismiss = { viewingBillId = null },
                        onMarkPaid = { payingBill = bill },
                        onSkip = {
                            viewModel.skipBillCycle(bill)
                            viewingBillId = null
                        },
                        onSetActive = { viewModel.setBillActive(bill, it) },
                        onEdit = {
                            editingBill = bill
                            viewingBillId = null
                        },
                        onPaymentClick = {
                            editingTransaction = it
                            viewingBillId = null
                        }
                    )
                }
            }

            editingCategoryBudget?.let { cat ->
                EditCategoryBudgetDialog(
                    category = cat,
                    currencySymbol = currencySymbol,
                    onDismiss = { editingCategoryBudget = null },
                    onSave = { limit, iconName, colorHex ->
                        viewModel.updateCategory(cat, limit, iconName, colorHex)
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
                    },
                    currencySymbol = currencySymbol
                )
            }

            if (showBackupDialog) {
                ExportImportBackupDialog(
                    onDismiss = { showBackupDialog = false },
                    onExportCsv = { viewModel.exportDataCsv() },
                    onExportJson = { viewModel.exportDataJson() },
                    onImportJson = { jsonStr -> viewModel.importJsonData(jsonStr) },
                    onImportCsv = { csvStr -> viewModel.importCsvData(csvStr) }
                )
            }

            if (showQuickReviewDialog && pendingTransactions.isNotEmpty()) {
                QuickReviewPendingDialog(
                    pendingTransactions = pendingTransactions,
                    accounts = accounts,
                    categories = categories,
                    currencySymbol = currencySymbol,
                    onDismiss = { showQuickReviewDialog = false },
                    onConfirm = { tx, title, categoryTag, accountId ->
                        viewModel.updateTransactionTitleAndTag(tx, title, categoryTag, accountId, tx.notes)
                    },
                    onDecline = { tx -> viewModel.deleteTransaction(tx) }
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
                    currencySymbol = currencySymbol,
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
