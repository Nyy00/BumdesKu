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

    // --- FUNGSI BARU UNTUK DASHBOARD & LAPORAN ---

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :transactionType")
    fun getTotalAmountByType(transactionType: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :transactionType AND date BETWEEN :startDate AND :endDate")
    suspend fun getAmountByTypeAndDateRange(transactionType: String, startDate: Long, endDate: Long): Double?

    // Fungsi ini untuk mengambil daftar transaksi dalam rentang tanggal
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
}