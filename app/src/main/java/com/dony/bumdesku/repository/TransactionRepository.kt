package com.dony.bumdesku.repository

import com.dony.bumdesku.data.Transaction
import com.dony.bumdesku.data.TransactionDao
import kotlinx.coroutines.flow.Flow

// Repository membutuhkan DAO sebagai dependency untuk mengakses database
class TransactionRepository(private val transactionDao: TransactionDao) {

    // Mengambil semua data transaksi dalam bentuk Flow dari DAO
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    // Mengambil satu transaksi berdasarkan ID
    fun getTransactionById(id: Int): Flow<Transaction?> {
        return transactionDao.getTransactionById(id)
    }

    // Fungsi suspend untuk menyisipkan data, yang memanggil fungsi DAO
    suspend fun insert(transaction: Transaction) {
        transactionDao.insert(transaction)
    }

    // Fungsi suspend untuk memperbarui data
    suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    // Fungsi suspend untuk menghapus data
    suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
    }
}