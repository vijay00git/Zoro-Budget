package com.example.ui.dialogs
import com.example.R
import com.example.ui.components.AppearancePicker
import com.example.ui.components.suggestedIconName
import com.example.ui.theme.AccentPalette

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, iconName: String, colorHex: String, budgetLimit: Double) -> Unit,
    currencySymbol: String = "$"
) {
    var name by remember { mutableStateOf("") }
    var budgetInput by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf(AccentPalette.first()) }
    var iconName by remember { mutableStateOf("Label") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = stringResource(R.string.add_category_dialog_title_content_desc),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.add_category_dialog_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.add_category_dialog_name_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_input")
                )

                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text(stringResource(R.string.add_category_dialog_budget_label, currencySymbol)) },
                    prefix = { Text(stringResource(R.string.add_category_dialog_currency_prefix)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_budget_input")
                )

                AppearancePicker(
                    name = name,
                    iconName = iconName,
                    colorHex = selectedColorHex,
                    onIconChange = { iconName = it },
                    onColorChange = { selectedColorHex = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val limit = budgetInput.toDoubleOrNull() ?: 0.0
                        // Not picked: use the icon the name suggests, so the category is never a bare tag.
                        val icon = if (iconName == "Label") suggestedIconName(name) ?: "Label" else iconName
                        onAdd(name.trim(), icon, selectedColorHex, limit)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("submit_add_category_button")
            ) {
                Text(stringResource(R.string.add_category_dialog_create_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.add_category_dialog_cancel_button))
            }
        }
    )
}
