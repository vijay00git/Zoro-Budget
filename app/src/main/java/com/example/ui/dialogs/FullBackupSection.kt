package com.example.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.backup.BackupManager
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Full backup to a ZIP file the user saves anywhere (Drive, Downloads, a PC), and restore from
 * one. Restore replaces all data, keeps a safety copy of the current data first, then restarts.
 */
@Composable
fun FullBackupSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<Triple<Uri, BackupManager.BackupInfo, BackupManager.Counts>?>(null) }

    val savedText = stringResource(R.string.full_backup_saved)
    val failedText = stringResource(R.string.full_backup_failed)

    val createLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        message = null
        scope.launch {
            message = try {
                val info = withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(context)
                    context.contentResolver.openOutputStream(uri)?.use { BackupManager.createBackup(context, db, it) }
                        ?: throw BackupManager.BackupException(failedText)
                }
                String.format(Locale.getDefault(), savedText, info.counts.transactions, info.counts.savingsItems, info.counts.attachments)
            } catch (e: Exception) {
                e.message ?: failedText
            }
            busy = false
        }
    }

    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        message = null
        scope.launch {
            try {
                val (info, current) = withContext(Dispatchers.IO) {
                    val info = context.contentResolver.openInputStream(uri)?.use { BackupManager.readInfo(it) }
                        ?: throw BackupManager.BackupException(failedText)
                    info to BackupManager.currentCounts(context, AppDatabase.getDatabase(context))
                }
                pendingRestore = Triple(uri, info, current)
            } catch (e: Exception) {
                message = e.message ?: failedText
            }
            busy = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("full_backup_section"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.full_backup_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(stringResource(R.string.full_backup_description), style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = { createLauncher.launch(BackupManager.suggestedFileName()) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().testTag("full_backup_save")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.full_backup_save))
            }
            OutlinedButton(
                onClick = { openLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().testTag("full_backup_restore")
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.full_backup_restore))
            }
            if (busy) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.full_backup_working), style = MaterialTheme.typography.bodySmall)
                }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)) }
        }
    }

    pendingRestore?.let { (uri, info, current) ->
        val date = remember(info.createdAt) { SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(info.createdAt) }
        AlertDialog(
            onDismissRequest = { if (!busy) pendingRestore = null },
            title = { Text(stringResource(R.string.full_backup_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.full_backup_confirm_date, date))
                    Text(
                        stringResource(
                            R.string.full_backup_confirm_compare,
                            info.counts.transactions, current.transactions,
                            info.counts.accounts, current.accounts,
                            info.counts.bills, current.bills,
                            info.counts.savingsItems, current.savingsItems,
                            info.counts.attachments, current.attachments
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(stringResource(R.string.full_backup_confirm_warning), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                    if (busy) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    BackupManager.saveSafetyCopy(context, AppDatabase.getDatabase(context))
                                    context.contentResolver.openInputStream(uri)?.use { BackupManager.restore(context, it) }
                                        ?: throw BackupManager.BackupException(failedText)
                                }
                                BackupManager.restartApp(context)
                            } catch (e: Exception) {
                                message = e.message ?: failedText
                                busy = false
                                pendingRestore = null
                            }
                        }
                    },
                    modifier = Modifier.testTag("full_backup_confirm_restore")
                ) { Text(stringResource(R.string.full_backup_confirm_button), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { pendingRestore = null }) { Text(stringResource(R.string.full_backup_cancel)) } }
        )
    }
}
