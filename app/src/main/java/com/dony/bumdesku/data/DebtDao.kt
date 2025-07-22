package com.dony.bumdesku.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {

    // --- Operasi untuk Utang (Payable) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayable(payable: Payable)

    @Update
    suspend fun updatePayable(payable: Payable)

    @Delete
    suspend fun deletePayable(payable: Payable)

    @Query("SELECT * FROM payables ORDER BY dueDate ASC")
    fun getAllPayables(): Flow<List<Payable>>

    // ✅ FUNGSI BARU UNTUK EDIT
    @Query("SELECT * FROM payables WHERE localId = :id")
    fun getPayableById(id: Int): Flow<Payable?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPayables(payables: List<Payable>)

    @Query("DELETE FROM payables")
    suspend fun deleteAllPayables()


    // --- Operasi untuk Piutang (Receivable) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceivable(receivable: Receivable)

    @Update
    suspend fun updateReceivable(receivable: Receivable)

    @Delete
    suspend fun deleteReceivable(receivable: Receivable)

    @Query("SELECT * FROM receivables ORDER BY dueDate ASC")
    fun getAllReceivables(): Flow<List<Receivable>>

    // ✅ FUNGSI BARU UNTUK EDIT
    @Query("SELECT * FROM receivables WHERE localId = :id")
    fun getReceivableById(id: Int): Flow<Receivable?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReceivables(receivables: List<Receivable>)

    @Query("DELETE FROM receivables")
    suspend fun deleteAllReceivables()
}
