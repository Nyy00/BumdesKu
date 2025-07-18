package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dony.bumdesku.repository.AccountRepository
import com.dony.bumdesku.repository.DebtRepository
import com.dony.bumdesku.repository.TransactionRepository

class DebtViewModelFactory(
    private val debtRepository: DebtRepository,
    // Tambahkan repository baru
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DebtViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Berikan semua repository saat membuat ViewModel
            return DebtViewModel(debtRepository, transactionRepository, accountRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}