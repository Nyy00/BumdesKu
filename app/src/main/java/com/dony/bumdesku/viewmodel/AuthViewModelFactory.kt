package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dony.bumdesku.repository.* // Import semua repositori

class AuthViewModelFactory(
    private val unitUsahaRepository: UnitUsahaRepository,
    private val transactionRepository: TransactionRepository,
    private val assetRepository: AssetRepository,
    private val posRepository: PosRepository,
    private val accountRepository: AccountRepository,
    private val debtRepository: DebtRepository // ✅ TAMBAHKAN PARAMETER INI
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(
                unitUsahaRepository,
                transactionRepository,
                assetRepository,
                posRepository,
                accountRepository,
                debtRepository // ✅ BERIKAN REPOSITORY SAAT MEMBUAT VIEWMODEL
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}