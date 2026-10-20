package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.CategoryEntity
import com.example.ui.theme.accentOf
import java.util.Locale

/** Whole rupees for tiles: "₹7,982" (paise only add noise at this size). */
private fun shortMoney(currencySymbol: String, amount: Double): String =
    formatMoney(currencySymbol, amount).substringBeforeLast('.')

private fun signedShort(currencySymbol: String, amount: Double): String =
    (if (amount < 0) "−" else "+") + shortMoney(currencySymbol, kotlin.math.abs(amount))

/** Green while comfortably inside a limit, amber close to it, red past it. */
@Composable
private fun limitColor(ratio: Double, amberFrom: Double): Color = when {
    ratio > 1.0 -> accentOf("#991B1B").ink
    ratio > amberFrom -> accentOf("#854D0E").ink
    else -> accentOf("#166534").ink
}

/** The month's cash flow on the indigo hero tile: net, income, expenses and the share kept. */
@Composable
fun CashFlowHeroTile(income: Double, expense: Double, currencySymbol: String, modifier: Modifier = Modifier) {
    val net = income - expense
    val keptPct = if (income > 0) (net / income * 100).coerceIn(-999.0, 100.0) else null
    val white = Color.White
    Surface(modifier = modifier.fillMaxWidth().testTag("cash_flow_tile"), shape = RoundedCornerShape(28.dp), color = heroColor(), contentColor = white) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.planning_cash_flow), style = MaterialTheme.typography.labelLarge, color = white.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
                if (keptPct != null) {
                    Surface(shape = RoundedCornerShape(12.dp), color = white.copy(alpha = 0.16f)) {
                        Text(
                            if (keptPct >= 0) {
                                stringResource(R.string.planning_kept_pct, String.format(Locale.getDefault(), "%.0f", keptPct))
                            } else {
                                stringResource(R.string.planning_spent_more)
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Text(signedShort(currencySymbol, net), style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroFigure(stringResource(R.string.planning_money_in), "+" + shortMoney(currencySymbol, income), Modifier.weight(1f))
                HeroFigure(stringResource(R.string.planning_money_out), "−" + shortMoney(currencySymbol, expense), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroFigure(label: String, value: String, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.12f)) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), maxLines = 1)
        }
    }
}

/** All category budgets together as a ring: how much of the month's plan is used. */
@Composable
fun BudgetRingTile(spent: Double, limit: Double, currencySymbol: String, onSetBudget: () -> Unit, modifier: Modifier = Modifier) {
    val ratio = if (limit > 0) spent / limit else 0.0
    val ringColor = limitColor(ratio, 0.85)
    val track = MaterialTheme.colorScheme.outlineVariant
    BentoTile(modifier = modifier.testTag("budget_ring_tile"), onClick = onSetBudget, contentPadding = 14) {
        Text(stringResource(R.string.planning_budget), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        if (limit <= 0) {
            Text(stringResource(R.string.planning_no_budget), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@BentoTile
        }
        val percent = (ratio * 100).toInt()
        Box(
            modifier = Modifier.align(Alignment.CenterHorizontally).size(108.dp).semantics { contentDescription = "$percent%" },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 12.dp.toPx()
                val inset = stroke / 2
                val arc = Size(size.width - stroke, size.height - stroke)
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                drawArc(ringColor, -90f, 360f * ratio.toFloat().coerceIn(0f, 1f), false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$percent%", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = ringColor)
                Text(stringResource(R.string.planning_used), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            stringResource(R.string.planning_of, shortMoney(currencySymbol, spent), shortMoney(currencySymbol, limit)),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            maxLines = 1
        )
        val left = limit - spent
        Text(
            if (left >= 0) stringResource(R.string.planning_left, shortMoney(currencySymbol, left)) else stringResource(R.string.planning_over, shortMoney(currencySymbol, -left)),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = ringColor
        )
    }
}

/** This month's savings goal: what was kept against the target. Tapping changes the target. */
@Composable
fun SavingsGoalTile(saved: Double, target: Double, currencySymbol: String, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val accent = accentOf("#166534")
    val progress = if (target > 0) (saved / target).coerceIn(0.0, 1.0) else 0.0
    BentoTile(modifier = modifier.testTag("savings_goal_tile"), onClick = onEdit, contentPadding = 14) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Savings, contentDescription = null, tint = accent.ink, modifier = Modifier.size(24.dp))
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.planning_edit_goal), tint = accent.ink, modifier = Modifier.size(18.dp))
        }
        Text(stringResource(R.string.planning_savings_goal), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        Text(
            "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = accent.ink
        )
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surface)) {
            Box(modifier = Modifier.fillMaxWidth(progress.toFloat()).height(8.dp).clip(RoundedCornerShape(4.dp)).background(accent.ink))
        }
        Text(
            stringResource(R.string.planning_of, shortMoney(currencySymbol, saved.coerceAtLeast(0.0)), shortMoney(currencySymbol, target)),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            when {
                target <= 0 -> stringResource(R.string.planning_goal_set)
                saved >= target -> stringResource(R.string.planning_goal_reached)
                saved > 0 -> stringResource(R.string.planning_goal_to_go, shortMoney(currencySymbol, target - saved))
                else -> stringResource(R.string.planning_goal_spending_more)
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** A small count tile that filters the category grid; highlighted while its filter is on. */
@Composable
fun CountTile(count: Int, label: String, hex: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = accentOf(hex)
    BentoTile(modifier = modifier, outline = if (selected) accent.ink else null, onClick = onClick, contentPadding = 12) {
        Text(count.toString(), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), color = accent.ink)
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 2)
    }
}

/** One category as a tile in its own colour: spent, the limit bar and what is left or over. */
@Composable
fun CategoryBudgetTile(category: CategoryEntity, spent: Double, currencySymbol: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = accentOf(category.colorHex)
    val limit = category.monthlyBudgetLimit
    val ratio = if (limit > 0) spent / limit else 0.0
    val status = limitColor(ratio, 0.8)
    BentoTile(modifier = modifier.testTag("category_tile_${category.name}"), outline = if (ratio > 1.0) status else null, onClick = onClick, contentPadding = 12) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(category.iconName, category.name, category.colorHex, size = 36.dp)
            Spacer(Modifier.width(8.dp))
            Text(category.name, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text(shortMoney(currencySymbol, spent), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), maxLines = 1)
        if (limit > 0) {
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surface)) {
                Box(modifier = Modifier.fillMaxWidth(ratio.toFloat().coerceIn(0f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp)).background(status))
            }
            val left = limit - spent
            Text(
                if (left >= 0) {
                    stringResource(R.string.planning_left_of, shortMoney(currencySymbol, left), shortMoney(currencySymbol, limit))
                } else {
                    stringResource(R.string.planning_over, shortMoney(currencySymbol, -left))
                },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (ratio > 0.8) status else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(stringResource(R.string.planning_no_limit), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun NewCategoryTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    BentoTile(modifier = modifier.testTag("add_custom_category_button"), onClick = onClick, contentPadding = 12) {
        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Text(stringResource(R.string.planning_new_category), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
    }
}
