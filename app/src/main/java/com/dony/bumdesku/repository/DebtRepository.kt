package com.dony.bumdesku.repository

import com.dony.bumdesku.data.DebtDao
import com.dony.bumdesku.data.Payable
import com.dony.bumdesku.data.Receivable
import kotlinx.coroutines.flow.Flow

class DebtRepository(private val debtDao: DebtDao) {

    // --- Alur Data untuk Utang (Payable) ---
    val allPayables: Flow<List<Payable>> = debtDao.getAllPayables()

    suspend fun insert(payable: Payable) {
        debtDao.insertPayable(payable)
    }

    suspend fun update(payable: Payable) {
        debtDao.updatePayable(payable)
    }

    suspend fun delete(payable: Payable) {
        debtDao.deletePayable(payable)
    }


    // --- Alur Data untuk Piutang (Receivable) ---
    val allReceivables: Flow<List<Receivable>> = debtDao.getAllReceivables()

    suspend fun insert(receivable: Receivable) {
        debtDao.insertReceivable(receivable)
    }

    suspend fun update(receivable: Receivable) {
        debtDao.updateReceivable(receivable)
    }

    suspend fun delete(receivable: Receivable) {
        debtDao.deleteReceivable(receivable)
    }
}