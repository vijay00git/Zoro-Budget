package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.accentOf

/**
 * Two letters for an account's badge: the start of a short all-caps bank name ("HDFC Bank" ->
 * "HD"), otherwise the first letters of the first two words ("Indian Bank Savings" -> "IB").
 */
fun accountInitials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return "?"
    val first = words.first()
    return when {
        first.length in 2..6 && first.all { it.isUpperCase() || it.isDigit() } -> first.take(2)
        words.size >= 2 -> "${first.first()}${words[1].first()}".uppercase()
        else -> first.take(2).uppercase()
    }
}

/** A rounded square in the account's colour: initials for banks, an icon for cash and cards. */
@Composable
fun AccountBadge(account: AccountEntity, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val accent = accentOf(account.colorHex)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.34f))
            .background(accent.solid),
        contentAlignment = Alignment.Center
    ) {
        when (account.type) {
            "CASH", "WALLET" -> Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
            "CREDIT_CARD" -> Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
            else -> Text(
                text = accountInitials(account.name),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.36f).sp
            )
        }
    }
}

/** The category's icon in a soft circle of its colour, with an optional dot for the account. */
@Composable
fun CategoryAvatar(
    iconName: String?,
    categoryName: String,
    colorHex: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    accountColorHex: String? = null,
    muted: Boolean = false
) {
    val accent = accentOf(colorHex)
    val ring = LocalIsDarkTheme.current.let { if (it) Color(0xFF1A1C21) else Color.White }
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(50))
                .background(if (muted) MaterialTheme.colorScheme.surfaceVariant else accent.fill),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIconFor(iconName ?: "Label", categoryName),
                contentDescription = null,
                tint = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else accent.ink,
                modifier = Modifier.size(size * 0.5f)
            )
        }
        if (accountColorHex != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.36f)
                    .clip(RoundedCornerShape(50))
                    .background(ring),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(size * 0.26f)
                        .clip(RoundedCornerShape(50))
                        .background(accentOf(accountColorHex).solid)
                )
            }
        }
    }
}
