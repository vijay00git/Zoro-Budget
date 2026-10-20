package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.SavingsAttachmentEntity
import com.example.data.local.entity.SavingsEntryEntity
import com.example.data.local.entity.SavingsItemEntity
import com.example.data.local.entity.SavingsSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsDao {
    // Items
    @Query("SELECT * FROM savings_items ORDER BY section, startDate DESC")
    fun getAllItems(): Flow<List<SavingsItemEntity>>

    @Query("SELECT * FROM savings_items")
    suspend fun getAllItemsList(): List<SavingsItemEntity>

    @Query("SELECT * FROM savings_items WHERE id = :id")
    suspend fun getItemById(id: Long): SavingsItemEntity?

    @Insert
    suspend fun insertItem(item: SavingsItemEntity): Long

    @Update
    suspend fun updateItem(item: SavingsItemEntity)

    @Delete
    suspend fun deleteItem(item: SavingsItemEntity)

    @Query("SELECT DISTINCT holder FROM savings_items ORDER BY holder COLLATE NOCASE")
    fun getHolders(): Flow<List<String>>

    @Query("SELECT DISTINCT holder FROM savings_items")
    suspend fun getHoldersList(): List<String>

    @Query("UPDATE savings_items SET holder = :newName WHERE holder = :oldName COLLATE NOCASE")
    suspend fun renameHolder(oldName: String, newName: String)

    // Investment history
    @Query("SELECT * FROM savings_entries ORDER BY date DESC, id DESC")
    fun getAllEntries(): Flow<List<SavingsEntryEntity>>

    @Query("SELECT * FROM savings_entries WHERE itemId = :itemId ORDER BY date DESC, id DESC")
    fun getEntriesForItem(itemId: Long): Flow<List<SavingsEntryEntity>>

    @Query("SELECT * FROM savings_entries WHERE itemId = :itemId ORDER BY date ASC, id ASC")
    suspend fun getEntriesForItemList(itemId: Long): List<SavingsEntryEntity>

    @Insert
    suspend fun insertEntry(entry: SavingsEntryEntity): Long

    @Insert
    suspend fun insertEntries(entries: List<SavingsEntryEntity>)

    @Update
    suspend fun updateEntry(entry: SavingsEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: SavingsEntryEntity)

    // Attachments
    @Query("SELECT * FROM savings_attachments ORDER BY addedAt DESC")
    fun getAllAttachments(): Flow<List<SavingsAttachmentEntity>>

    @Query("SELECT * FROM savings_attachments WHERE itemId = :itemId ORDER BY addedAt DESC")
    fun getAttachmentsForItem(itemId: Long): Flow<List<SavingsAttachmentEntity>>

    @Query("SELECT * FROM savings_attachments WHERE itemId = :itemId")
    suspend fun getAttachmentsForItemList(itemId: Long): List<SavingsAttachmentEntity>

    @Insert
    suspend fun insertAttachment(attachment: SavingsAttachmentEntity): Long

    @Delete
    suspend fun deleteAttachment(attachment: SavingsAttachmentEntity)

    // Snapshots (value history and shared gold/silver rates)
    @Query("SELECT * FROM savings_snapshots ORDER BY date ASC, id ASC")
    fun getAllSnapshots(): Flow<List<SavingsSnapshotEntity>>

    @Query("SELECT * FROM savings_snapshots WHERE itemId IS NULL AND `key` = :key ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun getLatestRate(key: String): SavingsSnapshotEntity?

    @Query("SELECT * FROM savings_snapshots WHERE itemId = :itemId")
    suspend fun getSnapshotsForItemList(itemId: Long): List<SavingsSnapshotEntity>

    @Insert
    suspend fun insertSnapshot(snapshot: SavingsSnapshotEntity): Long
}
