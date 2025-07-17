package com.dony.bumdesku.repository

import com.dony.bumdesku.data.Account
import com.dony.bumdesku.data.Transaction
import com.dony.bumdesku.data.TransactionDao
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    // Kita tidak butuh UnitUsahaDao lagi di sini, tapi biarkan untuk konsistensi Factory
    private val unitUsahaDao: com.dony.bumdesku.data.UnitUsahaDao
) {

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

    // ✅ SEMUA FUNGSI LAMA (getTotalIncome, getTotalExpenses, dll) SUDAH DIHAPUS
    //    KARENA LOGIKANYA SUDAH PINDAH KE VIEWMODEL
}