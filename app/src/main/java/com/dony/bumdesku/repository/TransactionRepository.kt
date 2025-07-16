package com.dony.bumdesku.repository

import com.dony.bumdesku.data.Transaction
import com.dony.bumdesku.data.TransactionDao
import com.dony.bumdesku.data.UnitUsahaDao
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val unitUsahaDao: UnitUsahaDao // Meskipun tidak digunakan di sini, tetap dibutuhkan untuk factory
) {

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    // --- FUNGSI UNTUK DASHBOARD ---
    fun getTotalIncome(): Flow<Double?> {
        return transactionDao.getTotalAmountByType("PEMASUKAN")
    }

    fun getTotalExpenses(): Flow<Double?> {
        return transactionDao.getTotalAmountByType("PENGELUARAN")
    }

    // --- FUNGSI UNTUK CRUD ---
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

    // --- FUNGSI UNTUK LAPORAN ---
    suspend fun getReportData(startDate: Long, endDate: Long, unitUsahaId: String?): Pair<Double, Double> {
        val income: Double
        val expenses: Double

        if (unitUsahaId == null) {
            // Jika tidak ada unit usaha dipilih, ambil semua data
            income = transactionDao.getAmountByTypeAndDateRange("PEMASUKAN", startDate, endDate) ?: 0.0
            expenses = transactionDao.getAmountByTypeAndDateRange("PENGELUARAN", startDate, endDate) ?: 0.0
        } else {
            // Jika ada unit usaha dipilih, gunakan query yang baru
            income = transactionDao.getAmountByTypeDateAndUnit("PEMASUKAN", unitUsahaId, startDate, endDate) ?: 0.0
            expenses = transactionDao.getAmountByTypeDateAndUnit("PENGELUARAN", unitUsahaId, startDate, endDate) ?: 0.0
        }
        return Pair(income, expenses)
    }

    // Tambahkan fungsi baru untuk mengambil transaksi yang difilter
    fun getFilteredTransactions(startDate: Long, endDate: Long, unitUsahaId: String?): Flow<List<Transaction>> {
        return if (unitUsahaId == null) {
            transactionDao.getTransactionsByDateRange(startDate, endDate)
        } else {
            transactionDao.getTransactionsByDateAndUnit(unitUsahaId, startDate, endDate)
        }
    }

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate)
    }
}