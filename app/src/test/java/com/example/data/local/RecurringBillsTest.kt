package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.RecurringBillEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.sms.SmsParser
import com.example.data.utils.BillSchedule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class RecurringBillsTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: BudgetRepository
    private var accountId = 0L

    private fun date(year: Int, month: Int, day: Int, hour: Int = 10): Long = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day, hour, 0)
    }.timeInMillis

    @Before
    fun setUp() = runTest {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = BudgetRepository(db, db.accountDao(), db.transactionDao(), db.categoryDao())
        accountId = db.accountDao().insertAccount(
            AccountEntity(name = "HDFC", type = "CHECKING", balance = 10_000.0, isDefault = true, senderKeywords = "HDFCBK")
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun airtelBill(nextDue: Long = BillSchedule.startOfDay(date(2026, 10, 15))): RecurringBillEntity {
        val id = repository.addBill(
            RecurringBillEntity(
                name = "Airtel", amount = 824.82, accountId = accountId, categoryTag = "Bills & Utilities",
                nextDueDate = nextDue, dueDay = 15
            )
        )
        return db.recurringBillDao().getBillById(id)!!
    }

    private suspend fun balance() = db.accountDao().getAccountById(accountId)!!.balance

    @Test
    fun creatingABillDoesNotTouchTheBalance() = runTest {
        airtelBill()
        assertEquals(10_000.0, balance(), 0.001)
        assertTrue(db.transactionDao().getAllTransactions().first().isEmpty())
    }

    @Test
    fun recordingAPaymentAddsALedgerEntryAndMovesToTheNextCycle() = runTest {
        val bill = airtelBill()

        repository.recordBillPayment(bill, 824.82, accountId, date(2026, 10, 15))

        val payment = db.transactionDao().getAllTransactions().first().single()
        assertEquals(bill.id, payment.recurringBillId)
        assertEquals(824.82, payment.amount, 0.001)
        assertEquals(10_000.0 - 824.82, balance(), 0.001)
        assertEquals(BillSchedule.startOfDay(date(2026, 11, 15)), db.recurringBillDao().getBillById(bill.id)!!.nextDueDate)
    }

    @Test
    fun linkingAnExistingSmsPaymentDoesNotCountItTwice() = runTest {
        val bill = airtelBill()
        val smsPaymentId = db.transactionDao().insertTransaction(
            TransactionEntity(
                title = "SMS: Airtel", amount = 824.82, type = "DEBIT", accountId = accountId,
                categoryTag = "Uncategorized", dateTimestamp = date(2026, 10, 14), merchantOrSender = "Airtel"
            )
        )
        val candidates = repository.billPaymentCandidates(bill, now = date(2026, 10, 20))
        assertEquals(smsPaymentId, candidates.first().id)

        repository.linkBillPayment(bill, candidates.first())

        assertEquals(1, db.transactionDao().getAllTransactions().first().size)
        assertEquals(10_000.0, balance(), 0.001) // the SMS row was inserted directly, without a balance change
        val updated = db.recurringBillDao().getBillById(bill.id)!!
        assertEquals("Airtel", updated.matchPayee)
        assertEquals(BillSchedule.startOfDay(date(2026, 11, 15)), updated.nextDueDate)
        assertEquals("Bills & Utilities", db.transactionDao().getTransactionById(smsPaymentId)!!.categoryTag)
    }

    @Test
    fun undoReversesPaymentAndSkip() = runTest {
        val bill = airtelBill()
        val paid = repository.recordBillPayment(bill, 824.82, accountId, date(2026, 10, 15))
        repository.undoBillAction(paid)
        assertTrue(db.transactionDao().getAllTransactions().first().isEmpty())
        assertEquals(10_000.0, balance(), 0.001)
        assertEquals(bill.nextDueDate, db.recurringBillDao().getBillById(bill.id)!!.nextDueDate)

        val skipped = repository.skipBillCycle(bill)
        assertEquals(BillSchedule.startOfDay(date(2026, 11, 15)), db.recurringBillDao().getBillById(bill.id)!!.nextDueDate)
        repository.undoBillAction(skipped)
        assertEquals(bill.nextDueDate, db.recurringBillDao().getBillById(bill.id)!!.nextDueDate)
    }

    @Test
    fun incomingSmsPaymentForABillMarksItPaid() = runTest {
        val bill = repository.addBill(
            RecurringBillEntity(
                name = "Airtel Black", amount = 824.82, accountId = accountId, categoryTag = "Bills & Utilities",
                nextDueDate = BillSchedule.startOfDay(date(2026, 10, 15)), dueDay = 15, matchPayee = "Airtel"
            )
        )
        val sms = SmsParser.parseMessage(
            "UPI Mandate: Sent Rs.824.82 from HDFC Bank A/c 7460 To Airtel 15/10/26 Ref 512345678901",
            timestamp = date(2026, 10, 15),
            senderAddress = "VM-HDFCBK-S"
        )!!

        repository.processParsedSmsTransaction(sms)

        val tx = db.transactionDao().getAllTransactions().first().single()
        assertEquals(bill, tx.recurringBillId)
        assertEquals("Bills & Utilities", tx.categoryTag)
        assertEquals(BillSchedule.startOfDay(date(2026, 11, 15)), db.recurringBillDao().getBillById(bill)!!.nextDueDate)
    }

    @Test
    fun smsWithADifferentAmountDoesNotMarkTheBillPaid() = runTest {
        val bill = airtelBill()
        val sms = SmsParser.parseMessage(
            "Sent Rs.300.00 From HDFC Bank A/C *7460 To Airtel On 15/10/26 Ref 512345678902",
            timestamp = date(2026, 10, 15),
            senderAddress = "VM-HDFCBK-S"
        )!!
        repository.processParsedSmsTransaction(sms)
        assertNull(db.transactionDao().getAllTransactions().first().single().recurringBillId)
        assertEquals(bill.nextDueDate, db.recurringBillDao().getBillById(bill.id)!!.nextDueDate)
    }

    @Test
    fun deletingABillKeepsItsPaymentsInTheLedger() = runTest {
        val bill = airtelBill()
        repository.recordBillPayment(bill, 824.82, accountId, date(2026, 10, 15))
        repository.deleteBill(db.recurringBillDao().getBillById(bill.id)!!)
        val payment = db.transactionDao().getAllTransactions().first().single()
        assertNull(payment.recurringBillId)
        assertEquals(10_000.0 - 824.82, balance(), 0.001)
    }

    @Test
    fun legacyRecurringTransactionsAreConvertedWithoutChangingBalances() = runTest {
        // The shape of the user's real v8 data: an SMS payment flagged recurring (due on the
        // 15th, last marked paid Oct 15), a later payment to the same payee, and a dismissed
        // flagged row that must be ignored.
        val dao = db.transactionDao()
        val template = dao.insertTransaction(
            TransactionEntity(
                title = "AIRTEL BLACK ", amount = 824.82, type = "DEBIT", accountId = accountId, categoryTag = "Bills & Utilities",
                dateTimestamp = date(2026, 8, 15), source = "SMS_AUTO", merchantOrSender = "Airtel",
                isRecurring = true, recurringFrequency = "MONTHLY", recurringDueDate = 15, lastPaidTimestamp = date(2026, 10, 15, 22)
            )
        )
        val september = dao.insertTransaction(
            TransactionEntity(
                title = "SMS: Airtel Payments Bank Limi", amount = 824.82, type = "DEBIT", accountId = accountId, categoryTag = "Bills & Utilities",
                dateTimestamp = date(2026, 9, 15), source = "SMS_AUTO", merchantOrSender = "Airtel Payments Bank Limi"
            )
        )
        dao.insertTransaction(
            TransactionEntity(
                title = "AIRTEL BILL", amount = 824.82, type = "DEBIT", accountId = accountId, categoryTag = "Bills & Utilities",
                dateTimestamp = date(2026, 8, 13), isRecurring = true, recurringDueDate = 14, isDismissed = true
            )
        )

        LegacyRecurringBillConverter.convert(db.openHelper.writableDatabase, now = date(2026, 10, 20))

        val bill = db.recurringBillDao().getAllBillsList().single()
        assertEquals("AIRTEL BLACK", bill.name)
        assertEquals(15, bill.dueDay)
        assertEquals("Airtel", bill.matchPayee)
        assertEquals(BillSchedule.startOfDay(date(2026, 11, 15)), bill.nextDueDate)
        assertEquals(bill.id, dao.getTransactionById(template)!!.recurringBillId)
        assertEquals(bill.id, dao.getTransactionById(september)!!.recurringBillId)
        assertFalse(dao.getTransactionById(template)!!.isRecurring)
        assertEquals(10_000.0, balance(), 0.001)
    }
}
