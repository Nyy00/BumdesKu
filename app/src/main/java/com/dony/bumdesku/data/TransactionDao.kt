package com.dony.bumdesku.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao // Anotasi wajib untuk menandakan ini adalah DAO
interface TransactionDao {

    // Menyisipkan satu transaksi. Jika ada konflik (misal: id sama), ganti dengan yang baru.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    // Memperbarui data transaksi yang sudah ada
    @Update
    suspend fun update(transaction: Transaction)

    // Menghapus data transaksi
    @Delete
    suspend fun delete(transaction: Transaction)

    // Mengambil satu transaksi berdasarkan ID-nya
    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Int): Flow<Transaction?>

    // Mengambil semua transaksi dan mengurutkannya dari yang terbaru
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    // FUNGSI BARU: Menjumlahkan semua nominal berdasarkan tipe transaksi
    @Query("SELECT SUM(amount) FROM transactions WHERE type = :transactionType")
    fun getTotalAmountByType(transactionType: String): Flow<Double?>

    // FUNGSI BARU: Menjumlahkan nominal berdasarkan tipe dan rentang tanggal
    @Query("SELECT SUM(amount) FROM transactions WHERE type = :transactionType AND date BETWEEN :startDate AND :endDate")
    suspend fun getAmountByTypeAndDateRange(transactionType: String, startDate: Long, endDate: Long): Double?
}