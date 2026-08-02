package com.example.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
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
                    // Category Tag Badge Chip with custom category colors
                    val categoryColorPair = when (transaction.categoryTag.lowercase()) {
                        "food", "dining", "restaurants", "groceries" -> Color(0xFFFF9800) to Color(0xFFFFF3E0)
                        "shopping", "clothing", "electronics" -> Color(0xFF9C27B0) to Color(0xFFF3E5F5)
                        "bills", "utilities", "rent" -> Color(0xFFE53935) to Color(0xFFFFEBEE)
                        "salary", "income", "freelance" -> Color(0xFF4CAF50) to Color(0xE8E8F5E9)
                        "transfer" -> Color(0xFF1E88E5) to Color(0xFFE3F2FD)
                        "health", "medical", "pharmacy" -> Color(0xFF009688) to Color(0xFFE0F2F1)
                        "entertainment", "movies", "games" -> Color(0xFFE91E63) to Color(0xFFFCE4EC)
                        "transport", "fuel", "cab", "travel" -> Color(0xFF3F51B5) to Color(0xFFE8EAF6)
                        else -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
                    }
                    Surface(
                        color = if (transaction.isPendingReview) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else categoryColorPair.second,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = transaction.categoryTag,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (transaction.isPendingReview) MaterialTheme.colorScheme.onSurfaceVariant else categoryColorPair.first
                            )
                        )
                    }

                    // Account Tag
                    if (account != null) {
                        Text(
                            text = "• ${account.name}",
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
                                    contentDescription = "Bill Attached",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "Receipt",
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
                            contentDescription = "Auto SMS",
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
                    text = "${if (isCredit) "+" else "-"}${String.format(Locale.getDefault(), "%s%,.2f", currencySymbol, transaction.amount)}",
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
                            text = "PENDING",
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
