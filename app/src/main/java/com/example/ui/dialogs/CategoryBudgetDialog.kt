package com.example.ui.dialogs
import com.example.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.local.entity.CategoryEntity
import com.example.ui.components.AppearancePicker
import com.example.ui.components.CategoryAvatar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditCategoryBudgetDialog(
    category: CategoryEntity,
    onDismiss: () -> Unit,
    onSave: (limit: Double, iconName: String, colorHex: String) -> Unit,
    onDeleteCategory: (() -> Unit)? = null,
    currencySymbol: String = "$"
) {
    var budgetInput by remember { mutableStateOf(if (category.monthlyBudgetLimit > 0) category.monthlyBudgetLimit.toString() else "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val presets = listOf(100, 250, 500, 750, 1000)
    var iconName by remember { mutableStateOf(category.iconName) }
    var colorHex by remember { mutableStateOf(category.colorHex) }
    var showAppearance by remember { mutableStateOf(false) }

    val catColor = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryAvatar(iconName = iconName, categoryName = category.name, colorHex = colorHex, size = 36.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.category_budget_dialog_title, category.name),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (onDeleteCategory != null) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_category_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.category_budget_dialog_delete_content_desc),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.category_budget_dialog_description),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text(stringResource(R.string.category_budget_dialog_limit_label, currencySymbol)) },
                    prefix = { Text(stringResource(R.string.category_budget_dialog_currency_prefix, currencySymbol)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_budget_limit_input")
                )

                Text(
                    text = stringResource(R.string.category_budget_dialog_quick_select),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { budgetInput = preset.toString() },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.category_budget_dialog_preset_amount, currencySymbol, preset),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                TextButton(onClick = { showAppearance = !showAppearance }, modifier = Modifier.testTag("category_appearance_toggle")) {
                    Text(stringResource(if (showAppearance) R.string.appearance_hide else R.string.appearance_change))
                }
                if (showAppearance) {
                    AppearancePicker(
                        name = category.name,
                        iconName = iconName,
                        colorHex = colorHex,
                        onIconChange = { iconName = it },
                        onColorChange = { colorHex = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = budgetInput.toDoubleOrNull() ?: 0.0
                    onSave(limit, iconName, colorHex)
                },
                modifier = Modifier.testTag("save_category_budget_button")
            ) {
                Text(stringResource(R.string.category_budget_dialog_save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.category_budget_dialog_cancel_button))
            }
        }
    )

    if (showDeleteConfirm && onDeleteCategory != null) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.confirm_delete_category_title),
            message = stringResource(R.string.confirm_delete_category_message, category.name),
            onConfirm = {
                showDeleteConfirm = false
                onDeleteCategory()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
