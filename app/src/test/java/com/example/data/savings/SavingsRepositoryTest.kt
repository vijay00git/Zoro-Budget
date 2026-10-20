package com.example.data.savings

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.SavingsAttachmentEntity
import com.example.data.local.entity.SavingsItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class SavingsRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: SavingsRepository

    private fun date(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day)
    }.timeInMillis

    private val today = date(2026, 10, 20)

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = SavingsRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun item(section: SavingsSection, holder: String = "Vijay", start: Long = date(2025, 10, 20), maturity: Long? = null) =
        SavingsItemEntity(section = section.name, name = "${section.name} record", holder = holder, startDate = start, maturityDate = maturity)

    private suspend fun addFd(holder: String = "Vijay"): Long {
        val fd = SavingsDetails.Fd(principal = 100_000.0, ratePct = 7.0)
        val start = date(2025, 10, 20)
        return repository.addItem(
            item(SavingsSection.FD, holder, start, date(2027, 10, 20)), fd,
            SavingsValuation.suggestedEntries(fd, start, today), today
        )
    }

    @Test
    fun addingAnFdRecordsItsHistoryAndValueWithoutTouchingTheLedger() = runTest {
        val accountId = db.accountDao().insertAccount(AccountEntity(name = "HDFC", type = "SAVINGS", balance = 50_000.0))

        val id = addFd()

        val saved = db.savingsDao().getItemById(id)!!
        assertEquals(100_000.0, saved.investedAmount, 0.001)
        assertEquals(107_185.90, saved.currentValue, 0.01)
        assertEquals(1, db.savingsDao().getEntriesForItemList(id).size)
        assertEquals(50_000.0, db.accountDao().getAccountById(accountId)!!.balance, 0.001)
        assertTrue(db.transactionDao().getAllTransactions().first().isEmpty())
    }

    @Test
    fun detailsRoundTripThroughJson() {
        val all = listOf(
            SavingsDetails.Fd(100_000.0, 7.0, Compounding.MONTHLY, Frequency.QUARTERLY, autoRenew = true),
            SavingsDetails.Rd(5_000.0, 6.5, 12, 5),
            SavingsDetails.Stock("TCS", "NSE", 10.0, 3_500.0, 3_900.0, today, 120.0, "IT"),
            SavingsDetails.MutualFund("PPFAS", "DIRECT", "EQUITY", 5_000.0, 10, null, 25_000.0, 2_150.432, 92.15, today),
            SavingsDetails.Gold(GoldForm.SGB, 10.0, GoldPurity.K24, 50_000.0, sgbIssuePrice = 5_000.0),
            SavingsDetails.Silver(SilverForm.COIN, 250.0, 99.9, 25_000.0),
            SavingsDetails.Land(PropertyType.PLOT, "Avadi", "123/4", null, 5.0, AreaUnit.CENT, 1_200_000.0, 108_000.0, 0.0, 20_000.0, 2_000_000.0, today, "OWN_ESTIMATE"),
            SavingsDetails.Scheme(250_000.0, today, 150_000.0, 7.1),
            SavingsDetails.Insurance(25_000.0, Frequency.HALF_YEARLY, 15, 500_000.0, 40_000.0),
            SavingsDetails.Chit(100_000.0, 5_000.0, 20, 8),
            SavingsDetails.Bond(10_000.0, 8.5, Frequency.HALF_YEARLY),
            SavingsDetails.LoanGiven("Ravi", 50_000.0, 12.0),
            SavingsDetails.Generic(10_000.0, 12_345.0)
        )
        all.forEach { assertEquals(it, SavingsDetails.fromJson(it.toJson())) }
        assertNull(SavingsDetails.fromJson("not json"))
    }

    @Test
    fun holderNamesAreTidiedAndReusedIgnoringCase() = runTest {
        val first = addFd(holder = "Vijay")
        val second = addFd(holder = "  vijay  ")
        val third = addFd(holder = "Priya   Devi")

        assertEquals("Vijay", db.savingsDao().getItemById(second)!!.holder)
        assertEquals("Priya Devi", db.savingsDao().getItemById(third)!!.holder)
        assertEquals(listOf("Priya Devi", "Vijay"), repository.holders.first())

        repository.renameHolder("VIJAY", "Vijay S")
        assertEquals("Vijay S", db.savingsDao().getItemById(first)!!.holder)
        assertEquals("Vijay S", db.savingsDao().getItemById(second)!!.holder)
    }

    @Test
    fun goldIsValuedAtPurchasePriceUntilARateIsSetThenRevalued() = runTest {
        val gold = SavingsDetails.Gold(grams = 20.0, purity = GoldPurity.K22, purchaseAmount = 150_000.0)
        val id = repository.addItem(item(SavingsSection.GOLD), gold, SavingsValuation.suggestedEntries(gold, date(2025, 10, 20), today), today)
        assertEquals(150_000.0, db.savingsDao().getItemById(id)!!.currentValue, 0.001)

        repository.setRate(SnapshotKeys.GOLD_24K, 10_000.0, today)

        assertEquals(183_333.33, db.savingsDao().getItemById(id)!!.currentValue, 0.01)
        assertEquals(10_000.0, repository.rates().gold24PerGram!!, 0.001)
        val history = repository.snapshots.first().filter { it.itemId == id }.map { it.value }
        assertEquals(listOf(150_000.0, 183_333.33), history.map { Math.round(it * 100) / 100.0 })
    }

    @Test
    fun rdHistoryDrivesInvestedAndInstalmentsPaid() = runTest {
        val rd = SavingsDetails.Rd(5_000.0, 6.5, 12, 5)
        val start = date(2026, 5, 5)
        val entries = SavingsValuation.suggestedEntries(rd, start, today)
        assertEquals(6, entries.size) // May..Oct

        val id = repository.addItem(item(SavingsSection.RD, start = start, maturity = date(2027, 5, 5)), rd, entries, today)
        assertEquals(30_000.0, db.savingsDao().getItemById(id)!!.investedAmount, 0.001)

        // The user missed August.
        val august = db.savingsDao().getEntriesForItemList(id).first { it.date == date(2026, 8, 5) }
        repository.deleteEntry(august, today)
        assertEquals(25_000.0, db.savingsDao().getItemById(id)!!.investedAmount, 0.001)
    }

    @Test
    fun editingAnFdAmountMovesItsOriginalEntryButNotHandEditedHistory() = runTest {
        val id = addFd()
        val saved = db.savingsDao().getItemById(id)!!

        repository.updateItem(saved, SavingsDetails.Fd(principal = 150_000.0, ratePct = 7.0), today)
        assertEquals(150_000.0, db.savingsDao().getEntriesForItemList(id).single().amount, 0.001)
        assertEquals(150_000.0, db.savingsDao().getItemById(id)!!.investedAmount, 0.001)

        // Once the user edits the history by hand, later edits leave it alone.
        val entry = db.savingsDao().getEntriesForItemList(id).single()
        repository.updateEntry(entry.copy(amount = 140_000.0), today)
        repository.updateItem(db.savingsDao().getItemById(id)!!, SavingsDetails.Fd(principal = 200_000.0, ratePct = 7.0), today)
        assertEquals(140_000.0, db.savingsDao().getEntriesForItemList(id).single().amount, 0.001)
    }

    @Test
    fun undoRestoresADeletedRecordWithItsHistory() = runTest {
        val id = addFd()
        val deleted = repository.deleteItemForUndo(db.savingsDao().getItemById(id)!!)
        assertNull(db.savingsDao().getItemById(id))

        repository.restore(deleted)

        assertEquals(107_185.90, db.savingsDao().getItemById(id)!!.currentValue, 0.01)
        assertEquals(1, db.savingsDao().getEntriesForItemList(id).size)
        assertEquals(deleted.snapshots.size, db.savingsDao().getSnapshotsForItemList(id).size)
    }

    @Test
    fun summaryFiltersByHolderAndStatusAndAddsAccountsToNetWorth() = runTest {
        addFd(holder = "Vijay")
        val priyaId = addFd(holder = "Priya")
        val closed = db.savingsDao().getItemById(priyaId)!!.copy(status = SavingsStatus.CLOSED.name)
        db.savingsDao().updateItem(closed)
        val all = db.savingsDao().getAllItemsList()

        assertEquals(1, SavingsSummary.filter(all, null, setOf(SavingsStatus.ACTIVE)).size)
        assertEquals(1, SavingsSummary.filter(all, "priya", setOf(SavingsStatus.CLOSED)).size)
        assertTrue(SavingsSummary.filter(all, "Priya", setOf(SavingsStatus.ACTIVE)).isEmpty())

        val active = SavingsSummary.filter(all, null, setOf(SavingsStatus.ACTIVE))
        // Savings 107,185.90 + bank 50,000 + credit card owing 2,000 (stored negative).
        assertEquals(155_185.90, SavingsSummary.netWorth(listOf(50_000.0, -2_000.0), active), 0.01)
        assertEquals(date(2027, 10, 20), SavingsSummary.upcomingMaturities(active, today).single().maturityDate)
        assertEquals(listOf(SavingsSection.FD), SavingsSummary.allocation(active).map { it.first })
    }

    @Test
    fun sipWithALumpSumStartsWithBothInItsHistory() = runTest {
        val start = date(2026, 7, 10)
        val fund = SavingsDetails.MutualFund(sipAmount = 5_000.0, sipDay = 10, lumpSum = 20_000.0, units = 500.0, nav = 85.0)
        val entries = SavingsValuation.suggestedEntries(fund, start, today)
        assertEquals(listOf(20_000.0, 5_000.0, 5_000.0, 5_000.0, 5_000.0), entries.map { it.amount }) // lump sum + Jul..Oct

        val id = repository.addItem(item(SavingsSection.MUTUAL_FUND, start = start), fund, entries, today)
        val saved = db.savingsDao().getItemById(id)!!
        assertEquals(40_000.0, saved.investedAmount, 0.001)
        assertEquals(42_500.0, saved.currentValue, 0.001)
    }

    @Test
    fun otherSchemesAreValuedFromTheirOwnInputs() = runTest {
        val crypto = SavingsDetails.Generic(invested = 10_000.0, currentValue = 12_345.0)
        val cryptoId = repository.addItem(item(SavingsSection.OTHER), crypto, SavingsValuation.suggestedEntries(crypto, date(2025, 10, 20), today), today)
        assertEquals(10_000.0, db.savingsDao().getItemById(cryptoId)!!.investedAmount, 0.001)
        assertEquals(12_345.0, db.savingsDao().getItemById(cryptoId)!!.currentValue, 0.001)

        val ppf = SavingsDetails.Scheme(balance = 250_000.0, yearlyContribution = 150_000.0, ratePct = 7.1)
        val ppfId = repository.addItem(item(SavingsSection.OTHER), ppf, emptyList(), today)
        assertEquals(250_000.0, db.savingsDao().getItemById(ppfId)!!.currentValue, 0.001)

        val plot = SavingsDetails.Land(area = 5.0, areaUnit = AreaUnit.CENT, purchasePrice = 1_200_000.0, currentValue = 2_000_000.0, loanOutstanding = 300_000.0)
        val plotId = repository.addItem(item(SavingsSection.LAND), plot, SavingsValuation.suggestedEntries(plot, date(2019, 6, 15), today), today)
        assertEquals(1_200_000.0, db.savingsDao().getItemById(plotId)!!.investedAmount, 0.001)
        // Net worth takes the loan against the plot off.
        val items = db.savingsDao().getAllItemsList()
        assertEquals(12_345.0 + 250_000.0 + 2_000_000.0 - 300_000.0, SavingsSummary.netWorth(emptyList(), items), 0.01)
    }

    @Test
    fun deletingARecordRemovesItsHistoryAttachmentsAndSnapshots() = runTest {
        val id = addFd()
        db.savingsDao().insertAttachment(
            SavingsAttachmentEntity(itemId = id, fileName = "fd.pdf", mimeType = "application/pdf", relativePath = "savings_attachments/$id/fd.pdf", sizeBytes = 10)
        )

        repository.deleteItem(db.savingsDao().getItemById(id)!!)

        assertTrue(db.savingsDao().getEntriesForItemList(id).isEmpty())
        assertTrue(db.savingsDao().getAttachmentsForItemList(id).isEmpty())
        assertTrue(repository.snapshots.first().none { it.itemId == id })
    }
}
