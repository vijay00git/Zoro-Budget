package com.example.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Start (inclusive) and end (exclusive) epoch millis of the month [offset] months from now. */
fun monthWindow(offset: Int): Pair<Long, Long> {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, offset)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val start = cal.timeInMillis
    cal.add(Calendar.MONTH, 1)
    return start to cal.timeInMillis
}

/**
 * "‹ October 2026 ›" picker for screens that show one month at a time. Can't go past the
 * current month; a "This month" chip jumps back when an earlier month is selected.
 */
@Composable
fun MonthSelector(
    monthOffset: Int,
    onMonthOffsetChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val title = remember(monthOffset) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthWindow(monthOffset).first)
    }

    Surface(
        modifier = modifier.fillMaxWidth().testTag("month_selector"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onMonthOffsetChange(monthOffset - 1) },
                modifier = Modifier.testTag("month_selector_previous")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.month_selector_previous)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f).testTag("month_selector_title")
            )
            if (monthOffset != 0) {
                AssistChip(
                    onClick = { onMonthOffsetChange(0) },
                    label = { Text(stringResource(R.string.month_selector_this_month)) },
                    modifier = Modifier.testTag("month_selector_reset")
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            IconButton(
                onClick = { onMonthOffsetChange(monthOffset + 1) },
                enabled = monthOffset < 0,
                modifier = Modifier.testTag("month_selector_next")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.month_selector_next)
                )
            }
        }
    }
}
