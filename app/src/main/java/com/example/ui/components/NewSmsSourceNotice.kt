package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.TransactionEntity

/**
 * Shown while reviewing an SMS transaction from a bank, sender or card that no account is linked
 * to yet. Confirming the account teaches it this sender, so the hint only appears once per source.
 */
@Composable
fun NewSmsSourceNotice(transaction: TransactionEntity, modifier: Modifier = Modifier) {
    val source = listOfNotNull(
        transaction.smsSender,
        transaction.smsAccountLast4?.let { stringResource(R.string.new_sms_source_last4, it) }
    ).joinToString(" · ")

    Card(
        modifier = modifier.fillMaxWidth().testTag("new_sms_source_notice"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(R.string.new_sms_source_message, source),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
