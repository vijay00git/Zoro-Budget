package com.example.data.local

import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.sms.ParsedSmsTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class BudgetRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {
    val accounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val transactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val pendingTransactions: Flow<List<TransactionEntity>> = transactionDao.getPendingReviewTransactions()
    val categories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun initializeDefaultDataIfNeeded() {
        // Seed default categories if empty
        if (categoryDao.getCategoryCount() == 0) {
            val defaultCategories = listOf(
                CategoryEntity(name = "Food & Dining", iconName = "Fastfood", colorHex = "#EF4444", monthlyBudgetLimit = 350.0),
                CategoryEntity(name = "Groceries", iconName = "ShoppingCart", colorHex = "#10B981", monthlyBudgetLimit = 500.0),
                CategoryEntity(name = "Shopping", iconName = "ShoppingBag", colorHex = "#3B82F6", monthlyBudgetLimit = 250.0),
                CategoryEntity(name = "Bills & Utilities", iconName = "Receipt", colorHex = "#8B5CF6", monthlyBudgetLimit = 400.0),
                CategoryEntity(name = "Salary & Income", iconName = "Payments", colorHex = "#10B981", monthlyBudgetLimit = 0.0),
                CategoryEntity(name = "Transportation", iconName = "DirectionsCar", colorHex = "#F59E0B", monthlyBudgetLimit = 150.0),
                CategoryEntity(name = "Entertainment", iconName = "Movie", colorHex = "#EC4899", monthlyBudgetLimit = 100.0),
                CategoryEntity(name = "Health & Wellness", iconName = "LocalHospital", colorHex = "#14B8A6", monthlyBudgetLimit = 150.0),
                CategoryEntity(name = "Investment & Savings", iconName = "Savings", colorHex = "#6366F1", monthlyBudgetLimit = 0.0),
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

            transactionDao.insertTransaction(
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
                    rawSmsMessage = "A/C XX4321 debited for USD 112.40 to City Power Utility",
                    isRecurring = true,
                    recurringFrequency = "MONTHLY",
                    recurringDueDate = 5
                )
            )

            transactionDao.insertTransaction(
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
                    notes = "Monthly car/home EMI due on 4th",
                    isRecurring = true,
                    recurringFrequency = "MONTHLY",
                    recurringDueDate = 4
                )
            )

            transactionDao.insertTransaction(
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
                    rawSmsMessage = "A/C XX1122 debited for USD 15.99 to Netflix.com",
                    isRecurring = true,
                    recurringFrequency = "MONTHLY",
                    recurringDueDate = 1
                )
            )

            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Gym Membership",
                    amount = 45.00,
                    type = "DEBIT",
                    accountId = primaryId,
                    categoryTag = "Health & Wellness",
                    dateTimestamp = now - (10 * day),
                    source = "MANUAL",
                    merchantOrSender = "Fitness Club",
                    isPendingReview = false,
                    isRecurring = true,
                    recurringFrequency = "MONTHLY",
                    recurringDueDate = 15
                )
            )
        }
    }

    suspend fun processParsedSmsTransaction(parsed: ParsedSmsTransaction): Boolean {
        // Check duplicate
        val existingCount = transactionDao.checkDuplicateSms(parsed.rawMessage, parsed.timestamp)
        if (existingCount > 0) return false

        val allAccounts = accountDao.getAllAccountsList()
        var matchedAccount: AccountEntity? = null

        val sender = parsed.senderAddress?.lowercase()?.trim()
        val rawLower = parsed.rawMessage.lowercase()

        // 1. First Priority: Match by SMS Bank Sender / Contact Keywords
        matchedAccount = allAccounts.find { acc ->
            val keywords = acc.senderKeywords?.lowercase()?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            if (!keywords.isNullOrEmpty()) {
                keywords.any { kw ->
                    (sender != null && (sender.contains(kw) || kw.contains(sender))) || rawLower.contains(kw)
                }
            } else false
        }

        // 2. Second Priority: Match by account last 4 digits
        if (matchedAccount == null && !parsed.accountLast4.isNullOrEmpty()) {
            matchedAccount = allAccounts.find { it.accountNumberLast4 == parsed.accountLast4 }
        }

        val targetAccount = matchedAccount ?: allAccounts.firstOrNull() ?: return false

        // Generate preliminary title indicating it needs title set later
        val defaultTitle = if (!parsed.merchantOrSender.isNullOrBlank()) {
            "SMS: ${parsed.merchantOrSender}"
        } else {
            "New SMS ${if (parsed.type == "CREDIT") "Credit" else "Debit"}"
        }

        val transaction = TransactionEntity(
            title = defaultTitle,
            amount = parsed.amount,
            type = parsed.type,
            accountId = targetAccount.id,
            categoryTag = parsed.suggestedCategory,
            dateTimestamp = parsed.timestamp,
            source = "SMS_AUTO",
            rawSmsMessage = parsed.rawMessage,
            merchantOrSender = parsed.merchantOrSender,
            isPendingReview = true, // Flagged so user sets title later in app!
            notes = "Auto-imported from SMS"
        )

        transactionDao.insertTransaction(transaction)

        // Update balance
        val balanceDelta = if (parsed.type == "CREDIT") parsed.amount else -parsed.amount
        accountDao.updateBalance(targetAccount.id, balanceDelta)

        return true
    }

    suspend fun addTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
        val balanceDelta = if (transaction.type == "CREDIT") transaction.amount else -transaction.amount
        accountDao.updateBalance(transaction.accountId, balanceDelta)
    }

    suspend fun updateTransaction(oldTransaction: TransactionEntity, newTransaction: TransactionEntity) {
        transactionDao.updateTransaction(newTransaction)

        // Revert old transaction effect on balance
        val oldDelta = if (oldTransaction.type == "CREDIT") -oldTransaction.amount else oldTransaction.amount
        accountDao.updateBalance(oldTransaction.accountId, oldDelta)

        // Apply new transaction effect
        val newDelta = if (newTransaction.type == "CREDIT") newTransaction.amount else -newTransaction.amount
        accountDao.updateBalance(newTransaction.accountId, newDelta)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
        val revertDelta = if (transaction.type == "CREDIT") -transaction.amount else transaction.amount
        accountDao.updateBalance(transaction.accountId, revertDelta)
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

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }
}
