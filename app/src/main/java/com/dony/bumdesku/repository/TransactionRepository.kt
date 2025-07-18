package com.dony.bumdesku.repository

import com.dony.bumdesku.data.Transaction
import com.dony.bumdesku.data.TransactionDao
import com.dony.bumdesku.data.UnitUsahaDao
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val unitUsahaDao: UnitUsahaDao
) {

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun insert(transaction: Transaction) {
        transactionDao.insert(transaction)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
    }

    fun getTransactionById(id: Int): Flow<Transaction?> {
        return transactionDao.getTransactionById(id)
    }

    suspend fun lockTransactionsUpTo(date: Long) {
        transactionDao.lockTransactionsUpTo(date)
    }

    // ✅ --- FUNGSI-FUNGSI INI WAJIB ADA UNTUK VIEWMODEL ---
    suspend fun getReportData(
        startDate: Long,
        endDate: Long,
        unitUsahaId: String?,
        pendapatanAccountIds: List<String>,
        bebanAccountIds: List<String>
    ): Pair<Double, Double> {
        val income: Double
        val expenses: Double

        if (unitUsahaId == null) {
            income = transactionDao.getCreditTotalByDateRange(pendapatanAccountIds, startDate, endDate) ?: 0.0
            expenses = transactionDao.getDebitTotalByDateRange(bebanAccountIds, startDate, endDate) ?: 0.0
        } else {
            income = transactionDao.getCreditTotalByDateRangeAndUnit(pendapatanAccountIds, unitUsahaId, startDate, endDate) ?: 0.0
            expenses = transactionDao.getDebitTotalByDateRangeAndUnit(bebanAccountIds, unitUsahaId, startDate, endDate) ?: 0.0
        }
        return Pair(income, expenses)
    }

    fun getFilteredTransactions(startDate: Long, endDate: Long, unitUsahaId: String?): Flow<List<Transaction>> {
        return if (unitUsahaId == null) {
            transactionDao.getTransactionsByDateRange(startDate, endDate)
        } else {
            transactionDao.getTransactionsByDateAndUnit(unitUsahaId, startDate, endDate)
        }
    }
}