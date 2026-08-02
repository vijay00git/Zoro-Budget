package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.sms.ParsedSmsTransaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Exercises BudgetRepository against a real in-memory Room database (via Robolectric), since
 * its whole job is keeping transaction rows and account balances consistent - that's not
 * something a mocked DAO would meaningfully verify.
 */
@RunWith(RobolectricTestRunner::class)
class BudgetRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: BudgetRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BudgetRepository(db, db.accountDao(), db.transactionDao(), db.categoryDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun addTransactionAppliesBalanceDelta() = runTest {
        val accountId = db.accountDao().insertAccount(
            AccountEntity(name = "Checking", type = "CHECKING", balance = 100.0)
        )

        repository.addTransaction(
            TransactionEntity(
                title = "Coffee",
                amount = 10.0,
                type = "DEBIT",
                accountId = accountId,
                categoryTag = "Food & Dining"
            )
        )

        val account = db.accountDao().getAccountById(accountId)
        assertEquals(90.0, account!!.balance, 0.001)
    }

    @Test
    fun updateTransactionRevertsOldEffectAndAppliesNewOne() = runTest {
        val accountId = db.accountDao().insertAccount(
            AccountEntity(name = "Checking", type = "CHECKING", balance = 100.0)
        )
        repository.addTransaction(
            TransactionEntity(
                title = "Coffee",
                amount = 10.0,
                type = "DEBIT",
                accountId = accountId,
                categoryTag = "Food & Dining"
            )
        )
        val stored = db.transactionDao().getAllTransactions().first().single()
        val updated = stored.copy(amount = 25.0)

        repository.updateTransaction(stored, updated)

        // 100 - 10 (initial debit) + 10 (revert old) - 25 (apply new) = 75
        val account = db.accountDao().getAccountById(accountId)
        assertEquals(75.0, account!!.balance, 0.001)
    }

    @Test
    fun deleteTransactionRevertsBalance() = runTest {
        val accountId = db.accountDao().insertAccount(
            AccountEntity(name = "Checking", type = "CHECKING", balance = 100.0)
        )
        repository.addTransaction(
            TransactionEntity(
                title = "Coffee",
                amount = 10.0,
                type = "DEBIT",
                accountId = accountId,
                categoryTag = "Food & Dining"
            )
        )
        val stored = db.transactionDao().getAllTransactions().first().single()

        repository.deleteTransaction(stored)

        val account = db.accountDao().getAccountById(accountId)
        assertEquals(100.0, account!!.balance, 0.001)
        assertTrue(db.transactionDao().getAllTransactions().first().isEmpty())
    }

    @Test
    fun transferBetweenAccountsMovesBalanceAndCreatesTwoTransactions() = runTest {
        val fromId = db.accountDao().insertAccount(AccountEntity(name = "Checking", type = "CHECKING", balance = 200.0))
        val toId = db.accountDao().insertAccount(AccountEntity(name = "Savings", type = "SAVINGS", balance = 50.0))

        repository.transferBetweenAccounts(fromId, toId, "Checking", "Savings", 75.0, notes = null)

        val fromAcc = db.accountDao().getAccountById(fromId)
        val toAcc = db.accountDao().getAccountById(toId)
        assertEquals(125.0, fromAcc!!.balance, 0.001)
        assertEquals(125.0, toAcc!!.balance, 0.001)
        assertEquals(2, db.transactionDao().getAllTransactions().first().size)
    }

    @Test
    fun processParsedSmsTransactionSkipsDuplicateMessage() = runTest {
        db.accountDao().insertAccount(AccountEntity(name = "Checking", type = "CHECKING", balance = 100.0))
        val parsed = ParsedSmsTransaction(
            amount = 20.0,
            type = "DEBIT",
            accountLast4 = null,
            merchantOrSender = "Store",
            suggestedCategory = "Shopping",
            rawMessage = "Rs. 20 debited at Store",
            timestamp = 1000L
        )

        val firstResult = repository.processParsedSmsTransaction(parsed)
        val secondResult = repository.processParsedSmsTransaction(parsed)

        assertTrue(firstResult)
        assertTrue("re-processing the same raw SMS must be rejected as a duplicate", !secondResult)
        assertEquals(1, db.transactionDao().getAllTransactions().first().size)
    }

    @Test
    fun deletingAnSmsTransactionKeepsItRejectedOnReSync() = runTest {
        // Regression test: deleting a transaction the user doesn't want must not let the next
        // "Sync SMS Inbox" recreate it from the same still-present phone SMS.
        val accountId = db.accountDao().insertAccount(
            AccountEntity(name = "Checking", type = "CHECKING", balance = 100.0)
        )
        val parsed = ParsedSmsTransaction(
            amount = 20.0,
            type = "DEBIT",
            accountLast4 = null,
            merchantOrSender = "Store",
            suggestedCategory = "Shopping",
            rawMessage = "Rs. 20 debited at Store",
            timestamp = 1000L
        )

        repository.processParsedSmsTransaction(parsed)
        val imported = db.transactionDao().getAllTransactions().first().single()
        assertEquals(accountId, imported.accountId)

        repository.deleteTransaction(imported)

        // Balance is reverted and the transaction no longer shows up anywhere...
        assertEquals(100.0, db.accountDao().getAccountById(accountId)!!.balance, 0.001)
        assertTrue(db.transactionDao().getAllTransactions().first().isEmpty())
        assertTrue(db.transactionDao().getPendingReviewTransactions().first().isEmpty())

        // ...but re-scanning the same SMS (simulating clicking "Sync SMS Inbox" again) must be
        // rejected as a duplicate, not silently recreate the transaction the user just deleted.
        val reprocessed = repository.processParsedSmsTransaction(parsed)
        assertTrue("re-syncing must not resurrect a deleted SMS transaction", !reprocessed)
        assertTrue(db.transactionDao().getAllTransactions().first().isEmpty())
        assertEquals(100.0, db.accountDao().getAccountById(accountId)!!.balance, 0.001)
    }

    @Test
    fun restoreAccountNeverCollidesWithAnExistingAccountId() = runTest {
        // Regression test: JSON backup restore used to insert accounts with REPLACE using the
        // id recorded in the backup, silently overwriting whatever account already had that id.
        val existingId = db.accountDao().insertAccount(
            AccountEntity(name = "Existing", type = "CHECKING", balance = 500.0)
        )

        val restoredId = repository.restoreAccount(
            AccountEntity(id = existingId, name = "Imported Duplicate", type = "SAVINGS", balance = 999.0)
        )

        assertNotEquals(existingId, restoredId)
        val existingAccount = db.accountDao().getAccountById(existingId)
        assertEquals("Existing", existingAccount!!.name)
        assertEquals(500.0, existingAccount.balance, 0.001)
    }

    @Test
    fun restoreTransactionDoesNotDoubleCountBalance() = runTest {
        // Regression test: a restored account's balance already reflects every transaction in
        // the backup, so restoring those transactions must not re-apply their delta on top.
        val restoredId = repository.restoreAccount(
            AccountEntity(id = 42, name = "Imported", type = "CHECKING", balance = 250.0)
        )

        repository.restoreTransaction(
            TransactionEntity(
                title = "Imported expense",
                amount = 30.0,
                type = "DEBIT",
                accountId = restoredId,
                categoryTag = "Shopping"
            )
        )

        val account = db.accountDao().getAccountById(restoredId)
        assertEquals(250.0, account!!.balance, 0.001)
        assertEquals(1, db.transactionDao().getAllTransactions().first().size)
    }
}
