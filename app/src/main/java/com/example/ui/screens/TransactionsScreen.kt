package com.example.ui.screens

import com.example.ui.theme.accentOf
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.components.formatMoney
import com.example.ui.components.CategoryAvatar
import com.example.ui.components.BentoTile
import com.example.data.utils.CategoryTotals
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material.icons.filled.Sms
import android.app.DatePickerDialog
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.ui.components.ScrollEdgeFade
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private fun formatDateGroupHeader(timestamp: Long, todayLabel: String, yesterdayLabel: String): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val now = Calendar.getInstance()

    val targetYear = cal.get(Calendar.YEAR)
    val nowYear = now.get(Calendar.YEAR)
    val targetDay = cal.get(Calendar.DAY_OF_YEAR)
    val nowDay = now.get(Calendar.DAY_OF_YEAR)

    return when {
        targetYear == nowYear && targetDay == nowDay -> todayLabel
        targetYear == nowYear && targetDay == nowDay - 1 -> yesterdayLabel
        targetYear == nowYear -> SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(cal.time)
        else -> SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(cal.time)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    currencySymbol: String = "$",
    selectedAccountId: Long? = null,
    onSelectAccountId: (Long?) -> Unit = {},
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    selectedTypeFilter: String,
    onSelectTypeFilter: (String) -> Unit,
    selectedDateRange: String = "ALL",
    onSelectDateRange: (String) -> Unit = {},
    customStartDate: Long? = null,
    customEndDate: Long? = null,
    onSetCustomDateRange: (Long?, Long?) -> Unit = { _, _ -> },
    minAmountFilter: Double? = null,
    maxAmountFilter: Double? = null,
    onSetMinAmountFilter: (Double?) -> Unit = {},
    onSetMaxAmountFilter: (Double?) -> Unit = {},
    onClearAllFilters: () -> Unit = {},
    onTransactionClick: (TransactionEntity) -> Unit,
    onAddClick: () -> Unit,
    onTransferClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showFilterSheet by remember { mutableStateOf(false) }

    val todayLabel = stringResource(R.string.date_today)
    val yesterdayLabel = stringResource(R.string.date_yesterday)
    val groupedTransactions = remember(transactions, todayLabel, yesterdayLabel) {
        transactions.groupBy { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.dateTimestamp }
            val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
            val headerLabel = formatDateGroupHeader(tx.dateTimestamp, todayLabel, yesterdayLabel)
            dateKey to headerLabel
        }
    }

    val selectedAccount = remember(accounts, selectedAccountId) {
        accounts.find { it.id == selectedAccountId }
    }

    val activeFilterCount = (if (selectedAccountId != null) 1 else 0) +
            (if (selectedCategory != null) 1 else 0) +
            (if (selectedTypeFilter != "ALL") 1 else 0) +
            (if (selectedDateRange != "ALL") 1 else 0) +
            (if (minAmountFilter != null || maxAmountFilter != null) 1 else 0)

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("transactions_screen"),
        floatingActionButton = {
            // Sized down from the 56dp default: still comfortably above the 48dp minimum touch
            // target, but a smaller footprint means it's less likely to fully cover a list row's
            // amount text when a transaction happens to be scrolled to rest right behind it.
            FloatingActionButton(
                onClick = onAddClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("add_transaction_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.action_add_transaction)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Header with Search Bar & Filter Button
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(
                        text = stringResource(R.string.transactions_screen_title),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text(stringResource(R.string.transactions_search_placeholder), style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.action_search),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onSearchQueryChange("") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = stringResource(R.string.action_clear),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("transactions_search_input"),
                            singleLine = true,
                            shape = CircleShape,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        FilledTonalIconButton(
                            onClick = onTransferClick,
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("transactions_transfer_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = stringResource(R.string.action_transfer_funds),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        FilledTonalIconButton(
                            onClick = { showFilterSheet = true },
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("open_filter_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (activeFilterCount > 0) {
                                        Badge {
                                            Text("$activeFilterCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.action_filter),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Quick Date Presets Row
                    Spacer(modifier = Modifier.height(10.dp))
                    val dateOptions = listOf(
                        "ALL" to stringResource(R.string.date_range_all_time),
                        "TODAY" to stringResource(R.string.date_range_today),
                        "THIS_WEEK" to stringResource(R.string.date_range_this_week),
                        "THIS_MONTH" to stringResource(R.string.date_range_this_month),
                        "LAST_30_DAYS" to stringResource(R.string.date_range_last_30_days)
                    )
                    val dateOptionsListState = rememberLazyListState()
                    Box(modifier = Modifier.fillMaxWidth()) {
                        LazyRow(
                            state = dateOptionsListState,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(dateOptions) { (key, label) ->
                                val isSelected = selectedDateRange == key
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectDateRange(key) },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        )
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier.height(30.dp)
                                )
                            }
                        }
                        ScrollEdgeFade(dateOptionsListState, modifier = Modifier.matchParentSize())
                    }

                    // Quick type chips: the common filters without opening the sheet.
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        items(
                            listOf(
                                "ALL" to R.string.type_all,
                                "DEBIT" to R.string.type_expenses,
                                "CREDIT" to R.string.type_income,
                                "PENDING" to R.string.type_needs_review
                            )
                        ) { (key, label) ->
                            val isSelected = selectedTypeFilter == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectTypeFilter(key) },
                                label = {
                                    Text(
                                        stringResource(label),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                    )
                                },
                                shape = CircleShape,
                                modifier = Modifier.height(30.dp).testTag("ledger_type_$key")
                            )
                        }
                    }

                    // Quick Category Tag Pills Bar
                    Spacer(modifier = Modifier.height(6.dp))
                    val categoryPillsListState = rememberLazyListState()
                    Box(modifier = Modifier.fillMaxWidth()) {
                        LazyRow(
                            state = categoryPillsListState,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val allCategoryPills = listOf(null) + categories.map { it.name }
                            items(allCategoryPills) { catName ->
                                val isSelected = (selectedCategory == catName) || (selectedCategory == null && catName == null)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectCategory(catName) },
                                    label = {
                                        Text(
                                            text = catName ?: stringResource(R.string.category_all),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                        ScrollEdgeFade(categoryPillsListState, modifier = Modifier.matchParentSize())
                    }

                    // Active Filter Summary Bar
                    if (activeFilterCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedAccount != null) {
                                item {
                                    InputChip(
                                        selected = true,
                                        onClick = { onSelectAccountId(null) },
                                        label = { Text(stringResource(R.string.active_filter_account, selectedAccount.name)) },
                                        trailingIcon = {
                                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_remove), modifier = Modifier.height(16.dp))
                                        }
                                    )
                                }
                            }

                            if (selectedTypeFilter != "ALL") {
                                item {
                                    val typeLabel = when (selectedTypeFilter) {
                                        "DEBIT" -> stringResource(R.string.type_expenses)
                                        "CREDIT" -> stringResource(R.string.type_income)
                                        "RECURRING" -> stringResource(R.string.type_subscriptions)
                                        "PENDING" -> stringResource(R.string.type_needs_review)
                                        else -> selectedTypeFilter
                                    }
                                    InputChip(
                                        selected = true,
                                        onClick = { onSelectTypeFilter("ALL") },
                                        label = { Text(stringResource(R.string.active_filter_type, typeLabel)) },
                                        trailingIcon = {
                                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_remove), modifier = Modifier.height(16.dp))
                                        }
                                    )
                                }
                            }

                            if (selectedDateRange != "ALL") {
                                item {
                                    val beginningLabel = stringResource(R.string.date_range_beginning)
                                    val presentLabel = stringResource(R.string.date_range_present)
                                    val dateLabel = when (selectedDateRange) {
                                        "THIS_WEEK" -> stringResource(R.string.date_range_this_week)
                                        "THIS_MONTH" -> stringResource(R.string.date_range_this_month)
                                        "LAST_30_DAYS" -> stringResource(R.string.date_range_last_30_days)
                                        "CUSTOM" -> {
                                            val startStr = customStartDate?.let { dateFormat.format(it) } ?: beginningLabel
                                            val endStr = customEndDate?.let { dateFormat.format(it) } ?: presentLabel
                                            stringResource(R.string.date_range_span, startStr, endStr)
                                        }
                                        else -> selectedDateRange
                                    }
                                    InputChip(
                                        selected = true,
                                        onClick = { onSelectDateRange("ALL") },
                                        label = { Text(stringResource(R.string.active_filter_date, dateLabel)) },
                                        trailingIcon = {
                                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_remove), modifier = Modifier.height(16.dp))
                                        }
                                    )
                                }
                            }

                            if (selectedCategory != null) {
                                item {
                                    InputChip(
                                        selected = true,
                                        onClick = { onSelectCategory(null) },
                                        label = { Text(stringResource(R.string.active_filter_category, selectedCategory)) },
                                        trailingIcon = {
                                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_remove), modifier = Modifier.height(16.dp))
                                        }
                                    )
                                }
                            }

                            if (minAmountFilter != null || maxAmountFilter != null) {
                                item {
                                    val amountLabel = when {
                                        minAmountFilter != null && maxAmountFilter != null ->
                                            stringResource(R.string.amount_range_both, currencySymbol, minAmountFilter, currencySymbol, maxAmountFilter)
                                        minAmountFilter != null -> stringResource(R.string.amount_range_min_only, currencySymbol, minAmountFilter)
                                        else -> stringResource(R.string.amount_range_max_only, currencySymbol, maxAmountFilter ?: 0.0)
                                    }
                                    InputChip(
                                        selected = true,
                                        onClick = {
                                            onSetMinAmountFilter(null)
                                            onSetMaxAmountFilter(null)
                                        },
                                        label = { Text(stringResource(R.string.active_filter_amount, amountLabel)) },
                                        trailingIcon = {
                                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_remove), modifier = Modifier.height(16.dp))
                                        }
                                    )
                                }
                            }

                            item {
                                TextButton(onClick = onClearAllFilters) {
                                    Text(stringResource(R.string.action_clear_all), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // Totals for exactly what is shown, so they follow every filter and search.
            if (transactions.isNotEmpty()) {
                item { LedgerSummaryTile(transactions, currencySymbol) }
            }

            // Transactions List Grouped By Date
            if (transactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.transactions_empty_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.transactions_empty_subtitle),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                            if (activeFilterCount > 0 || searchQuery.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        onSearchQueryChange("")
                                        onClearAllFilters()
                                    }
                                ) {
                                    Text(stringResource(R.string.action_reset_filters))
                                }
                            }
                        }
                    }
                }
            } else {
                groupedTransactions.forEach { (dateKeyAndLabel, txList) ->
                    val (_, headerLabel) = dateKeyAndLabel
                    item(key = "day_${dateKeyAndLabel.first}") {
                        LedgerDayBox(
                            label = headerLabel,
                            transactions = txList,
                            accounts = accounts,
                            categories = categories,
                            currencySymbol = currencySymbol,
                            onTransactionClick = onTransactionClick
                        )
                    }
                }
            }
        }
    }

    // Modal Filter Sheet
    if (showFilterSheet) {
        TransactionFilterBottomSheet(
            accounts = accounts,
            categories = categories,
            currencySymbol = currencySymbol,
            selectedAccountId = selectedAccountId,
            selectedTypeFilter = selectedTypeFilter,
            selectedDateRange = selectedDateRange,
            customStartDate = customStartDate,
            customEndDate = customEndDate,
            selectedCategory = selectedCategory,
            minAmountFilter = minAmountFilter,
            maxAmountFilter = maxAmountFilter,
            onSelectAccountId = onSelectAccountId,
            onSelectTypeFilter = onSelectTypeFilter,
            onSelectDateRange = onSelectDateRange,
            onSetCustomDateRange = onSetCustomDateRange,
            onSelectCategory = onSelectCategory,
            onSetMinAmountFilter = onSetMinAmountFilter,
            onSetMaxAmountFilter = onSetMaxAmountFilter,
            onClearAllFilters = onClearAllFilters,
            onDismiss = { showFilterSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFilterBottomSheet(
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String = "$",
    selectedAccountId: Long?,
    selectedTypeFilter: String,
    selectedDateRange: String,
    customStartDate: Long?,
    customEndDate: Long?,
    selectedCategory: String?,
    minAmountFilter: Double? = null,
    maxAmountFilter: Double? = null,
    onSelectAccountId: (Long?) -> Unit,
    onSelectTypeFilter: (String) -> Unit,
    onSelectDateRange: (String) -> Unit,
    onSetCustomDateRange: (Long?, Long?) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSetMinAmountFilter: (Double?) -> Unit = {},
    onSetMaxAmountFilter: (Double?) -> Unit = {},
    onClearAllFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }

    var tempStartDate by remember { mutableStateOf(customStartDate ?: System.currentTimeMillis()) }
    var tempEndDate by remember { mutableStateOf(customEndDate ?: System.currentTimeMillis()) }

    val startCalendar = remember(tempStartDate) { Calendar.getInstance().apply { timeInMillis = tempStartDate } }
    val endCalendar = remember(tempEndDate) { Calendar.getInstance().apply { timeInMillis = tempEndDate } }

    val startDatePicker = remember(tempStartDate) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                tempStartDate = cal.timeInMillis
                onSetCustomDateRange(tempStartDate, tempEndDate)
            },
            startCalendar.get(Calendar.YEAR),
            startCalendar.get(Calendar.MONTH),
            startCalendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val endDatePicker = remember(tempEndDate) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                tempEndDate = cal.timeInMillis
                onSetCustomDateRange(tempStartDate, tempEndDate)
            },
            endCalendar.get(Calendar.YEAR),
            endCalendar.get(Calendar.MONTH),
            endCalendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("filter_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.filter_sheet_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Account Filter
            Text(
                text = stringResource(R.string.filter_section_account),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedAccountId == null,
                    onClick = { onSelectAccountId(null) },
                    label = { Text(stringResource(R.string.filter_all_accounts)) }
                )
                accounts.forEach { acc ->
                    FilterChip(
                        selected = selectedAccountId == acc.id,
                        onClick = { onSelectAccountId(acc.id) },
                        label = { Text(acc.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Transaction Type Filter
            Text(
                text = stringResource(R.string.filter_section_transaction_type),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTypeFilter == "ALL",
                    onClick = { onSelectTypeFilter("ALL") },
                    label = { Text(stringResource(R.string.type_all)) }
                )
                FilterChip(
                    selected = selectedTypeFilter == "DEBIT",
                    onClick = { onSelectTypeFilter("DEBIT") },
                    label = { Text(stringResource(R.string.type_expenses)) }
                )
                FilterChip(
                    selected = selectedTypeFilter == "CREDIT",
                    onClick = { onSelectTypeFilter("CREDIT") },
                    label = { Text(stringResource(R.string.type_income)) }
                )
                FilterChip(
                    selected = selectedTypeFilter == "TRANSFER",
                    onClick = { onSelectTypeFilter("TRANSFER") },
                    label = { Text(stringResource(R.string.type_transfers)) }
                )
                FilterChip(
                    selected = selectedTypeFilter == "RECURRING",
                    onClick = { onSelectTypeFilter("RECURRING") },
                    label = { Text(stringResource(R.string.type_subscriptions)) }
                )
                FilterChip(
                    selected = selectedTypeFilter == "PENDING",
                    onClick = { onSelectTypeFilter("PENDING") },
                    label = { Text(stringResource(R.string.type_needs_review)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Date Range Filter & Custom Selection
            Text(
                text = stringResource(R.string.filter_section_date_range),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedDateRange == "ALL",
                    onClick = { onSelectDateRange("ALL") },
                    label = { Text(stringResource(R.string.date_range_all_time)) }
                )
                FilterChip(
                    selected = selectedDateRange == "THIS_WEEK",
                    onClick = { onSelectDateRange("THIS_WEEK") },
                    label = { Text(stringResource(R.string.date_range_this_week)) }
                )
                FilterChip(
                    selected = selectedDateRange == "THIS_MONTH",
                    onClick = { onSelectDateRange("THIS_MONTH") },
                    label = { Text(stringResource(R.string.date_range_this_month)) }
                )
                FilterChip(
                    selected = selectedDateRange == "LAST_30_DAYS",
                    onClick = { onSelectDateRange("LAST_30_DAYS") },
                    label = { Text(stringResource(R.string.date_range_last_30_days)) }
                )
                FilterChip(
                    selected = selectedDateRange == "CUSTOM",
                    onClick = {
                        onSelectDateRange("CUSTOM")
                        onSetCustomDateRange(tempStartDate, tempEndDate)
                    },
                    label = { Text(stringResource(R.string.date_range_custom)) }
                )
            }

            // If Custom Range is selected, show Start & End date pickers
            if (selectedDateRange == "CUSTOM") {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.filter_custom_date_range_selection),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Start Date
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.filter_from_date), style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = { startDatePicker.show() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = stringResource(R.string.filter_start_date),
                                        modifier = Modifier.height(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = dateFormat.format(tempStartDate),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            // End Date
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.filter_to_date), style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = { endDatePicker.show() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = stringResource(R.string.filter_end_date),
                                        modifier = Modifier.height(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = dateFormat.format(tempEndDate),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Category Filter
            Text(
                text = stringResource(R.string.filter_section_category),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onSelectCategory(null) },
                    label = { Text(stringResource(R.string.category_all)) }
                )
                categories.forEach { cat ->
                    val isCatSelected = selectedCategory.equals(cat.name, ignoreCase = true)
                    val catColor = try {
                        Color(android.graphics.Color.parseColor(cat.colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }
                    FilterChip(
                        selected = isCatSelected,
                        onClick = {
                            if (isCatSelected) {
                                onSelectCategory(null)
                            } else {
                                onSelectCategory(cat.name)
                            }
                        },
                        label = { Text(cat.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = catColor.copy(alpha = 0.15f),
                            labelColor = catColor,
                            selectedContainerColor = catColor,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Amount Range Filter
            Text(
                text = stringResource(R.string.filter_section_amount_range),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(8.dp))
            var minAmountText by remember(minAmountFilter) {
                mutableStateOf(minAmountFilter?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "")
            }
            var maxAmountText by remember(maxAmountFilter) {
                mutableStateOf(maxAmountFilter?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = minAmountText,
                    onValueChange = {
                        minAmountText = it
                        onSetMinAmountFilter(it.toDoubleOrNull())
                    },
                    label = { Text(stringResource(R.string.filter_amount_min)) },
                    placeholder = { Text(stringResource(R.string.filter_amount_no_min)) },
                    leadingIcon = { Text(currencySymbol, style = MaterialTheme.typography.bodyLarge) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).testTag("min_amount_filter_input")
                )
                OutlinedTextField(
                    value = maxAmountText,
                    onValueChange = {
                        maxAmountText = it
                        onSetMaxAmountFilter(it.toDoubleOrNull())
                    },
                    label = { Text(stringResource(R.string.filter_amount_max)) },
                    placeholder = { Text(stringResource(R.string.filter_amount_no_max)) },
                    leadingIcon = { Text(currencySymbol, style = MaterialTheme.typography.bodyLarge) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).testTag("max_amount_filter_input")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onClearAllFilters()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_clear_all))
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_apply))
                }
            }
        }
    }
}

/** Money in, money out and the net for the transactions on screen (transfers left out). */
@Composable
private fun LedgerSummaryTile(transactions: List<TransactionEntity>, currencySymbol: String) {
    val counted = transactions.filter { it.categoryTag != CategoryTotals.TRANSFER_CATEGORY }
    val moneyIn = counted.filter { it.type == "CREDIT" }.sumOf { it.amount }
    val moneyOut = counted.filter { it.type == "DEBIT" }.sumOf { it.amount }
    val net = moneyIn - moneyOut
    BentoTile(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).testTag("ledger_summary"), contentPadding = 14) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LedgerFigure(stringResource(R.string.ledger_money_in), "+" + formatMoney(currencySymbol, moneyIn).substringBefore('.'), IncomeGreen, Modifier.weight(1f))
            LedgerFigure(stringResource(R.string.ledger_money_out), "−" + formatMoney(currencySymbol, moneyOut).substringBefore('.'), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            LedgerFigure(
                stringResource(R.string.ledger_net),
                (if (net >= 0) "+" else "−") + formatMoney(currencySymbol, kotlin.math.abs(net)).substringBefore('.'),
                if (net >= 0) IncomeGreen else ExpenseRed,
                Modifier.weight(1f)
            )
        }
        Text(
            pluralStringResource(R.plurals.ledger_count, transactions.size, transactions.size),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LedgerFigure(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = color, maxLines = 1)
    }
}

/** One day as a single tile: its name and net total, then each transaction as a row. */
@Composable
private fun LedgerDayBox(
    label: String,
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    onTransactionClick: (TransactionEntity) -> Unit
) {
    val net = transactions.filter { it.categoryTag != CategoryTotals.TRANSFER_CATEGORY }.sumOf { if (it.type == "CREDIT") it.amount else -it.amount }
    BentoTile(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), contentPadding = 14) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
            Text(
                (if (net >= 0) "+" else "−") + formatMoney(currencySymbol, kotlin.math.abs(net)).substringBefore('.'),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (net >= 0) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        transactions.forEachIndexed { index, tx ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            LedgerRow(tx, accounts.find { it.id == tx.accountId }, categories, currencySymbol, onClick = { onTransactionClick(tx) })
        }
    }
}

private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

/** A transaction row: category icon with account dot, name, category · account, amount and time. */
@Composable
private fun LedgerRow(tx: TransactionEntity, account: AccountEntity?, categories: List<CategoryEntity>, currencySymbol: String, onClick: () -> Unit) {
    val category = categories.find { it.name.equals(tx.categoryTag, ignoreCase = true) }
    val isCredit = tx.type == "CREDIT"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag("transaction_item_${tx.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryAvatar(category?.iconName, tx.categoryTag, category?.colorHex, size = 40.dp, accountColorHex = account?.colorHex, muted = tx.isPendingReview)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                tx.title.removePrefix("SMS: ").removePrefix("SMS:").trim(),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (tx.source == "SMS_AUTO") {
                    Icon(Icons.Default.Sms, contentDescription = stringResource(R.string.ledger_from_sms), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    listOfNotNull(tx.categoryTag, account?.name).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = category?.colorHex?.let { accentOf(it).ink } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                (if (isCredit) "+" else "−") + formatMoney(currencySymbol, tx.amount),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isCredit) IncomeGreen else MaterialTheme.colorScheme.onSurface
            )
            if (tx.isPendingReview) {
                Text(stringResource(R.string.transaction_pending_label), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = PendingAmber)
            } else {
                Text(timeFormat.format(tx.dateTimestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
