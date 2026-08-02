package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ExportImportBackupDialog(
    onDismiss: () -> Unit,
    onExportCsv: suspend () -> String,
    onExportJson: suspend () -> String,
    onImportJson: (jsonString: String) -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Export CSV, 1 = Export JSON, 2 = Restore Backup
    var exportedText by remember { mutableStateOf("") }
    var importInputText by remember { mutableStateOf("") }
    var copiedStatusMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Data Backup & Export",
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
                // Tab Selection Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Export CSV", "Export JSON", "Restore JSON").forEachIndexed { index, title ->
                        val isSelected = activeTab == index
                        Surface(
                            onClick = {
                                activeTab = index
                                copiedStatusMessage = null
                                if (index == 0) {
                                    scope.launch { exportedText = onExportCsv() }
                                } else if (index == 1) {
                                    scope.launch { exportedText = onExportJson() }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = title,
                                modifier = Modifier.padding(vertical = 8.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                if (activeTab == 0 || activeTab == 1) {
                    if (exportedText.isEmpty()) {
                        scope.launch {
                            exportedText = if (activeTab == 0) onExportCsv() else onExportJson()
                        }
                    }

                    Text(
                        text = if (activeTab == 0) "CSV Financial Ledger Export:" else "Complete Database JSON Backup:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    OutlinedTextField(
                        value = exportedText,
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 10,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_text_output")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Backup Export", exportedText)
                                clipboard.setPrimaryClip(clip)
                                copiedStatusMessage = "Copied export content to clipboard!"
                            },
                            modifier = Modifier.testTag("copy_export_button")
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy to Clipboard")
                        }
                    }

                    copiedStatusMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                } else {
                    Text(
                        text = "Paste JSON Backup Content:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = { importInputText = it },
                        placeholder = { Text("Paste JSON backup data here...") },
                        minLines = 6,
                        maxLines = 10,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_text_input")
                    )

                    Button(
                        onClick = {
                            if (importInputText.isNotBlank()) {
                                onImportJson(importInputText)
                                onDismiss()
                            }
                        },
                        enabled = importInputText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_restore_backup_button")
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore Database Backup")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
