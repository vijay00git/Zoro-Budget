package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.utils.BillAlertSeverity
import com.example.data.utils.RecurringBillUtils
import com.example.ui.components.BudgetHealthGaugeCard
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.IncomeExpenseSummaryRow
import com.example.ui.components.MonthlyBarChart
import com.example.ui.components.MonthlySpendingTrendChart
import com.example.ui.components.PendingReviewBanner
import com.example.ui.components.TotalBalanceCard
import com.example.ui.components.TransactionItem
import com.example.ui.theme.ExpenseOnRedContainer
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedContainer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private fun formatDateGroupHeader(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val now = Calendar.getInstance()

    val targetYear = cal.get(Calendar.YEAR)
    val nowYear = now.get(Calendar.YEAR)
    val targetDay = cal.get(Calendar.DAY_OF_YEAR)
    val nowDay = now.get(Calendar.DAY_OF_YEAR)

    return when {
        targetYear == nowYear && targetDay == nowDay -> "Today"
        targetYear == nowYear && targetDay == nowDay - 1 -> "Yesterday"
        targetYear == nowYear -> SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(cal.time)
        else -> SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(cal.time)
    }
}

@Composable
fun HomeScreen(
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    pendingTransactions: List<TransactionEntity>,
    categories: List<CategoryEntity> = emptyList(),
    currencySymbol: String = "$",
    lowBalanceThreshold: Double = 200.0,
    selectedAccountId: Long? = null,
    onSelectAccount: (Long?) -> Unit = {},
    onAddAccountClick: () -> Unit = {},
    onAddRecurringBillClick: () -> Unit = {},
    onTransferClick: (Long?) -> Unit = {},
    onPendingReviewClick: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    onSeeAllClick: () -> Unit,
    onScanInbox: () -> Unit = {},
    smsSyncFromDate: Long? = null,
    onAddTransactionClick: () -> Unit = {},
    onMarkRecurringAsPaid: (TransactionEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentAccount = accounts.find { it.id == selectedAccountId }
    val lowBalanceAccounts = remember(accounts, lowBalanceThreshold) {
        accounts.filter { it.balance < lowBalanceThreshold }
    }
    val displayTransactions = if (selectedAccountId != null) {
        transactions.filter { it.accountId == selectedAccountId }
    } else transactions

    val totalBalance = if (selectedAccountId != null) {
        currentAccount?.balance ?: 0.0
    } else {
        accounts.sumOf { it.balance }
    }

    val totalIncome = displayTransactions.filter { it.type == "CREDIT" }.sumOf { it.amount }
    val totalExpense = displayTransactions.filter { it.type == "DEBIT" }.sumOf { it.amount }

    val upcomingBillsWithAlerts = remember(transactions) {
        transactions.filter { it.isRecurring }
            .distinctBy { it.title.lowercase() }
            .map { bill -> bill to RecurringBillUtils.calculateAlert(bill) }
            .sortedBy { (_, alert) -> alert.daysDifference }
    }
    val totalUpcomingBillsAmount = remember(upcomingBillsWithAlerts) {
        upcomingBillsWithAlerts.sumOf { (bill, _) -> bill.amount }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Account Switcher Filter Bar
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 2.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = selectedAccountId == null,
                        onClick = { onSelectAccount(null) },
                        label = { Text("All Accounts", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                items(accounts, key = { it.id }) { acc ->
                    val isSelected = selectedAccountId == acc.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectAccount(acc.id) },
                        label = {
                            Text(
                                text = "${acc.name} (${String.format(Locale.getDefault(), "%s%,.0f", currencySymbol, acc.balance)})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { onTransferClick(selectedAccountId) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("home_transfer_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Transfer Funds",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Transfer",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        IconButton(
                            onClick = onAddAccountClick,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Account",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Main Balance Card
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                TotalBalanceCard(
                    totalBalance = totalBalance,
                    accountName = currentAccount?.name ?: "All Accounts Total",
                    currencySymbol = currencySymbol
                )
            }
        }

        // Quick Actions Speed Dial Bar (Item 4)
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onAddTransactionClick,
                        modifier = Modifier.weight(1f).testTag("quick_add_expense_btn"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Transaction", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    OutlinedButton(
                        onClick = { onTransferClick(selectedAccountId) },
                        modifier = Modifier.weight(1f).testTag("quick_transfer_btn"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Transfer", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // 1-Tap Bank SMS Sync Status Card (Item 6)
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_sms_sync_widget"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = "SMS Auto-Sync",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Bank SMS Auto-Sync",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                val syncSubtext = if (smsSyncFromDate != null && smsSyncFromDate > 0L) {
                                    val formatted = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(java.util.Date(smsSyncFromDate))
                                    "From $formatted onwards"
                                } else {
                                    "All Bank Messages Enabled"
                                }
                                Text(
                                    text = syncSubtext,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = onScanInbox,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("home_scan_sms_now_btn")
                        ) {
                            Text("Sync Now", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        // Income vs Expense Summary Cards
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                IncomeExpenseSummaryRow(
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    currencySymbol = currencySymbol
                )
            }
        }

        // Net Worth & Balance Trend Sparkline Card (Item 5)
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                com.example.ui.components.NetWorthSparklineCard(
                    accounts = accounts,
                    transactions = transactions,
                    currencySymbol = currencySymbol
                )
            }
        }

        // Upcoming Subscriptions & Recurring Bills Widget (Item 4)
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                com.example.ui.components.UpcomingSubscriptionsWidget(
                    transactions = transactions,
                    currencySymbol = currencySymbol,
                    onManageSubscriptionsClick = onSeeAllClick,
                    onMarkAsPaid = onMarkRecurringAsPaid
                )
            }
        }

        // Category Spending Progress Card (Item 3)
        if (categories.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    com.example.ui.components.CategorySpendingProgressCard(
                        categories = categories,
                        transactions = transactions,
                        currencySymbol = currencySymbol
                    )
                }
            }
        }

        // Financial Health Gauge Card
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                BudgetHealthGaugeCard(
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    lowBalanceAccountsCount = lowBalanceAccounts.size,
                    currencySymbol = currencySymbol,
                    onViewAnalyticsClick = onSeeAllClick
                )
            }
        }

        // Low Balance Warning & Pending SMS Review Alert Cards (Compact Side-by-Side)
        if (lowBalanceAccounts.isNotEmpty() || pendingTransactions.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    if (lowBalanceAccounts.isNotEmpty() && pendingTransactions.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Low Balance Warning Compact Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("low_balance_warning_banner"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.12f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(ExpenseRed.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Low Balance",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = ExpenseRed
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (lowBalanceAccounts.size == 1) {
                                                "${lowBalanceAccounts.first().name} < ${currencySymbol}${lowBalanceThreshold.toInt()}"
                                            } else {
                                                "${lowBalanceAccounts.size} low accounts"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Action Required (Pending Review) Compact Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onPendingReviewClick() }
                                    .testTag("pending_review_banner"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = ExpenseRedContainer),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(ExpenseRed),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${pendingTransactions.size} Action Required",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = ExpenseOnRedContainer
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Review pending SMS",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = ExpenseOnRedContainer.copy(alpha = 0.8f),
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    } else if (lowBalanceAccounts.isNotEmpty()) {
                        // Single Low Balance Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("low_balance_warning_banner"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.12f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(ExpenseRed.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (lowBalanceAccounts.size == 1) "Low Balance Warning" else "${lowBalanceAccounts.size} Accounts Below Threshold",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ExpenseRed
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = lowBalanceAccounts.joinToString(", ") {
                                            "${it.name} (${String.format(Locale.getDefault(), "%s%,.2f", currencySymbol, it.balance)})"
                                        } + " below minimum threshold (${String.format(Locale.getDefault(), "%s%,.2f", currencySymbol, lowBalanceThreshold)})",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        // Single Pending SMS Review Banner
                        PendingReviewBanner(
                            pendingCount = pendingTransactions.size,
                            onClickReview = onPendingReviewClick
                        )
                    }
                }
            }
        }

        // Interactive Visual Charts & Diagrams Section
        item {
            var selectedChartTab by remember { mutableStateOf(0) } // 0: Trends, 1: Categories, 2: Daily Bars

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Visual Analytics & Trends",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = selectedChartTab == 0,
                            onClick = { selectedChartTab = 0 },
                            label = { Text("Trends", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(28.dp)
                        )
                        FilterChip(
                            selected = selectedChartTab == 1,
                            onClick = { selectedChartTab = 1 },
                            label = { Text("Categories", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(28.dp)
                        )
                        FilterChip(
                            selected = selectedChartTab == 2,
                            onClick = { selectedChartTab = 2 },
                            label = { Text("Daily", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                when (selectedChartTab) {
                    0 -> MonthlySpendingTrendChart(
                        transactions = displayTransactions,
                        categories = categories,
                        currencySymbol = currencySymbol
                    )
                    1 -> CategoryDonutChart(
                        transactions = displayTransactions,
                        categories = categories,
                        currencySymbol = currencySymbol
                    )
                    2 -> MonthlyBarChart(
                        transactions = displayTransactions,
                        currencySymbol = currencySymbol
                    )
                }
            }
        }

        // Upcoming Bills Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Upcoming Bills & EMIs",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Tracked recurring dues & custom alerts",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAddRecurringBillClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Bill", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Bill", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    if (upcomingBillsWithAlerts.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "%s%,.2f/mo", currencySymbol, totalUpcomingBillsAmount),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }

        // Upcoming Bills List
        if (upcomingBillsWithAlerts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Upcoming Bills or EMIs Tracked",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap '+ Bill' above to add custom EMI or recurring due dates with automatic alert tracking.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onAddRecurringBillClick) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Recurring Bill / EMI")
                        }
                    }
                }
            }
        } else {
            items(upcomingBillsWithAlerts, key = { "bill_${it.first.id}" }) { (bill, alert) ->
                val acc = accounts.find { it.id == bill.accountId }

                val (badgeBg, badgeFg) = when (alert.severity) {
                    BillAlertSeverity.URGENT -> ExpenseRedContainer to ExpenseOnRedContainer
                    BillAlertSeverity.WARNING -> Color(0xFFFEF3C7) to Color(0xFF92400E) // Amber/Yellow alert
                    BillAlertSeverity.INFO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clickable { onTransactionClick(bill) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(badgeBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = null,
                                    tint = badgeFg,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bill.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Alert status badge (e.g. Due Today, Due in 4 days, Overdue by 2 days)
                                    Surface(
                                        color = badgeBg,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = alert.alertMessage.uppercase(),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = badgeFg,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }

                                    Text(
                                        text = alert.frequencyLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format(Locale.getDefault(), "%s%,.2f", currencySymbol, bill.amount),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (alert.severity == BillAlertSeverity.URGENT) ExpenseRed else MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = if (acc != null) acc.name else bill.categoryTag,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
