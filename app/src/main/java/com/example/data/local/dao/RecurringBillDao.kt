package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.RecurringBillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringBillDao {
    @Query("SELECT * FROM recurring_bills ORDER BY isActive DESC, nextDueDate ASC")
    fun getAllBills(): Flow<List<RecurringBillEntity>>

    @Query("SELECT * FROM recurring_bills WHERE isActive = 1 ORDER BY nextDueDate ASC")
    suspend fun getActiveBillsList(): List<RecurringBillEntity>

    @Query("SELECT * FROM recurring_bills")
    suspend fun getAllBillsList(): List<RecurringBillEntity>

    @Query("SELECT * FROM recurring_bills WHERE id = :id")
    suspend fun getBillById(id: Long): RecurringBillEntity?

    @Insert
    suspend fun insertBill(bill: RecurringBillEntity): Long

    @Update
    suspend fun updateBill(bill: RecurringBillEntity)

    @Delete
    suspend fun deleteBill(bill: RecurringBillEntity)
}
