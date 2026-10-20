package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isDismissed = 0 ORDER BY dateTimestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND isDismissed = 0 ORDER BY dateTimestamp DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isPendingReview = 1 AND isDismissed = 0 ORDER BY dateTimestamp DESC")
    fun getPendingReviewTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE recurringBillId = :billId AND isDismissed = 0 ORDER BY dateTimestamp DESC")
    fun getPaymentsForBill(billId: Long): Flow<List<TransactionEntity>>

    // Ledger rows that could be a bill's payment: same direction, amount within the tolerance,
    // inside the date window and not already linked to a bill.
    @Query(
        "SELECT * FROM transactions WHERE isDismissed = 0 AND recurringBillId IS NULL AND type = :type " +
            "AND amount BETWEEN :minAmount AND :maxAmount AND dateTimestamp BETWEEN :fromMillis AND :toMillis " +
            "ORDER BY dateTimestamp DESC"
    )
    suspend fun findBillPaymentCandidates(
        type: String,
        minAmount: Double,
        maxAmount: Double,
        fromMillis: Long,
        toMillis: Long
    ): List<TransactionEntity>

    @Query("UPDATE transactions SET recurringBillId = NULL WHERE recurringBillId = :billId")
    suspend fun unlinkPaymentsFromBill(billId: Long)

    // Powers merchant-based auto-categorization: once the user has confirmed a category for a
    // given SMS sender/merchant, the next transaction from that same merchant is pre-tagged with
    // it instead of falling back to the generic keyword-based guess (or "Uncategorized").
    @Query(
        "SELECT categoryTag FROM transactions WHERE merchantOrSender = :merchant COLLATE NOCASE " +
            "AND isPendingReview = 0 AND isDismissed = 0 ORDER BY dateTimestamp DESC LIMIT 1"
    )
    suspend fun getLastCategoryForMerchant(merchant: String): String?

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("UPDATE transactions SET isDismissed = 1 WHERE id = :id")
    suspend fun dismissTransaction(id: Long)

    // Matches on the SMS body text alone, not dateTimestamp: a manual inbox scan reads the
    // provider's Telephony.Sms.DATE column, which is a different clock than the
    // SmsMessage.timestampMillis captured by the live broadcast receiver - the two can differ
    // by seconds for the exact same message, so requiring both to match let a reviewed
    // transaction get re-imported as a duplicate the next time "Scan & Sync SMS Inbox" ran.
    @Query("SELECT COUNT(*) FROM transactions WHERE rawSmsMessage = :rawSms")
    suspend fun checkDuplicateSms(rawSms: String): Int
}
