package com.example.data.local

import androidx.room.withTransaction
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringBillEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.sms.ParsedSmsTransaction
import com.example.data.sms.SmsAccountMatcher
import com.example.data.sms.SmsParser
import com.example.data.utils.BillFrequency
import com.example.data.utils.BillSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class BudgetRepository(
    private val db: AppDatabase,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {
    val accounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val transactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val pendingTransactions: Flow<List<TransactionEntity>> = transactionDao.getPendingReviewTransactions()
    val categories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    private val billDao = db.recurringBillDao()
    val bills: Flow<List<RecurringBillEntity>> = billDao.getAllBills()

    suspend fun initializeDefaultDataIfNeeded() {
        db.withTransaction { initializeDefaultDataIfNeededInternal() }
    }

    private suspend fun initializeDefaultDataIfNeededInternal() {
        // Seed default categories if empty
        if (categoryDao.getCategoryCount() == 0) {
            val defaultCategories = listOf(
                CategoryEntity(name = "Food & Dining", iconName = "Fastfood", colorHex = "#EF4444", monthlyBudgetLimit = 350.0),
                CategoryEntity(name = "Groceries", iconName = "ShoppingCart", colorHex = "#10B981", monthlyBudgetLimit = 500.0),
                CategoryEntity(name = "Shopping", iconName = "ShoppingBag", colorHex = "#3B82F6", monthlyBudgetLimit = 250.0),
                CategoryEntity(name = "Bills & Utilities", iconName = "Receipt", colorHex = "#8B5CF6", monthlyBudgetLimit = 400.0),
                CategoryEntity(name = "Salary & Income", iconName = "Payments", colorHex = "#10B981", monthlyBudgetLimit = 0.0, isIncomeCategory = true),
                CategoryEntity(name = "Transportation", iconName = "DirectionsCar", colorHex = "#F59E0B", monthlyBudgetLimit = 150.0),
                CategoryEntity(name = "Entertainment", iconName = "Movie", colorHex = "#EC4899", monthlyBudgetLimit = 100.0),
                CategoryEntity(name = "Health & Wellness", iconName = "LocalHospital", colorHex = "#14B8A6", monthlyBudgetLimit = 150.0),
                CategoryEntity(name = "Investment & Savings", iconName = "Savings", colorHex = "#6366F1", monthlyBudgetLimit = 0.0, isIncomeCategory = true),
                CategoryEntity(name = "Uncategorized", iconName = "Label", colorHex = "#64748B", monthlyBudgetLimit = 0.0)
            )
            categoryDao.insertCategories(defaultCategories)
        }

        // Seed default account if empty
        val currentAccounts = accounts.firstOrNull()
        if (currentAccounts.isNullOrEmpty()) {
            val primaryAccount = AccountEntity(
                name = "HDFC Bank Account",
                type = "CHECKING",
                balance = 2450.00,
                accountNumberLast4 = "4321",
                colorHex = "#2563EB",
                isDefault = true,
                senderKeywords = "VD-HDFCBK-T, AD-HDFCBK-S, HDFCBK"
            )
            val savingsAccount = AccountEntity(
                name = "Indian Bank Savings",
                type = "SAVINGS",
                balance = 8200.00,
                accountNumberLast4 = "9876",
                colorHex = "#059669",
                isDefault = false,
                senderKeywords = "BV-INDBNK-S, BZ-INDBNK-S, INDBNK"
            )
            val creditCard = AccountEntity(
                name = "Freedom Credit Card",
                type = "CREDIT_CARD",
                balance = -320.50,
                accountNumberLast4 = "1122",
                colorHex = "#7C3AED",
                isDefault = false,
                senderKeywords = "VISA, CHASE, AMEX"
            )

            val primaryId = accountDao.insertAccount(primaryAccount)
            val savingsId = accountDao.insertAccount(savingsAccount)
            val creditId = accountDao.insertAccount(creditCard)

            // Add sample starter transactions to showcase UI on first run
            val now = System.currentTimeMillis()
            val day = 86400000L

            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Monthly Salary Deposit",
                    amount = 3500.00,
                    type = "CREDIT",
                    accountId = primaryId,
                    categoryTag = "Salary & Income",
                    dateTimestamp = now - (2 * day),
                    source = "SMS_AUTO",
                    merchantOrSender = "Acme Corp Payroll",
                    isPendingReview = false,
                    rawSmsMessage = "Your A/C XX4321 credited with USD 3500.00 from Acme Corp Payroll"
                )
            )

            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Pending: Starbucks Coffee",
                    amount = 5.75,
                    type = "DEBIT",
                    accountId = primaryId,
                    categoryTag = "Food & Dining",
                    dateTimestamp = now - (1 * day),
                    source = "SMS_AUTO",
                    merchantOrSender = "Starbucks",
                    isPendingReview = true, // Prompt user to add title later!
                    rawSmsMessage = "USD 5.75 debited on A/C ending 4321 at Starbucks Cafe"
                )
            )

            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Supermarket Groceries",
                    amount = 84.20,
                    type = "DEBIT",
                    accountId = creditId,
                    categoryTag = "Groceries",
                    dateTimestamp = now - (3 * day),
                    source = "MANUAL",
                    merchantOrSender = "Trader Joes",
                    isPendingReview = false
                )
            )

            val electricTxId = transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Electric & Power Bill",
                    amount = 112.40,
                    type = "DEBIT",
                    accountId = primaryId,
                    categoryTag = "Bills & Utilities",
                    dateTimestamp = now - (5 * day),
                    source = "SMS_AUTO",
                    merchantOrSender = "City Power Utility",
                    isPendingReview = false,
                    rawSmsMessage = "A/C XX4321 debited for USD 112.40 to City Power Utility"
                )
            )

            val emiTxId = transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Home Loan EMI",
                    amount = 450.00,
                    type = "DEBIT",
                    accountId = primaryId,
                    categoryTag = "Bills & Utilities",
                    dateTimestamp = now,
                    source = "MANUAL",
                    merchantOrSender = "HDFC Bank Loan",
                    isPendingReview = false,
                    notes = "Monthly car/home EMI due on 4th"
                )
            )

            val netflixTxId = transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Netflix Subscription",
                    amount = 15.99,
                    type = "DEBIT",
                    accountId = creditId,
                    categoryTag = "Entertainment",
                    dateTimestamp = now - (7 * day),
                    source = "SMS_AUTO",
                    merchantOrSender = "Netflix",
                    isPendingReview = false,
                    rawSmsMessage = "A/C XX1122 debited for USD 15.99 to Netflix.com"
                )
            )

            val gymTxId = transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Gym Membership",
                    amount = 45.00,
                    type = "DEBIT",
                    accountId = primaryId,
                    categoryTag = "Health & Wellness",
                    dateTimestamp = now - (10 * day),
                    source = "MANUAL",
                    merchantOrSender = "Fitness Club",
                    isPendingReview = false
                )
            )

            // Demo bills, each with the sample payment above as its latest payment
            listOf(
                Triple(electricTxId, 5, "City Power Utility"),
                Triple(emiTxId, 4, "HDFC Bank Loan"),
                Triple(netflixTxId, 1, "Netflix"),
                Triple(gymTxId, 15, "Fitness Club")
            ).forEach { (txId, dueDay, payee) ->
                val payment = transactionDao.getTransactionById(txId) ?: return@forEach
                val billId = billDao.insertBill(
                    RecurringBillEntity(
                        name = payment.title,
                        amount = payment.amount,
                        type = payment.type,
                        accountId = payment.accountId,
                        categoryTag = payment.categoryTag,
                        frequency = BillFrequency.MONTHLY.name,
                        nextDueDate = BillSchedule.nextDueAfterPayment(payment.dateTimestamp, BillFrequency.MONTHLY, dueDay),
                        dueDay = dueDay,
                        matchPayee = payee,
                        lastPaidTimestamp = payment.dateTimestamp
                    )
                )
                transactionDao.updateTransaction(payment.copy(recurringBillId = billId))
            }
        }
    }

    suspend fun processParsedSmsTransaction(parsed: ParsedSmsTransaction): Boolean {
        val allAccounts = accountDao.getAllAccountsList()
        val senderCode = SmsParser.senderCode(parsed.senderAddress)

        // An SMS from a bank/card no account recognises still gets imported (to the default
        // account) and stays pending; the review flow asks which account it belongs to and
        // learns the sender for next time - see confirmSmsAccount.
        val targetAccount = SmsAccountMatcher.match(allAccounts, senderCode, parsed.accountLast4, parsed.rawMessage)
            ?: allAccounts.firstOrNull { it.isDefault }
            ?: allAccounts.firstOrNull()
            ?: return false

        // Generate preliminary title indicating it needs title set later
        val defaultTitle = if (!parsed.merchantOrSender.isNullOrBlank()) {
            "SMS: ${parsed.merchantOrSender}"
        } else {
            "New SMS ${if (parsed.type == "CREDIT") "Credit" else "Debit"}"
        }

        // If this exact merchant/sender has been categorized before, reuse that category instead
        // of the generic keyword-based guess - a user-confirmed choice for "Mr K Ghouse Sheriff"
        // is far more useful than a category derived from message text alone.
        val learnedCategory = parsed.merchantOrSender
            ?.takeIf { it.isNotBlank() }
            ?.let { transactionDao.getLastCategoryForMerchant(it) }

        val transaction = TransactionEntity(
            title = defaultTitle,
            amount = parsed.amount,
            type = parsed.type,
            accountId = targetAccount.id,
            categoryTag = learnedCategory ?: parsed.suggestedCategory,
            dateTimestamp = parsed.timestamp,
            source = "SMS_AUTO",
            rawSmsMessage = parsed.rawMessage,
            merchantOrSender = parsed.merchantOrSender,
            isPendingReview = true, // Flagged so user sets title later in app!
            notes = "Auto-imported from SMS",
            smsSender = senderCode,
            smsAccountLast4 = parsed.accountLast4
        )

        // The duplicate check runs inside the same transaction as the insert so a live SMS
        // broadcast and an inbox scan handling the same message can't both insert it.
        return db.withTransaction {
            if (transactionDao.checkDuplicateSms(parsed.rawMessage) > 0) return@withTransaction false

            val transactionId = transactionDao.insertTransaction(transaction)

            // Update balance
            val balanceDelta = if (parsed.type == "CREDIT") parsed.amount else -parsed.amount
            accountDao.updateBalance(targetAccount.id, balanceDelta)

            findBillPaidBySms(transaction)?.let { bill ->
                transactionDao.getTransactionById(transactionId)?.let { saved ->
                    transactionDao.updateTransaction(saved.copy(recurringBillId = bill.id, categoryTag = bill.categoryTag))
                }
                billDao.updateBill(advancedBill(bill, transaction.dateTimestamp))
            }
            true
        }
    }

    // --- Recurring bills ---

    fun paymentsForBill(billId: Long): Flow<List<TransactionEntity>> = transactionDao.getPaymentsForBill(billId)

    suspend fun addBill(bill: RecurringBillEntity): Long = billDao.insertBill(bill)

    /** Creates a bill from an existing ledger transaction, which becomes its first payment. */
    suspend fun addBillFromPayment(bill: RecurringBillEntity, payment: TransactionEntity): Long = db.withTransaction {
        val billId = billDao.insertBill(bill.copy(lastPaidTimestamp = payment.dateTimestamp))
        transactionDao.updateTransaction(payment.copy(recurringBillId = billId))
        billId
    }

    suspend fun updateBill(bill: RecurringBillEntity) = billDao.updateBill(bill)

    /** Deletes the bill only; its payments stay in the ledger as ordinary transactions. */
    suspend fun deleteBill(bill: RecurringBillEntity) {
        db.withTransaction {
            transactionDao.unlinkPaymentsFromBill(bill.id)
            billDao.deleteBill(bill)
        }
    }

    /**
     * Ledger transactions that look like this cycle's payment (same direction, amount within
     * 10%, from 15 days before the due date until now, not linked to another bill), best first:
     * exact amount and known payee rank above near matches.
     */
    suspend fun billPaymentCandidates(bill: RecurringBillEntity, now: Long = System.currentTimeMillis()): List<TransactionEntity> {
        val from = bill.nextDueDate - 15L * DAY_MS
        val to = maxOf(now, bill.nextDueDate + DAY_MS)
        return transactionDao.findBillPaymentCandidates(bill.type, bill.amount * 0.9, bill.amount * 1.1, from, to)
            .sortedWith(
                compareByDescending<TransactionEntity> { payeeMatches(bill, it) }
                    .thenBy { kotlin.math.abs(it.amount - bill.amount) }
                    .thenBy { kotlin.math.abs(it.dateTimestamp - bill.nextDueDate) }
            )
    }

    /**
     * Records this cycle's payment as a new ledger transaction (moving the account balance)
     * and advances the bill to its next due date.
     */
    suspend fun recordBillPayment(
        bill: RecurringBillEntity,
        amount: Double,
        accountId: Long,
        paidAt: Long
    ): BillActionResult = db.withTransaction {
        val payment = TransactionEntity(
            title = bill.name,
            amount = amount,
            type = bill.type,
            accountId = accountId,
            categoryTag = bill.categoryTag,
            dateTimestamp = paidAt,
            source = "RECURRING_PAID",
            isPendingReview = false,
            recurringBillId = bill.id
        )
        val paymentId = transactionDao.insertTransaction(payment)
        accountDao.updateBalance(accountId, if (bill.type == "CREDIT") amount else -amount)
        billDao.updateBill(advancedBill(bill, paidAt))
        BillActionResult(billBefore = bill, paymentId = paymentId, createdPayment = true)
    }

    /**
     * Marks the cycle paid by an existing ledger transaction (usually the bank SMS), so the
     * payment isn't counted twice. Remembers the payee so the next SMS payment links itself.
     */
    suspend fun linkBillPayment(bill: RecurringBillEntity, payment: TransactionEntity): BillActionResult = db.withTransaction {
        transactionDao.updateTransaction(payment.copy(recurringBillId = bill.id, categoryTag = bill.categoryTag))
        val payee = bill.matchPayee ?: payment.merchantOrSender?.takeIf { it.isNotBlank() }
        billDao.updateBill(advancedBill(bill, payment.dateTimestamp).copy(matchPayee = payee))
        BillActionResult(billBefore = bill, paymentId = payment.id, createdPayment = false, paymentBefore = payment)
    }

    /** Moves the bill to its next cycle without recording a payment. */
    suspend fun skipBillCycle(bill: RecurringBillEntity): BillActionResult {
        billDao.updateBill(bill.copy(nextDueDate = BillSchedule.advance(bill.nextDueDate, BillFrequency.from(bill.frequency), bill.dueDay)))
        return BillActionResult(billBefore = bill)
    }

    /** Reverses [recordBillPayment], [linkBillPayment] or [skipBillCycle]. */
    suspend fun undoBillAction(result: BillActionResult) {
        db.withTransaction {
            if (result.paymentId != null) {
                val payment = transactionDao.getTransactionById(result.paymentId)
                if (payment != null) {
                    if (result.createdPayment) {
                        transactionDao.deleteTransaction(payment)
                        accountDao.updateBalance(payment.accountId, if (payment.type == "CREDIT") -payment.amount else payment.amount)
                    } else {
                        transactionDao.updateTransaction(result.paymentBefore ?: payment.copy(recurringBillId = null))
                    }
                }
            }
            billDao.updateBill(result.billBefore)
        }
    }

    private fun advancedBill(bill: RecurringBillEntity, paidAt: Long) = bill.copy(
        nextDueDate = BillSchedule.advance(bill.nextDueDate, BillFrequency.from(bill.frequency), bill.dueDay),
        lastPaidTimestamp = paidAt
    )

    /**
     * An active bill an imported SMS pays: same direction, the exact amount, around the bill's
     * due date, and sent to the bill's payee (or a payee containing the bill's name).
     */
    private suspend fun findBillPaidBySms(tx: TransactionEntity): RecurringBillEntity? =
        billDao.getActiveBillsList().firstOrNull { bill ->
            bill.type == tx.type &&
                kotlin.math.abs(bill.amount - tx.amount) < 0.01 &&
                tx.dateTimestamp in (bill.nextDueDate - 10L * DAY_MS)..(bill.nextDueDate + 20L * DAY_MS) &&
                payeeMatches(bill, tx)
        }

    private fun payeeMatches(bill: RecurringBillEntity, tx: TransactionEntity): Boolean {
        val payee = tx.merchantOrSender?.lowercase() ?: return false
        bill.matchPayee?.let { if (payee == it.lowercase()) return true }
        // "Airtel" bill vs "AIRTEL PAYMENTS BANK" payee: any 4+ letter word of the bill name
        return bill.name.lowercase().split(Regex("[^a-z0-9]+")).any { it.length >= 4 && payee.contains(it) }
    }

    /**
     * Called when the user confirms which account an SMS transaction belongs to. If the SMS came
     * from a bank/card no account recognised, the chosen account learns its sender code (and its
     * last-4 digits, when the account has none saved) so later SMS from it match automatically.
     */
    suspend fun confirmSmsAccount(transaction: TransactionEntity, accountId: Long) {
        val allAccounts = accountDao.getAllAccountsList()
        if (!SmsAccountMatcher.needsAccountConfirmation(transaction, allAccounts)) return
        val account = allAccounts.firstOrNull { it.id == accountId } ?: return

        val keywords = account.senderKeywords?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty().toMutableList()
        val sender = transaction.smsSender
        if (sender != null && keywords.none { SmsParser.senderCode(it).equals(sender, ignoreCase = true) }) {
            keywords += sender
        }
        val last4 = account.accountNumberLast4?.takeIf { it.isNotBlank() } ?: transaction.smsAccountLast4
        accountDao.updateAccount(
            account.copy(
                senderKeywords = keywords.joinToString(", ").ifEmpty { null },
                accountNumberLast4 = last4
            )
        )
    }

    suspend fun addTransaction(transaction: TransactionEntity) {
        db.withTransaction {
            transactionDao.insertTransaction(transaction)
            val balanceDelta = if (transaction.type == "CREDIT") transaction.amount else -transaction.amount
            accountDao.updateBalance(transaction.accountId, balanceDelta)
        }
    }

    suspend fun updateTransaction(oldTransaction: TransactionEntity, newTransaction: TransactionEntity) {
        db.withTransaction {
            transactionDao.updateTransaction(newTransaction)

            // Revert old transaction effect on balance
            val oldDelta = if (oldTransaction.type == "CREDIT") -oldTransaction.amount else oldTransaction.amount
            accountDao.updateBalance(oldTransaction.accountId, oldDelta)

            // Apply new transaction effect
            val newDelta = if (newTransaction.type == "CREDIT") newTransaction.amount else -newTransaction.amount
            accountDao.updateBalance(newTransaction.accountId, newDelta)
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        db.withTransaction {
            // SMS-sourced transactions are dismissed (row kept, hidden from every query) rather
            // than hard-deleted: the row's rawSmsMessage is what checkDuplicateSms matches
            // against, so hard-deleting it would let the next inbox scan recreate the exact
            // transaction the user just declined.
            if (transaction.source == "SMS_AUTO" || transaction.source == "SIMULATED_SMS") {
                transactionDao.dismissTransaction(transaction.id)
            } else {
                transactionDao.deleteTransaction(transaction)
            }
            val revertDelta = if (transaction.type == "CREDIT") -transaction.amount else transaction.amount
            accountDao.updateBalance(transaction.accountId, revertDelta)
        }
    }

    suspend fun transferBetweenAccounts(
        fromAccountId: Long,
        toAccountId: Long,
        fromAccountName: String,
        toAccountName: String,
        amount: Double,
        notes: String?,
        dateTimestamp: Long = System.currentTimeMillis()
    ) {
        db.withTransaction {
            // 1. Create DEBIT transaction for source account
            val debitTx = TransactionEntity(
                title = "Transfer to $toAccountName",
                amount = amount,
                type = "DEBIT",
                accountId = fromAccountId,
                categoryTag = "Transfer",
                dateTimestamp = dateTimestamp,
                source = "MANUAL",
                notes = if (!notes.isNullOrBlank()) notes else "Transfer to $toAccountName"
            )
            transactionDao.insertTransaction(debitTx)
            accountDao.updateBalance(fromAccountId, -amount)

            // 2. Create CREDIT transaction for destination account
            val creditTx = TransactionEntity(
                title = "Transfer from $fromAccountName",
                amount = amount,
                type = "CREDIT",
                accountId = toAccountId,
                categoryTag = "Transfer",
                dateTimestamp = dateTimestamp,
                source = "MANUAL",
                notes = if (!notes.isNullOrBlank()) notes else "Transfer from $fromAccountName"
            )
            transactionDao.insertTransaction(creditTx)
            accountDao.updateBalance(toAccountId, amount)
        }
    }

    suspend fun addAccount(account: AccountEntity) {
        accountDao.insertAccount(account)
    }

    /**
     * Inserts an account as a brand-new row (id forced to 0 so Room autogenerates a fresh id
     * instead of REPLACE-ing/colliding with an existing row) and returns the generated id.
     * Used by backup restore, which supplies a balance that already reflects all its transactions.
     */
    suspend fun restoreAccount(account: AccountEntity): Long {
        return accountDao.insertAccount(account.copy(id = 0))
    }

    /**
     * Inserts a transaction as-is without touching account balances. Used by backup restore,
     * where the imported account balance already accounts for every imported transaction -
     * replaying addTransaction's balance delta here would double-count it.
     */
    suspend fun restoreTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction.copy(id = 0))
    }

    /**
     * Imports CSV rows (see DataExporter.parseCsvImport) by matching each row's account name
     * (case-insensitive) to an existing account, creating a new one if none matches. Unlike JSON
     * restore, CSV carries no per-account balance, so a new account starts at 0 and each imported
     * transaction's normal balance delta (via addTransaction) builds up the correct final balance.
     */
    suspend fun restoreCsvTransactions(rows: List<com.example.data.export.DataExporter.CsvTransactionRow>): Int {
        val accountIdByName = accountDao.getAllAccountsList()
            .associateTo(mutableMapOf()) { it.name.lowercase() to it.id }

        var imported = 0
        for (row in rows) {
            val accountId = accountIdByName.getOrPut(row.accountName.lowercase()) {
                accountDao.insertAccount(
                    AccountEntity(name = row.accountName, type = "CHECKING", balance = 0.0, colorHex = "#2563EB")
                )
            }
            addTransaction(
                TransactionEntity(
                    title = row.title,
                    amount = row.amount,
                    type = row.type,
                    accountId = accountId,
                    categoryTag = row.category,
                    dateTimestamp = row.dateTimestamp,
                    source = row.source,
                    merchantOrSender = row.merchantOrSender,
                    isPendingReview = false,
                    notes = row.notes
                )
            )
            imported++
        }
        return imported
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: AccountEntity) {
        accountDao.deleteAccount(account)
    }

    suspend fun addCategory(category: CategoryEntity) {
        categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    /**
     * Runs [block] inside a single database transaction. Used by multi-step operations that
     * span several repository calls (e.g. restoring a whole JSON backup) so a crash or error
     * partway through can't leave accounts/categories/transactions half-restored.
     */
    suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return db.withTransaction { block() }
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }
}

private const val DAY_MS = 24L * 60 * 60 * 1000

/** What a bill action changed, so it can be undone from the snackbar. */
data class BillActionResult(
    val billBefore: RecurringBillEntity,
    val paymentId: Long? = null,
    val createdPayment: Boolean = false,
    val paymentBefore: TransactionEntity? = null
)
