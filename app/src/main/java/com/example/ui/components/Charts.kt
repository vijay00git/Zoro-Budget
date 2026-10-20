package com.example.ui.components
import com.example.R

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.ui.theme.ExpenseRed
import java.util.Calendar
import java.util.Locale
import kotlin.math.atan2

data class CategoryExpense(
    val categoryName: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

@Composable
fun CategoryDonutChart(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier
) {
    // Net per category, so money received back in a category reduces its slice
    val totals = remember(transactions) { com.example.data.utils.CategoryTotals.of(transactions) }
    val totalExpense = totals.expense

    if (totalExpense <= 0.0) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.chart_no_expenses_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.chart_no_expenses_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        }
        return
    }

    // Default palette for category tags
    val palette = listOf(
        Color(0xFFEF4444), Color(0xFF10B981), Color(0xFF3B82F6),
        Color(0xFF8B5CF6), Color(0xFFF59E0B), Color(0xFFEC4899),
        Color(0xFF14B8A6), Color(0xFF6366F1), Color(0xFF64748B)
    )

    val categoryExpenses = totals.spendingByCategory
        .mapNotNull { (catName, sum) ->
            if (sum <= 0.0) return@mapNotNull null

            val catObj = categories.find { it.name.equals(catName, ignoreCase = true) }
            val color = catObj?.colorHex?.let { hex ->
                try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { null }
            } ?: palette[Math.abs(catName.hashCode()) % palette.size]

            CategoryExpense(
                categoryName = catName,
                amount = sum,
                percentage = (((sum / totalExpense) * 100).coerceIn(0.0, 100.0)).toFloat(),
                color = color
            )
        }
        .sortedByDescending { it.amount }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_donut_chart"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.chart_expenses_by_category_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .size(190.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = offset.x - center.x
                                val dy = offset.y - center.y
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f

                                // Start angle is -90 deg (270)
                                var currentAngle = 0f
                                categoryExpenses.forEachIndexed { idx, cat ->
                                    val sweep = (cat.percentage / 100f) * 360f
                                    val start = currentAngle
                                    val end = currentAngle + sweep

                                    // Rotate angle to match start at top (-90 deg)
                                    val adjustedAngle = (angle + 90f) % 360f
                                    if (adjustedAngle in start..end) {
                                        selectedIndex = if (selectedIndex == idx) null else idx
                                    }
                                    currentAngle += sweep
                                }
                            }
                        }
                ) {
                    var startAngle = -90f
                    val strokeWidth = 36.dp.toPx()

                    categoryExpenses.forEachIndexed { index, item ->
                        val safePercentage = item.percentage.coerceIn(0f, 100f)
                        val sweepAngle = (safePercentage / 100f) * 360f
                        val isSelected = selectedIndex == index

                        drawArc(
                            color = item.color,
                            startAngle = startAngle,
                            sweepAngle = (sweepAngle - 2f).coerceAtLeast(0f),
                            useCenter = false,
                            style = Stroke(
                                width = if (isSelected) strokeWidth * 1.15f else strokeWidth,
                                cap = StrokeCap.Round
                            ),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                        )
                        startAngle += sweepAngle
                    }
                }

                // Center Text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val displayCat = selectedIndex?.let { categoryExpenses.getOrNull(it) }
                    if (displayCat != null) {
                        Text(
                            text = displayCat.categoryName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = displayCat.color
                            )
                        )
                        Text(
                            text = stringResource(R.string.currency_amount_format, currencySymbol, displayCat.amount),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = stringResource(R.string.percentage_one_decimal_format, displayCat.percentage),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.chart_total_spent_label),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                        Text(
                            text = stringResource(R.string.currency_amount_format, currencySymbol, totalExpense),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Category Chips Legend
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                categoryExpenses.take(6).forEachIndexed { idx, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedIndex = if (selectedIndex == idx) null else idx }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(item.color)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = item.categoryName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selectedIndex == idx) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.currency_amount_format, currencySymbol, item.amount),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.percentage_parenthesized_format, item.percentage),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyBarChart(
    transactions: List<TransactionEntity>,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier
) {
    // Group debits by day of month (1..31)
    val dailyDebits = remember(transactions) {
        val map = mutableMapOf<Int, Double>()
        val cal = Calendar.getInstance()
        transactions.filter { it.type == "DEBIT" }.forEach { tx ->
            cal.timeInMillis = tx.dateTimestamp
            val day = cal.get(Calendar.DAY_OF_MONTH)
            map[day] = (map[day] ?: 0.0) + tx.amount
        }
        map
    }

    val maxAmount = remember(dailyDebits) {
        (dailyDebits.values.maxOrNull() ?: 100.0).coerceAtLeast(50.0)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_bar_chart"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.chart_daily_spending_trend_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Surface(
                    color = ExpenseRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.chart_peak_format, currencySymbol, maxAmount),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ExpenseRed,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val barColor = ExpenseRed
            val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val daysInMonth = 31
                val barSpacing = canvasWidth / daysInMonth
                val barWidth = (barSpacing * 0.6f).coerceAtLeast(3f)

                // Draw horizontal grid lines
                for (i in 0..3) {
                    val y = canvasHeight * (i / 3f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1f
                    )
                }

                // Draw daily bars
                for (day in 1..daysInMonth) {
                    val amount = dailyDebits[day] ?: 0.0
                    val barHeight = ((amount / maxAmount) * canvasHeight).toFloat()
                    val x = (day - 1) * barSpacing + (barSpacing / 2)

                    if (barHeight > 0f) {
                        drawLine(
                            color = barColor,
                            start = Offset(x, canvasHeight),
                            end = Offset(x, canvasHeight - barHeight),
                            strokeWidth = barWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.chart_day_label_1), style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)))
                Text(stringResource(R.string.chart_day_label_10), style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)))
                Text(stringResource(R.string.chart_day_label_20), style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)))
                Text(stringResource(R.string.chart_day_label_31), style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)))
            }
        }
    }
}

data class MonthCategoryTrend(
    val monthLabel: String,
    val fullMonthName: String,
    val yearMonthKey: String,
    val totalExpense: Double,
    val categoryBreakdown: Map<String, Double>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthlySpendingTrendChart(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier
) {
    var chartMode by remember { mutableStateOf("STACKED") } // "STACKED", "TREND_LINE"
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var selectedMonthIndex by remember { mutableStateOf<Int?>(null) }

    val defaultPalette = listOf(
        Color(0xFFEF4444), Color(0xFF10B981), Color(0xFF3B82F6),
        Color(0xFF8B5CF6), Color(0xFFF59E0B), Color(0xFFEC4899),
        Color(0xFF14B8A6), Color(0xFF6366F1), Color(0xFF64748B)
    )

    fun getCategoryColor(catName: String): Color {
        val catObj = categories.find { it.name.equals(catName, ignoreCase = true) }
        return catObj?.colorHex?.let { hex ->
            try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { null }
        } ?: defaultPalette[Math.abs(catName.hashCode()) % defaultPalette.size]
    }

    val monthlyData = remember(transactions, selectedCategoryFilter) {
        val list = mutableListOf<MonthCategoryTrend>()
        val monthFormat = java.text.SimpleDateFormat("MMM", Locale.getDefault())
        val fullMonthFormat = java.text.SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        for (i in 5 downTo 0) {
            val monthCal = Calendar.getInstance()
            monthCal.add(Calendar.MONTH, -i)
            monthCal.set(Calendar.DAY_OF_MONTH, 1)
            monthCal.set(Calendar.HOUR_OF_DAY, 0)
            monthCal.set(Calendar.MINUTE, 0)
            monthCal.set(Calendar.SECOND, 0)
            val startMs = monthCal.timeInMillis

            monthCal.add(Calendar.MONTH, 1)
            val endMs = monthCal.timeInMillis

            monthCal.add(Calendar.MONTH, -1)
            val label = monthFormat.format(monthCal.time)
            val fullLabel = fullMonthFormat.format(monthCal.time)
            val key = "${monthCal.get(Calendar.YEAR)}-${monthCal.get(Calendar.MONTH)}"

            val monthDebits = transactions.filter {
                it.type == "DEBIT" && it.dateTimestamp in startMs until endMs
            }

            val categoryMap = monthDebits.groupBy { it.categoryTag }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            val total = if (selectedCategoryFilter != null) {
                (categoryMap[selectedCategoryFilter] ?: 0.0).coerceAtLeast(0.0)
            } else {
                monthDebits.sumOf { it.amount }.coerceAtLeast(0.0)
            }

            list.add(
                MonthCategoryTrend(
                    monthLabel = label,
                    fullMonthName = fullLabel,
                    yearMonthKey = key,
                    totalExpense = total,
                    categoryBreakdown = categoryMap
                )
            )
        }
        list
    }

    val maxVal = remember(monthlyData) {
        (monthlyData.maxOfOrNull { it.totalExpense } ?: 100.0).coerceAtLeast(100.0)
    }

    val averageExpense = remember(monthlyData) {
        if (monthlyData.isNotEmpty()) monthlyData.map { it.totalExpense }.average() else 0.0
    }

    val availableCategoryTags = remember(transactions, categories) {
        val tagsFromTx = transactions.filter { it.type == "DEBIT" }.map { it.categoryTag }.toSet()
        val tagsFromCat = categories.map { it.name }.toSet()
        (tagsFromTx + tagsFromCat).toList().sorted()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_spending_trend_chart"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedCategoryFilter != null) stringResource(R.string.chart_category_spending_trend_title_format, selectedCategoryFilter!!) else stringResource(R.string.chart_categorized_monthly_trends_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = stringResource(R.string.chart_trend_subtitle),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        IconButton(
                            onClick = { chartMode = "STACKED" },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (chartMode == "STACKED") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .testTag("trend_mode_stacked")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = stringResource(R.string.chart_stacked_bar_view_description),
                                tint = if (chartMode == "STACKED") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { chartMode = "TREND_LINE" },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (chartMode == "TREND_LINE") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .testTag("trend_mode_line")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = stringResource(R.string.chart_line_trend_view_description),
                                tint = if (chartMode == "TREND_LINE") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = selectedCategoryFilter == null,
                    onClick = { selectedCategoryFilter = null },
                    label = { Text(stringResource(R.string.chart_all_categories_label), style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp).testTag("trend_category_all")
                )

                availableCategoryTags.take(6).forEach { tag ->
                    val color = getCategoryColor(tag)
                    FilterChip(
                        selected = selectedCategoryFilter == tag,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == tag) null else tag
                        },
                        label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        },
                        modifier = Modifier.height(28.dp).testTag("trend_category_$tag")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.chart_six_month_avg_label),
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    )
                    Text(
                        text = stringResource(R.string.currency_amount_format, currencySymbol, averageExpense),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (monthlyData.size >= 2) {
                    val currentM = monthlyData.last().totalExpense
                    val prevM = monthlyData[monthlyData.size - 2].totalExpense
                    val diffPct = if (prevM > 0) ((currentM - prevM) / prevM) * 100 else 0.0
                    val isUp = diffPct > 0

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (isUp) ExpenseRed else com.example.ui.theme.IncomeGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.chart_percent_vs_last_month_format, Math.abs(diffPct)),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isUp) ExpenseRed else com.example.ui.theme.IncomeGreen
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            val primaryColor = MaterialTheme.colorScheme.primary

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val defaultLineColor = ExpenseRed // read here: theme colours cannot be read inside the draw block
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(monthlyData) {
                            detectTapGestures { offset ->
                                val monthWidth = size.width / monthlyData.size
                                val tappedIdx = (offset.x / monthWidth).toInt().coerceIn(0, monthlyData.size - 1)
                                selectedMonthIndex = if (selectedMonthIndex == tappedIdx) null else tappedIdx
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val monthCount = monthlyData.size
                    val colWidth = w / monthCount

                    for (i in 0..3) {
                        val y = h * (i / 3f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }

                    if (maxVal > 0) {
                        val avgY = h - ((averageExpense / maxVal) * h).toFloat()
                        drawLine(
                            color = primaryColor.copy(alpha = 0.4f),
                            start = Offset(0f, avgY),
                            end = Offset(w, avgY),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    if (chartMode == "STACKED") {
                        val barWidth = (colWidth * 0.45f).coerceAtLeast(16f)

                        monthlyData.forEachIndexed { idx, item ->
                            val xCenter = idx * colWidth + (colWidth / 2f)
                            val isSelected = selectedMonthIndex == idx

                            if (selectedCategoryFilter != null) {
                                val amount = item.totalExpense.coerceAtLeast(0.0)
                                val barHeight = ((amount / maxVal) * h).toFloat().coerceAtLeast(0f)
                                val catColor = getCategoryColor(selectedCategoryFilter!!)

                                if (barHeight > 0) {
                                    drawRoundRect(
                                        color = catColor,
                                        topLeft = Offset(xCenter - (barWidth / 2), h - barHeight),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = CornerRadius(8f, 8f)
                                    )
                                }
                            } else {
                                var currentY = h
                                val sortedCategories = item.categoryBreakdown.entries
                                    .filter { it.value > 0.0 }
                                    .sortedByDescending { it.value }

                                sortedCategories.forEach { (catName, amount) ->
                                    val segmentHeight = ((amount.coerceAtLeast(0.0) / maxVal) * h).toFloat().coerceAtLeast(0f)
                                    val catColor = getCategoryColor(catName)

                                    if (segmentHeight > 0) {
                                        drawRect(
                                            color = catColor,
                                            topLeft = Offset(xCenter - (barWidth / 2), currentY - segmentHeight),
                                            size = Size(barWidth, segmentHeight)
                                        )
                                        currentY -= segmentHeight
                                    }
                                }
                            }

                            if (isSelected) {
                                drawCircle(
                                    color = primaryColor,
                                    radius = 4.dp.toPx(),
                                    center = Offset(xCenter, 12.dp.toPx())
                                )
                            }
                        }
                    } else {
                        val points = monthlyData.mapIndexed { idx, item ->
                            val x = idx * colWidth + (colWidth / 2f)
                            val safeAmount = item.totalExpense.coerceAtLeast(0.0)
                            val y = h - ((safeAmount / maxVal) * h).toFloat()
                            Offset(x, y)
                        }

                        if (points.isNotEmpty()) {
                            val path = Path()
                            val fillPath = Path()

                            path.moveTo(points.first().x, points.first().y)
                            fillPath.moveTo(points.first().x, h)
                            fillPath.lineTo(points.first().x, points.first().y)

                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                val controlP1 = Offset(p1.x + (p2.x - p1.x) / 2, p1.y)
                                val controlP2 = Offset(p1.x + (p2.x - p1.x) / 2, p2.y)

                                path.cubicTo(controlP1.x, controlP1.y, controlP2.x, controlP2.y, p2.x, p2.y)
                                fillPath.cubicTo(controlP1.x, controlP1.y, controlP2.x, controlP2.y, p2.x, p2.y)
                            }

                            fillPath.lineTo(points.last().x, h)
                            fillPath.close()

                            val lineColor = if (selectedCategoryFilter != null) getCategoryColor(selectedCategoryFilter!!) else defaultLineColor

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0.02f)),
                                    startY = 0f,
                                    endY = h
                                )
                            )

                            drawPath(
                                path = path,
                                color = lineColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            points.forEachIndexed { idx, pt ->
                                val isSelected = selectedMonthIndex == idx
                                drawCircle(
                                    color = Color.White,
                                    radius = if (isSelected) 7.dp.toPx() else 4.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = lineColor,
                                    radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                                    center = pt
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                monthlyData.forEachIndexed { idx, item ->
                    val isSelected = selectedMonthIndex == idx
                    Text(
                        text = item.monthLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { selectedMonthIndex = if (selectedMonthIndex == idx) null else idx }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            selectedMonthIndex?.let { idx ->
                val monthInfo = monthlyData.getOrNull(idx)
                if (monthInfo != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = monthInfo.fullMonthName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = stringResource(R.string.chart_month_total_format, currencySymbol, monthInfo.totalExpense),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            if (monthInfo.categoryBreakdown.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.chart_top_spending_categories_label),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                monthInfo.categoryBreakdown.entries
                                    .sortedByDescending { it.value }
                                    .take(3)
                                    .forEach { (catName, amount) ->
                                        val color = getCategoryColor(catName)
                                        val pct = if (monthInfo.totalExpense > 0) (amount / monthInfo.totalExpense) * 100 else 0.0

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(color)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = catName,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                                )
                                            }

                                            Text(
                                                text = stringResource(R.string.chart_amount_with_percentage_format, currencySymbol, amount, pct),
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                            }
                        }
                    }
                }
            }
        }
    }
}

