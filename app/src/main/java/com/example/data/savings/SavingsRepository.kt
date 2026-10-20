package com.example.data.savings

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entity.SavingsAttachmentEntity
import com.example.data.local.entity.SavingsEntryEntity
import com.example.data.local.entity.SavingsItemEntity
import com.example.data.local.entity.SavingsSnapshotEntity
import kotlinx.coroutines.flow.Flow

/**
 * Savings & assets records. Nothing here touches accounts or the ledger: the Savings page only
 * keeps a record of what was invested, when, and what it is worth.
 */
class SavingsRepository(private val db: AppDatabase) {
    private val dao = db.savingsDao()

    val items: Flow<List<SavingsItemEntity>> = dao.getAllItems()
    val entries: Flow<List<SavingsEntryEntity>> = dao.getAllEntries()
    val attachments: Flow<List<SavingsAttachmentEntity>> = dao.getAllAttachments()
    val snapshots: Flow<List<SavingsSnapshotEntity>> = dao.getAllSnapshots()
    val holders: Flow<List<String>> = dao.getHolders()

    fun entriesFor(itemId: Long): Flow<List<SavingsEntryEntity>> = dao.getEntriesForItem(itemId)
    fun attachmentsFor(itemId: Long): Flow<List<SavingsAttachmentEntity>> = dao.getAttachmentsForItem(itemId)

    suspend fun rates(): MarketRates {
        val gold = dao.getLatestRate(SnapshotKeys.GOLD_24K)
        val silver = dao.getLatestRate(SnapshotKeys.SILVER_KG)
        return MarketRates(
            gold24PerGram = gold?.value,
            gold22PerGram = dao.getLatestRate(SnapshotKeys.GOLD_22K)?.value,
            silverPerKg = silver?.value,
            goldDate = gold?.date,
            silverDate = silver?.date
        )
    }

    /**
     * Tidies a typed holder name and reuses the spelling already on file, so "vijay " and
     * "Vijay" are one holder.
     */
    suspend fun normalizeHolder(raw: String): String {
        val tidy = raw.trim().replace(Regex("\\s+"), " ")
        return dao.getHoldersList().firstOrNull { it.equals(tidy, ignoreCase = true) } ?: tidy
    }

    /** Saves a new record with its starting investment history and returns its id. */
    suspend fun addItem(
        item: SavingsItemEntity,
        details: SavingsDetails,
        initialEntries: List<SavingsEntryEntity>,
        today: Long = System.currentTimeMillis()
    ): Long = db.withTransaction {
        val id = dao.insertItem(item.copy(id = 0, holder = normalizeHolder(item.holder), detailsJson = details.toJson()))
        if (initialEntries.isNotEmpty()) dao.insertEntries(initialEntries.map { it.copy(id = 0, itemId = id) })
        refresh(id, today, recordSnapshot = true)
        id
    }

    /** Saves edits to a record; a changed value is added to its value history. */
    suspend fun updateItem(item: SavingsItemEntity, details: SavingsDetails, today: Long = System.currentTimeMillis()) {
        db.withTransaction {
            syncLumpSumEntry(item, details, today)
            dao.updateItem(item.copy(holder = normalizeHolder(item.holder), detailsJson = details.toJson(), updatedAt = today))
            refresh(item.id, today, recordSnapshot = true)
        }
    }

    /**
     * A lump-sum record (an FD, a plot...) starts with one history entry made from its amount.
     * When the user edits that amount or the start date, the entry follows - but only while it
     * is still the untouched original, so history the user edited by hand is never overwritten.
     */
    private suspend fun syncLumpSumEntry(item: SavingsItemEntity, details: SavingsDetails, today: Long) {
        val saved = dao.getItemById(item.id) ?: return
        val oldDetails = SavingsDetails.fromJson(saved.detailsJson) ?: return
        val original = SavingsValuation.suggestedEntries(oldDetails, saved.startDate, today).singleOrNull() ?: return
        val entry = dao.getEntriesForItemList(item.id).singleOrNull() ?: return
        if (entry.date != original.date || entry.amount != original.amount || entry.type != original.type) return
        val updated = SavingsValuation.suggestedEntries(details, item.startDate, today).singleOrNull() ?: return
        dao.updateEntry(entry.copy(date = updated.date, amount = updated.amount, units = updated.units, unitPrice = updated.unitPrice))
    }

    suspend fun deleteItem(item: SavingsItemEntity) = dao.deleteItem(item)

    /** Everything a deleted record had, so Undo can put it back exactly. */
    data class DeletedItem(
        val item: SavingsItemEntity,
        val entries: List<SavingsEntryEntity>,
        val snapshots: List<SavingsSnapshotEntity>,
        val attachments: List<SavingsAttachmentEntity>
    )

    /** Deletes a record with its history; the returned bundle restores it via [restore]. */
    suspend fun deleteItemForUndo(item: SavingsItemEntity): DeletedItem = db.withTransaction {
        val deleted = DeletedItem(
            item = dao.getItemById(item.id) ?: item,
            entries = dao.getEntriesForItemList(item.id),
            snapshots = dao.getSnapshotsForItemList(item.id),
            attachments = dao.getAttachmentsForItemList(item.id)
        )
        dao.deleteItem(item)
        deleted
    }

    suspend fun restore(deleted: DeletedItem) {
        db.withTransaction {
            dao.insertItem(deleted.item)
            if (deleted.entries.isNotEmpty()) dao.insertEntries(deleted.entries)
            deleted.snapshots.forEach { dao.insertSnapshot(it) }
            deleted.attachments.forEach { dao.insertAttachment(it) }
        }
    }

    suspend fun getItem(id: Long): SavingsItemEntity? = dao.getItemById(id)

    suspend fun addEntry(entry: SavingsEntryEntity, today: Long = System.currentTimeMillis()): Long = db.withTransaction {
        val id = dao.insertEntry(entry.copy(id = 0))
        refresh(entry.itemId, today)
        id
    }

    suspend fun updateEntry(entry: SavingsEntryEntity, today: Long = System.currentTimeMillis()) {
        db.withTransaction {
            dao.updateEntry(entry)
            refresh(entry.itemId, today)
        }
    }

    suspend fun deleteEntry(entry: SavingsEntryEntity, today: Long = System.currentTimeMillis()) {
        db.withTransaction {
            dao.deleteEntry(entry)
            refresh(entry.itemId, today)
        }
    }

    /** Records today's gold or silver rate ([SnapshotKeys]) and revalues every record that uses it. */
    suspend fun setRate(key: String, value: Double, today: Long = System.currentTimeMillis()) {
        db.withTransaction {
            dao.insertSnapshot(SavingsSnapshotEntity(key = key, value = value, date = today))
            val section = if (key == SnapshotKeys.SILVER_KG) SavingsSection.SILVER else SavingsSection.GOLD
            dao.getAllItemsList().filter { it.section == section.name }.forEach { refresh(it.id, today, recordSnapshot = true) }
        }
    }

    suspend fun renameHolder(oldName: String, newName: String) {
        val tidy = newName.trim().replace(Regex("\\s+"), " ")
        if (tidy.isNotEmpty()) dao.renameHolder(oldName, tidy)
    }

    /** Recomputes all cached values, e.g. on opening the page so FDs and RDs show today's value. */
    suspend fun refreshAll(today: Long = System.currentTimeMillis()) {
        db.withTransaction { dao.getAllItemsList().forEach { refresh(it.id, today) } }
    }

    /**
     * Recomputes a record's cached invested and current value from its details, history and the
     * shared rates. With [recordSnapshot], a changed current value is kept for the trend line
     * (only on user edits and rate changes, so FD interest accruing day by day adds no rows).
     */
    private suspend fun refresh(itemId: Long, today: Long, recordSnapshot: Boolean = false) {
        val item = dao.getItemById(itemId) ?: return
        val value = SavingsValuation.value(
            item, SavingsDetails.fromJson(item.detailsJson), dao.getEntriesForItemList(itemId), rates(), today
        )
        if (value.invested != item.investedAmount || value.current != item.currentValue) {
            dao.updateItem(item.copy(investedAmount = value.invested, currentValue = value.current))
        }
        if (recordSnapshot && value.current != item.currentValue) {
            dao.insertSnapshot(SavingsSnapshotEntity(itemId = itemId, key = SnapshotKeys.ITEM_VALUE, value = value.current, date = today))
        }
    }
}
