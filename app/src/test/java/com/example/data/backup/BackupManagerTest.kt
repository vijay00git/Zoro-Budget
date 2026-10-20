package com.example.data.backup

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DATABASE_NAME
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.RecurringBillEntity
import com.example.data.local.entity.SavingsItemEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.savings.SavingsDetails
import com.example.data.savings.SavingsRepository
import com.example.data.savings.SavingsValuation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
class BackupManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val attachments get() = File(context.filesDir, BackupManager.ATTACHMENTS_DIR)

    @Before
    fun setUp() {
        AppDatabase.closeInstance()
        context.deleteDatabase(DATABASE_NAME)
        attachments.deleteRecursively()
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        context.deleteDatabase(DATABASE_NAME)
        attachments.deleteRecursively()
    }

    private fun db() = AppDatabase.getDatabase(context)

    /** One of everything the backup must carry: account, kept and dismissed SMS, bill, FD, attachment, settings. */
    private suspend fun seed() {
        val db = db()
        val accountId = db.accountDao().insertAccount(AccountEntity(name = "HDFC", type = "SAVINGS", balance = 48_064.35))
        db.transactionDao().insertTransaction(
            TransactionEntity(title = "Kept", amount = 450.0, type = "DEBIT", accountId = accountId, categoryTag = "Food", source = "SMS_AUTO", smsSender = "VM-HDFCBK")
        )
        db.transactionDao().insertTransaction(
            TransactionEntity(title = "Dismissed OTP", amount = 1.0, type = "DEBIT", accountId = accountId, categoryTag = "Food", isDismissed = true)
        )
        db.recurringBillDao().insertBill(
            RecurringBillEntity(name = "AIRTEL BLACK", amount = 824.82, accountId = accountId, categoryTag = "Bills", nextDueDate = 1_794_681_000_000, dueDay = 15)
        )
        val fd = SavingsDetails.Fd(principal = 100_000.0, ratePct = 7.0)
        val start = 1_792_454_400_000
        SavingsRepository(db).addItem(
            SavingsItemEntity(section = "FD", name = "SBI FD", holder = "Vijay", startDate = start, maturityDate = start + 63_158_400_000),
            fd, SavingsValuation.suggestedEntries(fd, start, start)
        )
        File(attachments, "1/receipt.pdf").apply {
            parentFile!!.mkdirs()
            writeText("pdf bytes")
        }
        context.getSharedPreferences("smart_budget_prefs", Context.MODE_PRIVATE).edit(commit = true) {
            putString("currency_symbol", "₹")
            putFloat("low_balance_threshold", 10_000f)
            putLong("sms_sync_from_date", 1_790_000_000_000)
        }
        context.getSharedPreferences("savings_prefs", Context.MODE_PRIVATE).edit(commit = true) {
            putStringSet("status_filter", setOf("ACTIVE", "MATURED"))
            putBoolean("group_by_holder", true)
        }
    }

    private fun backupBytes(): ByteArray = ByteArrayOutputStream().also { BackupManager.createBackup(context, db(), it) }.toByteArray()

    @Test
    fun backupThenRestoreBringsEverythingBackExactly() = runTest {
        seed()
        val bytes = backupBytes()

        val info = BackupManager.readInfo(bytes.inputStream())
        assertEquals(BackupManager.Counts(accounts = 1, transactions = 1, bills = 1, savingsItems = 1, attachments = 1), info.counts)
        assertEquals(10, info.dbVersion)

        // Change everything after the backup.
        val db = db()
        db.savingsDao().deleteItem(db.savingsDao().getAllItemsList().single())
        db.transactionDao().insertTransaction(TransactionEntity(title = "After backup", amount = 9.0, type = "DEBIT", accountId = 1, categoryTag = "Food"))
        attachments.deleteRecursively()
        context.getSharedPreferences("smart_budget_prefs", Context.MODE_PRIVATE).edit(commit = true) {
            clear()
            putString("currency_symbol", "$")
        }

        BackupManager.restore(context, bytes.inputStream())

        val restored = db()
        val transactions = restored.transactionDao().getAllTransactions().first()
        assertEquals(listOf("Kept"), transactions.map { it.title })
        assertEquals("VM-HDFCBK", transactions.single().smsSender)
        // The dismissed SMS comes back too, so the next inbox scan does not re-import it.
        assertEquals(2, restored.openHelper.readableDatabase.query("SELECT COUNT(*) FROM transactions").use { it.moveToFirst(); it.getInt(0) })
        assertEquals(48_064.35, restored.accountDao().getAllAccountsList().single().balance, 0.001)
        assertEquals("AIRTEL BLACK", restored.recurringBillDao().getAllBillsList().single().name)
        val fd = restored.savingsDao().getAllItemsList().single()
        assertEquals("SBI FD", fd.name)
        assertEquals(1, restored.savingsDao().getEntriesForItemList(fd.id).size)
        assertEquals("pdf bytes", File(attachments, "1/receipt.pdf").readText())

        val prefs = context.getSharedPreferences("smart_budget_prefs", Context.MODE_PRIVATE)
        assertEquals("₹", prefs.getString("currency_symbol", null))
        assertEquals(10_000f, prefs.getFloat("low_balance_threshold", 0f))
        assertEquals(1_790_000_000_000, prefs.getLong("sms_sync_from_date", 0))
        val savingsPrefs = context.getSharedPreferences("savings_prefs", Context.MODE_PRIVATE)
        assertEquals(setOf("ACTIVE", "MATURED"), savingsPrefs.getStringSet("status_filter", null))
        assertTrue(savingsPrefs.getBoolean("group_by_holder", false))
    }

    @Test
    fun safetyCopyIsKeptBeforeARestoreAndOnlyTheNewestTwoStay() = runTest {
        seed()
        repeat(3) { i ->
            BackupManager.saveSafetyCopy(context, db()).setLastModified(1_000L * (i + 1))
        }
        val copies = File(context.filesDir, "backups").listFiles()!!.toList()
        assertTrue(copies.size <= 2)
        assertEquals(1, BackupManager.readInfo(copies.first().inputStream()).counts.savingsItems)
    }

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((name, data) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(data)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun assertRefused(bytes: ByteArray) {
        try {
            BackupManager.restore(context, bytes.inputStream())
            fail("restore should have been refused")
        } catch (e: BackupManager.BackupException) {
            // expected
        }
    }

    @Test
    fun badFilesAreRefusedAndLeaveTheCurrentDataAlone() = runTest {
        seed()
        val manifest = JSONObject().put("format", BackupManager.FORMAT).put("formatVersion", 1).toString().toByteArray()

        assertRefused("not a zip at all".toByteArray())
        assertRefused(zipOf("notes.txt" to "hello".toByteArray()))
        assertRefused(zipOf("manifest.json" to manifest)) // no database
        assertRefused(zipOf("manifest.json" to manifest, "database/$DATABASE_NAME" to "garbage".toByteArray()))
        assertRefused(zipOf("manifest.json" to manifest, "../escape.txt" to "x".toByteArray()))
        assertFalse(File(context.cacheDir.parentFile, "escape.txt").exists())

        // A backup from a newer schema is refused rather than half-opened.
        val newer = File(context.cacheDir, "newer.db")
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(newer, null).use { it.version = 99 }
        assertRefused(zipOf("manifest.json" to manifest, "database/$DATABASE_NAME" to newer.readBytes()))

        // None of that touched the real data.
        assertEquals(1, db().savingsDao().getAllItemsList().size)
        assertEquals("pdf bytes", File(attachments, "1/receipt.pdf").readText())
    }
}
