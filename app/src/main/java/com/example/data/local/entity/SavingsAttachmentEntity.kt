package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A PDF or photo kept with a savings record. The file is copied into the app's own storage;
 * [relativePath] is relative to filesDir, so it survives a reboot and travels with a backup.
 */
@Entity(
    tableName = "savings_attachments",
    foreignKeys = [
        ForeignKey(entity = SavingsItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("itemId")]
)
data class SavingsAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val fileName: String,
    val mimeType: String,
    val relativePath: String,
    val sizeBytes: Long,
    val label: String? = null, // "FD receipt", "Sale deed"...
    val docTag: String? = null, // property checklist tag: SALE_DEED, PATTA, EC...
    val addedAt: Long = System.currentTimeMillis()
)
