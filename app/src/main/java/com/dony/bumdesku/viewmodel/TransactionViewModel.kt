package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.DashboardData
import com.dony.bumdesku.data.Transaction
import com.dony.bumdesku.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionViewModel(private val repository: TransactionRepository) : ViewModel() {

    val allTransactions: Flow<List<Transaction>> = repository.allTransactions

    // FUNGSI BARU: Menggabungkan data pemasukan dan pengeluaran menjadi satu
    val dashboardData: StateFlow<DashboardData> =
        combine(
            repository.getTotalIncome(),
            repository.getTotalExpenses()
        ) { income, expenses ->
            val totalIncome = income ?: 0.0
            val totalExpenses = expenses ?: 0.0
            val finalBalance = totalIncome - totalExpenses
            DashboardData(
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                finalBalance = finalBalance
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardData() // Nilai awal sebelum data dari database datang
        )

    fun getTransactionById(id: Int): Flow<Transaction?> {
        return repository.getTransactionById(id)
    }

    fun insert(transaction: Transaction) = viewModelScope.launch {
        repository.insert(transaction)
    }

    fun update(transaction: Transaction) = viewModelScope.launch {
        repository.update(transaction)
    }

    fun delete(transaction: Transaction) = viewModelScope.launch {
        repository.delete(transaction)
    }
}