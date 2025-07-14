package com.dony.bumdesku.repository

import com.dony.bumdesku.data.Transaction
import com.dony.bumdesku.data.TransactionDao
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionById(id: Int): Flow<Transaction?> {
        return transactionDao.getTransactionById(id)
    }

    suspend fun insert(transaction: Transaction) {
        transactionDao.insert(transaction)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
    }

    // --- FUNGSI BARU UNTUK DASHBOARD & LAPORAN ---

    fun getTotalIncome(): Flow<Double?> {
        return transactionDao.getTotalAmountByType("PEMASUKAN")
    }

    fun getTotalExpenses(): Flow<Double?> {
        return transactionDao.getTotalAmountByType("PENGELUARAN")
    }

    suspend fun getReportData(startDate: Long, endDate: Long): Pair<Double, Double> {
        val income = transactionDao.getAmountByTypeAndDateRange("PEMASUKAN", startDate, endDate) ?: 0.0
        val expenses = transactionDao.getAmountByTypeAndDateRange("PENGELUARAN", startDate, endDate) ?: 0.0
        return Pair(income, expenses)
    }

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate)
    }
}