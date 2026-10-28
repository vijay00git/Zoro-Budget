package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.TransactionEntity
import com.example.data.preferences.FirstRun
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Demo data is seeded only on a fresh install, and "Clear demo data" removes only demo rows. */
@RunWith(RobolectricTestRunner::class)
class DemoDataTest {

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
    fun freshInstallSeedsDemoDataOnce() = runTest {
        val demo = repository.initializeDefaultDataIfNeeded(seedDemo = true)

        assertNotNull(demo)
        assertEquals(3, demo!!.accounts.size)
        assertEquals(7, demo.transactions.size)
        assertEquals(4, demo.bills.size)
        assertNull(repository.initializeDefaultDataIfNeeded(seedDemo = true))
    }

    @Test
    fun withoutDemoAnEmptyDatabaseGetsOneEmptyAccount() = runTest {
        assertNull(repository.initializeDefaultDataIfNeeded(seedDemo = false))

        val accounts = db.accountDao().getAllAccountsList()
        assertEquals(1, accounts.size)
        assertTrue(accounts.single().isDefault)
        assertEquals(0.0, accounts.single().balance, 0.0)
        assertTrue(db.transactionDao().getAllTransactionsList().isEmpty())
        assertTrue(repository.bills.first().isEmpty())
        assertTrue(repository.categories.first().isNotEmpty())
    }

    @Test
    fun clearingRemovesDemoRowsAndKeepsCategories() = runTest {
        val demo = repository.initializeDefaultDataIfNeeded(seedDemo = true)!!
        val categories = repository.categories.first().size

        repository.clearDemoData(demo)

        assertTrue(db.transactionDao().getAllTransactionsList().isEmpty())
        assertTrue(repository.bills.first().isEmpty())
        val accounts = db.accountDao().getAllAccountsList()
        assertEquals(listOf("Main account"), accounts.map { it.name })
        assertTrue(accounts.single().isDefault)
        assertEquals(categories, repository.categories.first().size)
    }

    @Test
    fun clearingKeepsRealAndEditedRows() = runTest {
        val demo = repository.initializeDefaultDataIfNeeded(seedDemo = true)!!
        val hdfc = db.accountDao().getAllAccountsList().first { it.name == "HDFC Bank Account" }
        val indianBank = db.accountDao().getAllAccountsList().first { it.name == "Indian Bank Savings" }
        // A real SMS that landed in the demo default account.
        val realId = db.transactionDao().insertTransaction(
            TransactionEntity(title = "Real payment", amount = 99.0, type = "DEBIT", accountId = hdfc.id, categoryTag = "Shopping")
        )
        // The user renamed a demo account and a demo bill: they are theirs now.
        db.accountDao().updateAccount(indianBank.copy(name = "My savings"))
        val netflix = repository.bills.first().first { it.name == "Netflix Subscription" }
        db.recurringBillDao().updateBill(netflix.copy(name = "Netflix"))
        // Reviewing the demo SMS changes its title; it is still demo data.
        val starbucks = db.transactionDao().getAllTransactionsList().first { it.title.contains("Starbucks") }
        db.transactionDao().updateTransaction(starbucks.copy(title = "Coffee", isPendingReview = false))

        repository.clearDemoData(demo)

        assertEquals(listOf(realId), db.transactionDao().getAllTransactionsList().map { it.id })
        assertEquals(listOf("Netflix"), repository.bills.first().map { it.name })
        val names = db.accountDao().getAllAccountsList().map { it.name }.toSet()
        assertEquals(setOf("HDFC Bank Account", "My savings"), names)
    }

    @Test
    fun demoRecordSurvivesJson() {
        val demo = DemoData(mapOf(1L to "a|1"), mapOf(2L to "5.0|10", 3L to "6.0|11"), emptyMap())

        assertEquals(demo, DemoData.fromJson(demo.toJson()))
        assertNull(DemoData.fromJson(null))
        assertNull(DemoData.fromJson("not json"))
    }

    @Test
    fun onlyAFreshInstallCounts() {
        assertTrue(FirstRun.isFreshInstall(firstInstallTime = 1000L, lastUpdateTime = 1000L))
        assertFalse(FirstRun.isFreshInstall(firstInstallTime = 1000L, lastUpdateTime = 5000L))
    }
}
