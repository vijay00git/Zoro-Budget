package com.example.ui.components
import com.example.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenContainer
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PendingAmberContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    account: AccountEntity?,
    categories: List<CategoryEntity> = emptyList(),
    onClick: () -> Unit,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier
) {
    val isCredit = transaction.type == "CREDIT"
    val accentColor = if (isCredit) IncomeGreen else ExpenseRed
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(transaction.dateTimestamp))

    val cardContainerColor = if (transaction.isPendingReview) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (transaction.isPendingReview) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Transaction Icon Container
            val isTransfer = transaction.categoryTag == "Transfer" || transaction.title.startsWith("Transfer", ignoreCase = true)
            val iconBg = when {
                transaction.isPendingReview -> MaterialTheme.colorScheme.surface
                isTransfer -> MaterialTheme.colorScheme.tertiaryContainer
                isCredit -> IncomeGreenContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
            val iconTint = when {
                transaction.isPendingReview -> MaterialTheme.colorScheme.onSurfaceVariant
                isTransfer -> MaterialTheme.colorScheme.onTertiaryContainer
                isCredit -> IncomeGreen
                else -> MaterialTheme.colorScheme.onPrimaryContainer
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isTransfer -> Icons.Default.SwapHoriz
                        isCredit -> Icons.Default.Add
                        else -> Icons.Default.Remove
                    },
                    contentDescription = transaction.type,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (transaction.isPendingReview) FontWeight.Bold else FontWeight.Bold,
                        fontStyle = if (transaction.isPendingReview) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                        color = if (transaction.isPendingReview) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category Tag Badge Chip - color sourced from the category's own colorHex
                    // (same lookup Charts.kt uses) instead of a separate keyword table, so a
                    // category is the same color everywhere in the app.
                    val categoryColor = categories.find { it.name.equals(transaction.categoryTag, ignoreCase = true) }
                        ?.colorHex
                        ?.let { hex -> try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { null } }
                        ?: MaterialTheme.colorScheme.primary
                    val categoryContainerColor = categoryColor.copy(alpha = 0.16f)
                    Surface(
                        color = if (transaction.isPendingReview) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else categoryContainerColor,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = transaction.categoryTag,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (transaction.isPendingReview) MaterialTheme.colorScheme.onSurfaceVariant else categoryColor
                            )
                        )
                    }

                    // Account Tag
                    if (account != null) {
                        Text(
                            text = stringResource(R.string.transaction_account_tag_format, account.name),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        )
                    }

                    // Receipt Attached Badge
                    if (!transaction.receiptUri.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = stringResource(R.string.transaction_receipt_attached_description),
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = stringResource(R.string.transaction_receipt_label),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            }
                        }
                    }

                    // SMS Badge
                    if (transaction.source == "SMS_AUTO") {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = stringResource(R.string.transaction_auto_sms_description),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Amount and Date / Pending Tag
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = if (isCredit) stringResource(R.string.transaction_amount_credit_format, currencySymbol, transaction.amount) else stringResource(R.string.transaction_amount_debit_format, currencySymbol, transaction.amount),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                if (transaction.isPendingReview) {
                    Surface(
                        color = PendingAmberContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.transaction_pending_label),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = PendingAmber,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                } else {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }
    }
}
