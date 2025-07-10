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
}