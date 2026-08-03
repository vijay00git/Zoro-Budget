package com.example.ui.dialogs
import com.example.R

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.ui.components.ScrollEdgeFade
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Cycles through every pending SMS transaction one at a time so the user can triage the whole
 * inbox backlog in one sitting, instead of opening the full EditTransactionDialog per row from
 * the Ledger tab. "Confirm" applies the title/category and clears isPendingReview; "Decline"
 * soft-dismisses SMS-sourced rows (same path as swipe-to-delete) so they don't reappear on the
 * next inbox scan; "Skip" leaves the row untouched for later.
 */
@Composable
fun QuickReviewPendingDialog(
    pendingTransactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onConfirm: (TransactionEntity, title: String, categoryTag: String) -> Unit,
    onDecline: (TransactionEntity) -> Unit
) {
    var index by remember { mutableIntStateOf(0) }

    if (index >= pendingTransactions.size) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val current = pendingTransactions[index]
    var titleText by remember(current.id) { mutableStateOf(current.merchantOrSender ?: current.title) }
    var selectedCategory by remember(current.id) { mutableStateOf(current.categoryTag) }
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val categoryChipsListState = rememberLazyListState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.FactCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.quick_review_dialog_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = stringResource(R.string.quick_review_dialog_progress, index + 1, pendingTransactions.size),
                        style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (index + 1f) / pendingTransactions.size },
                    modifier = Modifier.fillMaxWidth().height(4.dp).testTag("quick_review_progress_bar")
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(
                                R.string.quick_review_dialog_amount,
                                if (current.type == "CREDIT") "+" else "-",
                                currencySymbol,
                                current.amount
                            ),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (current.type == "CREDIT") IncomeGreen else ExpenseRed
                            )
                        )
                        current.merchantOrSender?.let { sender ->
                            Text(
                                text = sender,
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Text(
                            text = dateFormat.format(current.dateTimestamp),
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text(stringResource(R.string.quick_review_dialog_title_field_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quick_review_title_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.quick_review_dialog_category_label),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    LazyRow(
                        state = categoryChipsListState,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = selectedCategory == category.name
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = category.name },
                                label = {
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(32.dp).testTag("quick_review_category_chip_${category.name}")
                            )
                        }
                    }
                    ScrollEdgeFade(categoryChipsListState, modifier = Modifier.matchParentSize(), edgeColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedButton(
                    onClick = {
                        onDecline(current)
                        index++
                    },
                    modifier = Modifier.fillMaxWidth().testTag("quick_review_decline_button"),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.height(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.quick_review_dialog_decline_button))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(current, titleText, selectedCategory)
                    index++
                },
                modifier = Modifier.testTag("quick_review_confirm_button")
            ) {
                Text(stringResource(R.string.quick_review_dialog_confirm_button))
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { index++ }, modifier = Modifier.testTag("quick_review_skip_button")) {
                    Text(stringResource(R.string.quick_review_dialog_skip_button))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.quick_review_dialog_close_button))
                }
            }
        }
    )
}
