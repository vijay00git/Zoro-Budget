package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.sms.ParsedSmsTransaction
import com.example.data.utils.CategoryTotals
import com.example.ui.components.lowBalanceAccounts
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Any category but "Uncategorized" can be deleted; transactions already in it keep it. */
@RunWith(RobolectricTestRunner::class)
class CategoryDeletionTest {

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

    private suspend fun category(name: String) = repository.categories.first().first { it.name == name }

    @Test
    fun deletingABuiltInCategoryKeepsItOnOldTransactionsAndBills() = runTest {
        repository.initializeDefaultDataIfNeeded(seedDemo = true)
        val bills = category("Bills & Utilities")

        repository.deleteCategory(bills)

        val names = repository.categories.first().map { it.name }
        assertTrue("Bills & Utilities" !in names)
        val transactions = db.transactionDao().getAllTransactionsList()
        assertEquals(2, transactions.count { it.categoryTag == "Bills & Utilities" })
        assertTrue(repository.bills.first().any { it.categoryTag == "Bills & Utilities" })
    }

    @Test
    fun deletingAnAddedCategoryWorksToo() = runTest {
        repository.initializeDefaultDataIfNeeded(seedDemo = false)
        repository.addCategory(CategoryEntity(name = "Pets", iconName = "Pets", colorHex = "#000000", isPredefined = false))

        repository.deleteCategory(category("Pets"))

        assertTrue(repository.categories.first().none { it.name == "Pets" })
    }

    @Test
    fun uncategorizedCannotBeDeleted() = runTest {
        repository.initializeDefaultDataIfNeeded(seedDemo = false)

        repository.deleteCategory(category(CategoryTotals.UNCATEGORIZED))

        assertTrue(repository.categories.first().any { it.name == CategoryTotals.UNCATEGORIZED })
    }

    @Test
    fun smsForADeletedCategoryGoesToUncategorized() = runTest {
        repository.initializeDefaultDataIfNeeded(seedDemo = false)
        repository.deleteCategory(category("Shopping"))

        repository.processParsedSmsTransaction(
            ParsedSmsTransaction(
                amount = 20.0,
                type = "DEBIT",
                accountLast4 = null,
                merchantOrSender = "Store",
                suggestedCategory = "Shopping",
                rawMessage = "Rs. 20 debited at Store",
                timestamp = 1000L
            )
        )

        assertEquals(CategoryTotals.UNCATEGORIZED, db.transactionDao().getAllTransactionsList().single().categoryTag)
    }

    @Test
    fun aNewEmptyAccountIsNotLowOnMoney() {
        val fresh = AccountEntity(id = 1, name = "Main account", type = "CHECKING", balance = 0.0)
        val usedUp = AccountEntity(id = 2, name = "Spent", type = "CHECKING", balance = 0.0)
        val low = AccountEntity(id = 3, name = "Low", type = "CHECKING", balance = 50.0)
        val spend = TransactionEntity(title = "Rent", amount = 100.0, type = "DEBIT", accountId = 2, categoryTag = "Bills & Utilities")

        val result = lowBalanceAccounts(listOf(fresh, usedUp, low), threshold = 200.0, transactions = listOf(spend))

        assertEquals(listOf("Spent", "Low"), result.map { it.name })
    }
}
