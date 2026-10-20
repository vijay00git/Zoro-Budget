package com.example.ui.savings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.SavingsItemEntity
import com.example.data.savings.GoldForm
import com.example.data.savings.OtherKind
import com.example.data.savings.SavingsSection
import com.example.data.savings.SavingsStatus
import com.example.data.savings.SilverForm
import com.example.ui.components.formatMoney
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Each section keeps one colour everywhere it appears: header accent, record cards and charts. */
data class SectionStyle(@StringRes val title: Int, @StringRes val addFirst: Int, val icon: ImageVector, val color: Color)

fun SavingsSection.style(): SectionStyle = when (this) {
    SavingsSection.FD -> SectionStyle(R.string.savings_section_fd, R.string.savings_add_first_fd, Icons.Default.AccountBalance, Color(0xFF2563EB))
    SavingsSection.RD -> SectionStyle(R.string.savings_section_rd, R.string.savings_add_first_rd, Icons.Default.EventRepeat, Color(0xFF0D9488))
    SavingsSection.STOCK -> SectionStyle(R.string.savings_section_stock, R.string.savings_add_first_stock, Icons.Default.ShowChart, Color(0xFF7C3AED))
    SavingsSection.MUTUAL_FUND -> SectionStyle(R.string.savings_section_mf, R.string.savings_add_first_mf, Icons.Default.PieChart, Color(0xFFDB2777))
    SavingsSection.GOLD -> SectionStyle(R.string.savings_section_gold, R.string.savings_add_first_gold, Icons.Default.Diamond, Color(0xFFCA8A04))
    SavingsSection.SILVER -> SectionStyle(R.string.savings_section_silver, R.string.savings_add_first_silver, Icons.Default.Toll, Color(0xFF64748B))
    SavingsSection.LAND -> SectionStyle(R.string.savings_section_land, R.string.savings_add_first_land, Icons.Default.Landscape, Color(0xFF8D6E63))
    SavingsSection.OTHER -> SectionStyle(R.string.savings_section_other, R.string.savings_add_first_other, Icons.Default.Category, Color(0xFF0891B2))
}

/** The sections in the order the Savings page shows them. */
val availableSections = SavingsSection.entries

/** The kind of record within its section ("SGB", "PPF", "Coin"...), when it has one. */
@StringRes
fun subTypeLabel(item: SavingsItemEntity): Int? = when (SavingsSection.from(item.section)) {
    SavingsSection.OTHER -> OtherKind.entries.firstOrNull { it.name == item.subType }?.label()
    SavingsSection.GOLD -> GoldForm.entries.firstOrNull { it.name == item.subType }?.label()
    SavingsSection.SILVER -> SilverForm.entries.firstOrNull { it.name == item.subType }?.label()
    else -> null
}

@StringRes
fun SavingsStatus.label(): Int = when (this) {
    SavingsStatus.ACTIVE -> R.string.savings_status_active
    SavingsStatus.MATURED -> R.string.savings_status_matured
    SavingsStatus.CLOSED -> R.string.savings_status_closed
    SavingsStatus.SOLD -> R.string.savings_status_sold
}

private val dayFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

fun formatDay(millis: Long): String = dayFormat.format(millis)

fun formatPct(value: Double): String = String.format(Locale.getDefault(), "%.2f%%", value)

fun formatSignedMoney(currencySymbol: String, amount: Double): String =
    (if (amount >= 0) "+" else "−") + formatMoney(currencySymbol, kotlin.math.abs(amount))

fun formatPlain(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')

fun parseAmount(text: String): Double? = text.replace(",", "").trim().toDoubleOrNull()

/** Green for a gain, red for a loss; lighter shades on dark backgrounds so they stay readable. */
@Composable
fun gainColor(amount: Double): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when {
        amount >= 0 && dark -> Color(0xFF81C784)
        amount >= 0 -> IncomeGreen
        dark -> Color(0xFFEF9A9A)
        else -> ExpenseRed
    }
}

/** "+₹12,500.00 (+20.83%)" on a small pill, so it reads on the coloured summary cards too. */
@Composable
fun GainText(currencySymbol: String, gain: Double, gainPct: Double, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface, modifier = modifier.padding(top = 4.dp)) {
        Text(
            text = formatSignedMoney(currencySymbol, gain) + " (" + (if (gainPct >= 0) "+" else "") + formatPct(gainPct) + ")",
            color = gainColor(gain),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
        )
    }
}

/** A label on the left and a value on the right, for detail and calculation cards. */
@Composable
fun LabelValueRow(label: String, value: String, modifier: Modifier = Modifier, detail: String? = null, emphasize: Boolean = false, valueColor: Color? = null) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            text = value,
            textAlign = TextAlign.End,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            style = if (emphasize) MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
fun NumberField(value: String, onValueChange: (String) -> Unit, @StringRes label: Int, modifier: Modifier = Modifier, isError: Boolean = false, decimal: Boolean = true, suffix: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        isError = isError,
        suffix = suffix?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier.fillMaxWidth()
    )
}

/** A read-only field that opens the Material date picker; [millis] is a local start-of-day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(@StringRes label: Int, millis: Long?, onChange: (Long) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { open = true }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(millis?.let { formatDay(it) } ?: stringResource(R.string.savings_pick_date), style = MaterialTheme.typography.bodyLarge)
        }
    }
    if (open) {
        // The Material date picker works in UTC midnights; convert to and from local days.
        val state = rememberDatePickerState(initialSelectedDateMillis = localDayToUtc(millis ?: System.currentTimeMillis()))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(utcDayToLocal(it)) }
                    open = false
                }) { Text(stringResource(R.string.savings_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.savings_cancel)) } }
        ) { DatePicker(state = state) }
    }
}

private fun localDayToUtc(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun utcDayToLocal(utcMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
