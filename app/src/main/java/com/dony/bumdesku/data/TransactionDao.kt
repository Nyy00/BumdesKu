package com.dony.bumdesku.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE localId = :id")
    fun getTransactionById(id: Int): Flow<Transaction?>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    // ✅ --- QUERY-QUERY INI WAJIB ADA UNTUK FILTER LAPORAN ---
    @Query("SELECT SUM(amount) FROM transactions WHERE creditAccountId IN (:accountIds) AND date BETWEEN :startDate AND :endDate")
    suspend fun getCreditTotalByDateRange(accountIds: List<String>, startDate: Long, endDate: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE debitAccountId IN (:accountIds) AND date BETWEEN :startDate AND :endDate")
    suspend fun getDebitTotalByDateRange(accountIds: List<String>, startDate: Long, endDate: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE creditAccountId IN (:accountIds) AND unitUsahaId = :unitUsahaId AND date BETWEEN :startDate AND :endDate")
    suspend fun getCreditTotalByDateRangeAndUnit(accountIds: List<String>, unitUsahaId: String, startDate: Long, endDate: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE debitAccountId IN (:accountIds) AND unitUsahaId = :unitUsahaId AND date BETWEEN :startDate AND :endDate")
    suspend fun getDebitTotalByDateRangeAndUnit(accountIds: List<String>, unitUsahaId: String, startDate: Long, endDate: Long): Double?

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE unitUsahaId = :unitUsahaId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateAndUnit(unitUsahaId: String, startDate: Long, endDate: Long): Flow<List<Transaction>>
}