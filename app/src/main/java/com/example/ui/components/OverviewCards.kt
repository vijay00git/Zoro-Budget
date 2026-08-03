package com.example.ui.components
import com.example.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpenseOnRedContainer
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedContainer
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenContainer
import com.example.ui.theme.IncomeOnGreenContainer
import com.example.ui.theme.PendingAmber

@Composable
fun TotalBalanceCard(
    totalBalance: Double,
    accountName: String = stringResource(R.string.overview_default_account_name),
    currencySymbol: String = "$",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("total_balance_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.currency_amount_format, currencySymbol, totalBalance),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.overview_active_badge_label),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeExpenseSummaryRow(
    totalIncome: Double,
    totalExpense: Double,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Income Card
        Card(
            modifier = Modifier
                .weight(1f)
                .testTag("summary_income_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = IncomeGreenContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(IncomeGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = stringResource(R.string.overview_income_label),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = stringResource(R.string.overview_income_label),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = IncomeOnGreenContainer.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = stringResource(R.string.currency_amount_format, currencySymbol, totalIncome),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    )
                }
            }
        }

        // Expense Card
        Card(
            modifier = Modifier
                .weight(1f)
                .testTag("summary_expense_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ExpenseRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = stringResource(R.string.overview_expenses_label),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = stringResource(R.string.overview_expenses_label),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = stringResource(R.string.currency_amount_format, currencySymbol, totalExpense),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun PendingReviewBanner(
    pendingCount: Int,
    onClickReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = pendingCount > 0) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onClickReview() }
                .testTag("pending_review_banner"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = ExpenseRedContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
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
                            .background(ExpenseRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = stringResource(R.string.overview_action_required_description),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = stringResource(R.string.overview_pending_action_required_format, pendingCount),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ExpenseOnRedContainer
                            )
                        )
                        Text(
                            text = stringResource(R.string.overview_pending_review_subtitle),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ExpenseOnRedContainer.copy(alpha = 0.8f)
                            )
                        )
                    }
                }

                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.overview_review_button_label),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetHealthGaugeCard(
    totalIncome: Double,
    totalExpense: Double,
    totalBudgetLimit: Double = 0.0,
    lowBalanceAccountsCount: Int = 0,
    currencySymbol: String = "$",
    onViewAnalyticsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val budgetConsumption = if (totalBudgetLimit > 0) totalExpense / totalBudgetLimit else if (totalIncome > 0) totalExpense / totalIncome else 0.5
    var score = 100
    if (budgetConsumption > 1.0) score -= 40
    else if (budgetConsumption > 0.8) score -= 20
    else if (budgetConsumption > 0.6) score -= 10

    if (lowBalanceAccountsCount > 0) score -= (lowBalanceAccountsCount * 15)
    score = score.coerceIn(10, 100)

    val (statusLabel, statusColor) = when {
        score >= 80 -> stringResource(R.string.overview_health_excellent) to IncomeGreen
        score >= 60 -> stringResource(R.string.overview_health_good) to MaterialTheme.colorScheme.primary
        score >= 40 -> stringResource(R.string.overview_health_caution) to PendingAmber
        else -> stringResource(R.string.overview_health_critical) to ExpenseRed
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onViewAnalyticsClick() }
            .testTag("budget_health_gauge_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.overview_financial_health_gauge_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelMedium.copy(color = statusColor, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.overview_score_percentage_format, score),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = statusColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { (score / 100f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (totalBudgetLimit > 0) {
                        stringResource(R.string.overview_spent_of_budget_format, currencySymbol, totalExpense, totalBudgetLimit)
                    } else {
                        stringResource(R.string.overview_monthly_expenses_format, currencySymbol, totalExpense)
                    },
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                if (lowBalanceAccountsCount > 0) {
                    Text(
                        text = stringResource(R.string.overview_low_account_count_format, lowBalanceAccountsCount),
                        style = MaterialTheme.typography.bodySmall.copy(color = ExpenseRed, fontWeight = FontWeight.Bold)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.overview_accounts_safe_label),
                        style = MaterialTheme.typography.bodySmall.copy(color = IncomeGreen, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

data class CategoryProgressData(
    val name: String,
    val spent: Double,
    val limit: Double,
    val ratio: Double,
    val colorHex: String,
    val iconName: String
)

@Composable
fun CategorySpendingProgressCard(
    categories: List<com.example.data.local.entity.CategoryEntity>,
    transactions: List<com.example.data.local.entity.TransactionEntity>,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier
) {
    val categorySpending = remember(categories, transactions) {
        val debitTxs = transactions.filter { it.type == "DEBIT" }
        val spendingMap = debitTxs.groupBy { it.categoryTag.lowercase() }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        categories.map { cat ->
            val spent = spendingMap[cat.name.lowercase()] ?: 0.0
            val limit = cat.monthlyBudgetLimit
            val ratio = if (limit > 0) (spent / limit).coerceAtMost(1.5) else 0.0
            CategoryProgressData(
                name = cat.name,
                spent = spent,
                limit = limit,
                ratio = ratio,
                colorHex = cat.colorHex,
                iconName = cat.iconName
            )
        }.filter { it.spent > 0 }.sortedByDescending { it.spent }
    }

    if (categorySpending.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_spending_progress_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.overview_category_budget_progress_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = stringResource(R.string.overview_monthly_spending_label),
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val displayList = categorySpending.take(5)
            displayList.forEachIndexed { index, item ->
                val progressColor = when {
                    item.ratio >= 1.0 -> ExpenseRed
                    item.ratio >= 0.8 -> PendingAmber
                    else -> IncomeGreen
                }

                val itemColor = try {
                    Color(android.graphics.Color.parseColor(item.colorHex))
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.primary
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(itemColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = categoryIconFor(item.iconName),
                                    contentDescription = null,
                                    tint = itemColor,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Text(
                            text = if (item.limit > 0) {
                                stringResource(R.string.overview_spent_of_limit_format, currencySymbol, item.spent, item.limit)
                            } else {
                                stringResource(R.string.overview_spent_only_format, currencySymbol, item.spent)
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (item.ratio >= 1.0) ExpenseRed else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val progressValue = if (item.limit > 0) (item.spent / item.limit).coerceIn(0.0, 1.0).toFloat() else 0.5f

                    LinearProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = progressColor,
                        trackColor = progressColor.copy(alpha = 0.15f)
                    )

                    // The bar caps visually at 100%, so spell out how far over budget this
                    // category actually is instead of just showing an undifferentiated full bar.
                    // (item.ratio is coerced to 1.5 for progress-bar/color purposes, so the real
                    // overage is recomputed here rather than reusing that capped value.)
                    if (item.limit > 0 && item.spent > item.limit) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = stringResource(R.string.reports_screen_over_budget_by_percent, ((item.spent / item.limit) - 1.0) * 100f),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        )
                    }

                    if (index < displayList.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingSubscriptionsWidget(
    transactions: List<com.example.data.local.entity.TransactionEntity>,
    currencySymbol: String = "$",
    onManageSubscriptionsClick: () -> Unit = {},
    onMarkAsPaid: (com.example.data.local.entity.TransactionEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val recurringTxList = remember(transactions) {
        transactions.filter { it.isRecurring || it.title.contains("Subscription", ignoreCase = true) || it.title.contains("Bill", ignoreCase = true) || it.title.contains("Netflix", ignoreCase = true) || it.title.contains("Spotify", ignoreCase = true) || it.title.contains("Rent", ignoreCase = true) }
            .distinctBy { it.title.lowercase() }
            .take(4)
    }

    if (recurringTxList.isEmpty()) return

    val totalMonthlyCommitment = remember(recurringTxList) {
        recurringTxList.sumOf { it.amount }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("upcoming_subscriptions_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Title & Active Count + Total Commitment Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = stringResource(R.string.overview_recurring_bills_label),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.overview_recurring_bills_label),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${recurringTxList.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.overview_monthly_commitment_format, currencySymbol, totalMonthlyCommitment),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recurring Bills List
            recurringTxList.forEachIndexed { index, tx ->
                val titleLower = tx.title.lowercase()
                val (avatarBg, avatarText) = when {
                    titleLower.contains("electric") || titleLower.contains("power") || titleLower.contains("utility") ->
                        Color(0xFFFF9800) to Color(0xFFFFF3E0)
                    titleLower.contains("netflix") || titleLower.contains("movie") || titleLower.contains("tv") ->
                        Color(0xFFE53935) to Color(0xFFFFEBEE)
                    titleLower.contains("gym") || titleLower.contains("health") || titleLower.contains("fitness") ->
                        Color(0xFF009688) to Color(0xFFE0F2F1)
                    titleLower.contains("spotify") || titleLower.contains("music") ->
                        Color(0xFF4CAF50) to Color(0xE8E8F5E9)
                    titleLower.contains("rent") || titleLower.contains("lease") || titleLower.contains("housing") ->
                        Color(0xFF3F51B5) to Color(0xFFE8EAF6)
                    else ->
                        MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = avatarBg.copy(alpha = 0.2f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = tx.title.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = avatarBg
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = tx.recurringFrequency ?: "MONTHLY",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Text(
                                        text = if (tx.recurringDueDate != null) stringResource(R.string.overview_due_date_format, tx.recurringDueDate) else stringResource(R.string.overview_auto_pay_label),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.currency_amount_format, currencySymbol, tx.amount),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Surface(
                                color = IncomeGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .testTag("mark_recurring_paid_${tx.id}")
                                    .clickable { onMarkAsPaid(tx) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = stringResource(R.string.overview_mark_as_paid_description),
                                        tint = IncomeGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = stringResource(R.string.overview_mark_paid_label),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = IncomeGreen
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                if (index < recurringTxList.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onManageSubscriptionsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.overview_manage_subscriptions_button), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

@Composable
fun NetWorthSparklineCard(
    accounts: List<com.example.data.local.entity.AccountEntity>,
    transactions: List<com.example.data.local.entity.TransactionEntity>,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier
) {
    val totalNetWorth = remember(accounts) { accounts.sumOf { acc -> acc.balance } }
    if (accounts.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("net_worth_sparkline_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.overview_net_worth_trend_label),
                        style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = stringResource(R.string.currency_amount_format, currencySymbol, totalNetWorth),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = IncomeGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.overview_accounts_count_format, accounts.size),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Account Balance Multi-color Distribution Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            ) {
                val colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary,
                    IncomeGreen,
                    PendingAmber
                )
                accounts.forEachIndexed { idx, acc ->
                    val weight = if (totalNetWorth > 0) (acc.balance / totalNetWorth).toFloat().coerceAtLeast(0.05f) else 1f / accounts.size
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .background(colors[idx % colors.size])
                    )
                }
            }
        }
    }
}

