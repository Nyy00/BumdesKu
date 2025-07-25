package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dony.bumdesku.repository.AccountRepository
import com.dony.bumdesku.repository.DebtRepository
import com.dony.bumdesku.repository.TransactionRepository
import com.dony.bumdesku.repository.UnitUsahaRepository // ✅ 1. Tambahkan import ini

class DebtViewModelFactory(
    private val debtRepository: DebtRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val unitUsahaRepository: UnitUsahaRepository // ✅ 2. Tambahkan parameter ini
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DebtViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // ✅ 3. Berikan semua repository saat membuat ViewModel
            return DebtViewModel(
                debtRepository,
                transactionRepository,
                accountRepository,
                unitUsahaRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}