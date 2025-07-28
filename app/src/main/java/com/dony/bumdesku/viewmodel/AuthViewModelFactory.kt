package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dony.bumdesku.repository.*

class AuthViewModelFactory(
    private val unitUsahaRepository: UnitUsahaRepository,
    private val transactionRepository: TransactionRepository,
    private val assetRepository: AssetRepository,
    private val posRepository: PosRepository,
    private val accountRepository: AccountRepository,
    private val debtRepository: DebtRepository,
    private val agriRepository: AgriRepository
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
                debtRepository,
                agriRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}