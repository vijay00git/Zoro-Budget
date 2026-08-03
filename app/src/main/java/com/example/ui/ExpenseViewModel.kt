package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.export.DataExporter
import com.example.data.local.BudgetRepository
import com.example.data.local.entity.AccountEntity
import com.example.data.preferences.AppPreferences
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.sms.SmsInboxScanner
import com.example.data.sms.SmsParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Lets ExpenseViewModel take a constructor-injected BudgetRepository while still being created
 * through androidx.activity's `by viewModels()` delegate, so the ViewModelStore retains the
 * instance (and all its in-memory filter/UI state) across configuration changes instead of
 * MainActivity.onCreate() rebuilding it from scratch every time.
 */
class ExpenseViewModelFactory(private val repository: BudgetRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class ExpenseViewModel(private val repository: BudgetRepository) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.initializeDefaultDataIfNeeded()
        }
    }

    val accounts: StateFlow<List<AccountEntity>> = repository.accounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingTransactions: StateFlow<List<TransactionEntity>> = repository.pendingTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filters
    private val _selectedAccountId = MutableStateFlow<Long?>(null) // null = All
    val selectedAccountId = _selectedAccountId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter = _selectedCategoryFilter.asStateFlow()

    private val _typeFilter = MutableStateFlow("ALL") // ALL, DEBIT, CREDIT, PENDING, RECURRING
    val typeFilter = _typeFilter.asStateFlow()

    private val _selectedMonthOffset = MutableStateFlow(0) // 0 = Current month, -1 = previous month
    val selectedMonthOffset = _selectedMonthOffset.asStateFlow()

    // Multi-Filter & Custom Date Range Search
    private val _themeMode = MutableStateFlow("SYSTEM") // SYSTEM, LIGHT, DARK
    val themeMode = _themeMode.asStateFlow()

    private val _currencySymbol = MutableStateFlow("$")
    val currencySymbol = _currencySymbol.asStateFlow()

    private val _savingsGoalTarget = MutableStateFlow(500.0)
    val savingsGoalTarget = _savingsGoalTarget.asStateFlow()

    private val _lowBalanceThreshold = MutableStateFlow(200.0)
    val lowBalanceThreshold = _lowBalanceThreshold.asStateFlow()

    private val _dateRangeFilter = MutableStateFlow("ALL") // ALL, THIS_WEEK, THIS_MONTH, LAST_30_DAYS, CUSTOM
    val dateRangeFilter = _dateRangeFilter.asStateFlow()

    private val _customStartDate = MutableStateFlow<Long?>(null)
    val customStartDate = _customStartDate.asStateFlow()

    private val _customEndDate = MutableStateFlow<Long?>(null)
    val customEndDate = _customEndDate.asStateFlow()

    private val _minAmountFilter = MutableStateFlow<Double?>(null)
    val minAmountFilter = _minAmountFilter.asStateFlow()

    private val _maxAmountFilter = MutableStateFlow<Double?>(null)
    val maxAmountFilter = _maxAmountFilter.asStateFlow()

    private val _importStatusMessage = MutableStateFlow<String?>(null)
    val importStatusMessage = _importStatusMessage.asStateFlow()

    private val _smsSyncFromDate = MutableStateFlow<Long?>(null)
    val smsSyncFromDate = _smsSyncFromDate.asStateFlow()

    // Filtered Transactions Flow
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        repository.transactions,
        _selectedAccountId,
        _searchQuery,
        _selectedCategoryFilter,
        _typeFilter,
        _dateRangeFilter,
        _customStartDate,
        _customEndDate,
        _minAmountFilter,
        _maxAmountFilter
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val allTx = args[0] as List<TransactionEntity>
        val accountId = args[1] as Long?
        val query = args[2] as String
        val category = args[3] as String?
        val type = args[4] as String
        val dateRange = args[5] as String
        val customStart = args[6] as Long?
        val customEnd = args[7] as Long?
        val minAmount = args[8] as Double?
        val maxAmount = args[9] as Double?

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        allTx.filter { tx ->
            val matchAccount = accountId == null || tx.accountId == accountId
            val matchQuery = query.isBlank() ||
                    tx.title.contains(query, ignoreCase = true) ||
                    (tx.merchantOrSender?.contains(query, ignoreCase = true) == true) ||
                    tx.categoryTag.contains(query, ignoreCase = true) ||
                    tx.amount.toString().contains(query)
            val matchCategory = category == null || tx.categoryTag.equals(category, ignoreCase = true)
            val matchType = when (type) {
                "DEBIT" -> tx.type == "DEBIT" && tx.categoryTag != "Transfer"
                "CREDIT" -> tx.type == "CREDIT" && tx.categoryTag != "Transfer"
                "TRANSFER" -> tx.categoryTag == "Transfer" || tx.title.startsWith("Transfer", ignoreCase = true)
                "PENDING" -> tx.isPendingReview
                "RECURRING" -> tx.isRecurring
                else -> true
            }

            val matchDateRange = when (dateRange) {
                "THIS_WEEK" -> {
                    cal.timeInMillis = now
                    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    tx.dateTimestamp >= cal.timeInMillis
                }
                "THIS_MONTH" -> {
                    cal.timeInMillis = now
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    tx.dateTimestamp >= cal.timeInMillis
                }
                "LAST_30_DAYS" -> tx.dateTimestamp >= (now - (30L * 86400000L))
                "CUSTOM" -> {
                    val startOk = customStart == null || tx.dateTimestamp >= customStart
                    val endOk = customEnd == null || tx.dateTimestamp <= (customEnd + 86399999L) // end of selected day
                    startOk && endOk
                }
                else -> true
            }

            val matchAmount = (minAmount == null || tx.amount >= minAmount) &&
                    (maxAmount == null || tx.amount <= maxAmount)

            matchAccount && matchQuery && matchCategory && matchType && matchDateRange && matchAmount
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedAccountId(id: Long?) {
        _selectedAccountId.value = id
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun setTypeFilter(type: String) {
        _typeFilter.value = type
    }

    fun setCurrencySymbol(symbol: String, context: Context? = null) {
        _currencySymbol.value = symbol
        _importStatusMessage.value = "Updated default currency symbol to $symbol"
        context?.let { AppPreferences.save(context = it, currencySymbol = symbol) }
    }

    fun setThemeMode(mode: String, context: Context? = null) {
        _themeMode.value = mode
        val label = when (mode) {
            "LIGHT" -> "Light mode"
            "DARK" -> "Dark mode"
            else -> "System default theme"
        }
        _importStatusMessage.value = "App theme set to $label"
        context?.let { AppPreferences.save(context = it, themeMode = mode) }
    }

    fun setSavingsGoalTarget(target: Double, context: Context? = null) {
        _savingsGoalTarget.value = target
        _importStatusMessage.value = "Updated monthly savings target to ${_currencySymbol.value}${String.format(java.util.Locale.getDefault(), "%.2f", target)}"
        context?.let { AppPreferences.save(context = it, savingsGoalTarget = target) }
    }

    fun setLowBalanceThreshold(threshold: Double, context: Context? = null) {
        _lowBalanceThreshold.value = threshold
        _importStatusMessage.value = "Updated low balance alert limit to ${_currencySymbol.value}${String.format(java.util.Locale.getDefault(), "%.2f", threshold)}"
        context?.let { AppPreferences.save(context = it, lowBalanceThreshold = threshold) }
    }

    fun setMonthOffset(offset: Int) {
        _selectedMonthOffset.value = offset
    }

    fun setDateRangeFilter(range: String) {
        _dateRangeFilter.value = range
        if (range != "CUSTOM") {
            _customStartDate.value = null
            _customEndDate.value = null
        }
    }

    fun setCustomDateRange(startDate: Long?, endDate: Long?) {
        _customStartDate.value = startDate
        _customEndDate.value = endDate
        _dateRangeFilter.value = if (startDate != null || endDate != null) "CUSTOM" else "ALL"
    }

    fun setMinAmountFilter(min: Double?) {
        _minAmountFilter.value = min
    }

    fun setMaxAmountFilter(max: Double?) {
        _maxAmountFilter.value = max
    }

    fun clearAllFilters() {
        _selectedAccountId.value = null
        _searchQuery.value = ""
        _selectedCategoryFilter.value = null
        _typeFilter.value = "ALL"
        _dateRangeFilter.value = "ALL"
        _customStartDate.value = null
        _customEndDate.value = null
        _minAmountFilter.value = null
        _maxAmountFilter.value = null
    }

    fun clearStatusMessage() {
        _importStatusMessage.value = null
    }

    fun updateCategoryBudget(category: CategoryEntity, limit: Double) {
        viewModelScope.launch {
            val updated = category.copy(monthlyBudgetLimit = limit)
            repository.updateCategory(updated)
            _importStatusMessage.value = "Updated budget limit for ${category.name} to ${_currencySymbol.value}${String.format(java.util.Locale.getDefault(), "%.2f", limit)}"
        }
    }

    fun setTotalMonthlyBudget(total: Double) {
        viewModelScope.launch {
            // Income/savings categories (e.g. "Salary & Income") don't get a slice of an
            // expense budget - only categories people actually spend against do.
            val expenseCats = repository.categories.first().filter { !it.isIncomeCategory }
            if (expenseCats.isNotEmpty()) {
                val perCatLimit = (total / expenseCats.size)
                expenseCats.forEach { cat ->
                    repository.updateCategory(cat.copy(monthlyBudgetLimit = perCatLimit))
                }
                _importStatusMessage.value = "Set overall monthly budget to ${_currencySymbol.value}${String.format(java.util.Locale.getDefault(), "%.2f", total)} across ${expenseCats.size} categories"
            }
        }
    }

    fun addCustomCategory(name: String, iconName: String, colorHex: String, budgetLimit: Double) {
        viewModelScope.launch {
            val cat = CategoryEntity(
                name = name,
                iconName = iconName,
                colorHex = colorHex,
                isPredefined = false,
                monthlyBudgetLimit = budgetLimit
            )
            repository.addCategory(cat)
            _importStatusMessage.value = "Created category: $name"
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            _importStatusMessage.value = "Deleted category: ${category.name}"
        }
    }

    fun toggleTransactionRecurring(
        transaction: TransactionEntity,
        isRecurring: Boolean,
        frequency: String? = "MONTHLY",
        dueDate: Int? = null
    ) {
        viewModelScope.launch {
            val updated = transaction.copy(
                isRecurring = isRecurring,
                recurringFrequency = if (isRecurring) (frequency ?: "MONTHLY") else null,
                recurringDueDate = if (isRecurring) dueDate else null
            )
            repository.updateTransaction(transaction, updated)
            _importStatusMessage.value = if (isRecurring) "Marked as recurring bill (${updated.recurringFrequency ?: "MONTHLY"})" else "Removed from recurring bills"
        }
    }

    fun markRecurringAsPaid(transaction: TransactionEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val paidTx = TransactionEntity(
                title = "Paid: ${transaction.title}",
                amount = transaction.amount,
                type = transaction.type,
                accountId = transaction.accountId,
                categoryTag = transaction.categoryTag,
                dateTimestamp = now,
                source = "RECURRING_PAID",
                isPendingReview = false,
                notes = "Recurring ${transaction.recurringFrequency ?: "MONTHLY"} payment recorded on ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}"
            )
            repository.runInTransaction {
                repository.addTransaction(paidTx)
                // Record the payment on the recurring bill itself too, so its due/overdue alert
                // is suppressed for this cycle instead of still nagging right after being paid.
                // type/amount are unchanged so this doesn't touch the account balance again.
                repository.updateTransaction(transaction, transaction.copy(lastPaidTimestamp = now))
            }
            _importStatusMessage.value = "Recorded payment for '${transaction.title}' (${_currencySymbol.value}${String.format(java.util.Locale.getDefault(), "%.2f", transaction.amount)})"
        }
    }

    /**
     * Suppresses this cycle's due/overdue alert WITHOUT recording a payment or touching the
     * account balance - for a bill you're deliberately not paying this cycle (e.g. a
     * subscription paused this month) but still want tracked as recurring going forward.
     */
    fun skipRecurringCycle(transaction: TransactionEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.updateTransaction(transaction, transaction.copy(lastPaidTimestamp = now))
            _importStatusMessage.value = "Skipped this cycle for '${transaction.title}'"
        }
    }

    fun addOrUpdateRecurringBill(
        existingBill: TransactionEntity? = null,
        title: String,
        amount: Double,
        type: String = "DEBIT",
        accountId: Long,
        categoryTag: String = "Bills & Utilities",
        frequency: String = "MONTHLY",
        dueDate: Int = 4,
        notes: String? = null,
        receiptUri: String? = null
    ) {
        viewModelScope.launch {
            if (existingBill != null) {
                val updated = existingBill.copy(
                    title = title.ifBlank { "Recurring Bill" },
                    amount = amount,
                    type = type,
                    accountId = accountId,
                    categoryTag = categoryTag,
                    isRecurring = true,
                    recurringFrequency = frequency,
                    recurringDueDate = dueDate,
                    notes = notes,
                    receiptUri = receiptUri ?: existingBill.receiptUri
                )
                repository.updateTransaction(existingBill, updated)
                _importStatusMessage.value = "Updated recurring bill: $title"
            } else {
                val newBill = TransactionEntity(
                    title = title.ifBlank { "Recurring Bill" },
                    amount = amount,
                    type = type,
                    accountId = accountId,
                    categoryTag = categoryTag,
                    source = "MANUAL",
                    isPendingReview = false,
                    isRecurring = true,
                    recurringFrequency = frequency,
                    recurringDueDate = dueDate,
                    notes = notes,
                    receiptUri = receiptUri
                )
                repository.addTransaction(newBill)
                _importStatusMessage.value = "Added recurring bill: $title"
            }
        }
    }

    suspend fun exportDataCsv(): String {
        val txs = repository.transactions.first()
        val accs = repository.accounts.first()
        return DataExporter.exportToCsv(txs, accs)
    }

    suspend fun exportDataJson(): String {
        val txs = repository.transactions.first()
        val accs = repository.accounts.first()
        val cats = repository.categories.first()
        return DataExporter.exportToJson(txs, accs, cats)
    }

    fun importJsonData(jsonString: String) {
        viewModelScope.launch {
            val parsed = DataExporter.parseJsonImport(jsonString)
            if (parsed == null) {
                _importStatusMessage.value = "Failed to parse JSON backup. Check format."
                return@launch
            }

            // The whole restore runs as one DB transaction: a crash or error partway through
            // (e.g. after accounts are restored but before transactions are) would otherwise
            // leave the database in a half-imported state.
            val count = repository.runInTransaction {
                // Restore accounts as brand-new rows and remember old id -> new id so imported
                // transactions can be re-pointed at the right account, without colliding with (and
                // silently overwriting) any account that already exists on this device.
                val accountIdRemap = mutableMapOf<Long, Long>()
                for (acc in parsed.accounts) {
                    accountIdRemap[acc.id] = repository.restoreAccount(acc)
                }
                val fallbackAccountId = accountIdRemap.values.firstOrNull()
                    ?: repository.accounts.first().firstOrNull()?.id

                // Categories are matched by name, not id - skip any category that already exists
                // locally so re-importing a backup doesn't create duplicate categories.
                val existingCategoryNames = repository.categories.first().map { it.name.lowercase() }.toSet()
                for (cat in parsed.categories) {
                    if (cat.name.lowercase() !in existingCategoryNames) {
                        repository.addCategory(cat.copy(id = 0))
                    }
                }

                var imported = 0
                for (tx in parsed.transactions) {
                    val remappedAccountId = accountIdRemap[tx.accountId] ?: fallbackAccountId ?: continue
                    // restoreTransaction (not addTransaction) - the imported account balance already
                    // reflects this transaction, so we must not re-apply its delta on top of it.
                    repository.restoreTransaction(tx.copy(accountId = remappedAccountId))
                    imported++
                }
                imported
            }
            _importStatusMessage.value = "Restored backup! Imported $count transactions."
        }
    }

    fun importCsvData(csvString: String) {
        viewModelScope.launch {
            val rows = DataExporter.parseCsvImport(csvString)
            if (rows == null) {
                _importStatusMessage.value = "Failed to parse CSV. Check the header/column format."
                return@launch
            }
            if (rows.isEmpty()) {
                _importStatusMessage.value = "No transaction rows found in CSV."
                return@launch
            }
            val count = repository.runInTransaction {
                repository.restoreCsvTransactions(rows)
            }
            _importStatusMessage.value = "Imported $count transactions from CSV."
        }
    }

    fun loadPreferences(context: Context) {
        val prefs = AppPreferences.load(context)
        _currencySymbol.value = prefs.currencySymbol
        _savingsGoalTarget.value = prefs.savingsGoalTarget
        _lowBalanceThreshold.value = prefs.lowBalanceThreshold
        _themeMode.value = prefs.themeMode

        val ts = context.getSharedPreferences("smart_budget_prefs", Context.MODE_PRIVATE)
            .getLong("sms_sync_from_date", 0L)
        _smsSyncFromDate.value = if (ts > 0L) ts else null
    }

    fun setSmsSyncFromDate(context: Context, timestamp: Long?) {
        _smsSyncFromDate.value = timestamp
        val prefs = context.getSharedPreferences("smart_budget_prefs", Context.MODE_PRIVATE)
        if (timestamp != null && timestamp > 0L) {
            prefs.edit().putLong("sms_sync_from_date", timestamp).apply()
            val formatted = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
            _importStatusMessage.value = "Automated Bank SMS Sync starting date set to $formatted"
        } else {
            prefs.edit().remove("sms_sync_from_date").apply()
            _importStatusMessage.value = "Cleared SMS sync date filter. Syncing all bank SMS messages."
        }
    }

    fun scanInboxMessages(context: Context) {
        viewModelScope.launch {
            _importStatusMessage.value = "Scanning SMS messages..."
            val prefs = context.getSharedPreferences("smart_budget_prefs", Context.MODE_PRIVATE)
            val minTimestamp = prefs.getLong("sms_sync_from_date", 0L)
            val scanner = SmsInboxScanner(context, repository)
            val count = scanner.scanInboxAndImport(150, minTimestamp)
            _importStatusMessage.value = if (count > 0) {
                "Successfully imported $count new transactions from SMS!"
            } else {
                "No new financial SMS messages found."
            }
            if (count > 0) {
                com.example.data.notifications.BillAlertScheduler.enqueueImmediateCheck(context)
            }
        }
    }

    fun processSimulatedSms(messageText: String) {
        viewModelScope.launch {
            if (messageText.isBlank()) {
                _importStatusMessage.value = "Please enter SMS message content."
                return@launch
            }
            val parsed = SmsParser.parseMessage(messageText)
            if (parsed != null) {
                val success = repository.processParsedSmsTransaction(parsed)
                if (success) {
                    _importStatusMessage.value = "Parsed: ${parsed.type} of ${_currencySymbol.value}${String.format(java.util.Locale.getDefault(), "%.2f", parsed.amount)} at ${parsed.merchantOrSender ?: "Merchant"}"
                } else {
                    _importStatusMessage.value = "Transaction already imported."
                }
            } else {
                _importStatusMessage.value = "Could not parse monetary debit/credit from SMS. Please check format."
            }
        }
    }

    fun updateTransactionTitleAndTag(
        transaction: TransactionEntity,
        newTitle: String,
        newCategory: String,
        newAccountId: Long,
        newNotes: String?,
        newReceiptUri: String? = transaction.receiptUri,
        isRecurring: Boolean = transaction.isRecurring,
        recurringFrequency: String? = transaction.recurringFrequency,
        recurringDueDate: Int? = transaction.recurringDueDate
    ) {
        viewModelScope.launch {
            val updated = transaction.copy(
                title = newTitle.ifBlank { "Untitled Transaction" },
                categoryTag = newCategory,
                accountId = newAccountId,
                isPendingReview = false,
                notes = newNotes,
                receiptUri = newReceiptUri,
                isRecurring = isRecurring,
                recurringFrequency = if (isRecurring) (recurringFrequency ?: "MONTHLY") else null,
                recurringDueDate = if (isRecurring) recurringDueDate else null
            )
            repository.updateTransaction(transaction, updated)
            _importStatusMessage.value = if (isRecurring) {
                "Updated transaction and recurring settings."
            } else {
                "Updated transaction details & receipt."
            }
        }
    }

    fun addManualTransaction(
        title: String,
        amount: Double,
        type: String,
        accountId: Long,
        categoryTag: String,
        notes: String?,
        dateTimestamp: Long = System.currentTimeMillis(),
        receiptUri: String? = null
    ) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                title = title.ifBlank { "Manual Transaction" },
                amount = amount,
                type = type,
                accountId = accountId,
                categoryTag = categoryTag,
                dateTimestamp = dateTimestamp,
                source = "MANUAL",
                isPendingReview = false,
                notes = notes,
                receiptUri = receiptUri
            )
            repository.addTransaction(tx)
            _importStatusMessage.value = "Added $type transaction of ${_currencySymbol.value}${String.format(java.util.Locale.getDefault(), "%.2f", amount)}"
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _importStatusMessage.value = "Transaction deleted."
        }
    }

    fun addAccount(
        name: String,
        type: String,
        balance: Double,
        last4: String?,
        colorHex: String,
        senderKeywords: String? = null,
        customTypeLabel: String? = null
    ) {
        viewModelScope.launch {
            val acc = AccountEntity(
                name = name,
                type = type,
                balance = balance,
                accountNumberLast4 = last4?.takeIf { it.isNotBlank() },
                colorHex = colorHex,
                isDefault = false,
                senderKeywords = senderKeywords?.takeIf { it.isNotBlank() },
                customTypeLabel = customTypeLabel?.takeIf { it.isNotBlank() }
            )
            repository.addAccount(acc)
            _importStatusMessage.value = "Added account: $name"
        }
    }

    fun updateAccount(
        account: AccountEntity,
        name: String,
        type: String,
        balance: Double,
        last4: String?,
        colorHex: String,
        senderKeywords: String? = null,
        customTypeLabel: String? = null
    ) {
        viewModelScope.launch {
            val updated = account.copy(
                name = name,
                type = type,
                balance = balance,
                accountNumberLast4 = last4?.takeIf { it.isNotBlank() },
                colorHex = colorHex,
                senderKeywords = senderKeywords?.takeIf { it.isNotBlank() },
                customTypeLabel = customTypeLabel?.takeIf { it.isNotBlank() }
            )
            repository.updateAccount(updated)
            _importStatusMessage.value = "Updated account: $name"
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
            _importStatusMessage.value = "Deleted account: ${account.name}"
        }
    }

    fun transferMoney(
        fromAccount: AccountEntity,
        toAccount: AccountEntity,
        amount: Double,
        notes: String?,
        dateTimestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            if (fromAccount.id == toAccount.id) {
                _importStatusMessage.value = "Source and destination accounts must be different."
                return@launch
            }
            if (amount <= 0) {
                _importStatusMessage.value = "Transfer amount must be greater than zero."
                return@launch
            }
            repository.transferBetweenAccounts(
                fromAccountId = fromAccount.id,
                toAccountId = toAccount.id,
                fromAccountName = fromAccount.name,
                toAccountName = toAccount.name,
                amount = amount,
                notes = notes,
                dateTimestamp = dateTimestamp
            )
            val formattedAmount = String.format(java.util.Locale.getDefault(), "%.2f", amount)
            _importStatusMessage.value = "Transferred ${_currencySymbol.value}$formattedAmount from ${fromAccount.name} to ${toAccount.name}"
        }
    }

    fun clearAppCache(context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Clear Coil image loader cache
                coil.Coil.imageLoader(context).memoryCache?.clear()
                coil.Coil.imageLoader(context).diskCache?.clear()

                // Clear internal app cache directory
                context.cacheDir.deleteRecursively()

                // Trigger runtime garbage collection hint
                System.gc()

                _importStatusMessage.value = "App cache & temp files purged! Memory & Phone Storage optimized."
            } catch (e: Exception) {
                _importStatusMessage.value = "Failed to clear cache: ${e.message ?: "unknown error"}"
            }
        }
    }
}
