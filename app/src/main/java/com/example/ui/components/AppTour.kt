package com.example.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R

/** One page of the welcome tour; [tabIndex] is the bottom-bar tab shown behind it. */
data class TourStep(
    @StringRes val title: Int,
    @StringRes val body: Int,
    val icon: ImageVector,
    val tabIndex: Int
)

val TOUR_STEPS = listOf(
    TourStep(R.string.tour_welcome_title, R.string.tour_welcome_body, Icons.Default.WavingHand, 0),
    TourStep(R.string.tour_overview_title, R.string.tour_overview_body, Icons.Default.Home, 0),
    TourStep(R.string.tour_ledger_title, R.string.tour_ledger_body, Icons.Default.ReceiptLong, 1),
    TourStep(R.string.tour_planning_title, R.string.tour_planning_body, Icons.Default.BarChart, 2),
    TourStep(R.string.tour_savings_title, R.string.tour_savings_body, Icons.Default.Savings, 3),
    TourStep(R.string.tour_settings_title, R.string.tour_settings_body, Icons.Default.Settings, 4),
    TourStep(R.string.tour_finish_title, R.string.tour_finish_body, Icons.Default.CleaningServices, 0)
)

/**
 * The welcome tour: a card over a dimmed screen, one tab per step. The last step always offers
 * to clear the demo data before the tour ends ("Skip tour" jumps there rather than closing).
 */
@Composable
fun AppTourOverlay(
    stepIndex: Int,
    hasDemoData: Boolean,
    onStepChange: (Int) -> Unit,
    onFinish: (clearDemo: Boolean) -> Unit
) {
    val step = TOUR_STEPS[stepIndex]
    val isLast = stepIndex == TOUR_STEPS.lastIndex
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            // Swallows taps so the screen behind can't be used half-way through the tour.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .testTag("app_tour"),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(heroColor().copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(step.icon, contentDescription = null, tint = heroColor())
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(R.string.tour_step_count, stepIndex + 1, TOUR_STEPS.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(stringResource(step.title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                val body = if (isLast && !hasDemoData) R.string.tour_finish_body_no_demo else step.body
                Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                StepDots(current = stepIndex)
                if (isLast) {
                    FinishButtons(hasDemoData = hasDemoData, onFinish = onFinish)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onStepChange(TOUR_STEPS.lastIndex) }) { Text(stringResource(R.string.tour_skip)) }
                        Spacer(Modifier.weight(1f))
                        if (stepIndex > 0) {
                            TextButton(onClick = { onStepChange(stepIndex - 1) }) { Text(stringResource(R.string.tour_back)) }
                        }
                        Button(
                            onClick = { onStepChange(stepIndex + 1) },
                            colors = ButtonDefaults.buttonColors(containerColor = heroColor()),
                            modifier = Modifier.testTag("tour_next")
                        ) {
                            Text(stringResource(if (stepIndex == 0) R.string.tour_start else R.string.tour_next))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinishButtons(hasDemoData: Boolean, onFinish: (Boolean) -> Unit) {
    if (!hasDemoData) {
        Button(
            onClick = { onFinish(false) },
            colors = ButtonDefaults.buttonColors(containerColor = heroColor()),
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.tour_done)) }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { onFinish(true) },
            colors = ButtonDefaults.buttonColors(containerColor = heroColor()),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tour_clear_demo")
        ) { Text(stringResource(R.string.tour_clear_demo)) }
        OutlinedButton(
            onClick = { onFinish(false) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tour_keep_demo")
        ) { Text(stringResource(R.string.tour_keep_demo)) }
    }
}

@Composable
private fun StepDots(current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TOUR_STEPS.indices.forEach { i ->
            Box(
                modifier = Modifier
                    .size(if (i == current) 8.dp else 6.dp)
                    .background(
                        if (i == current) heroColor() else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape
                    )
            )
        }
    }
}
