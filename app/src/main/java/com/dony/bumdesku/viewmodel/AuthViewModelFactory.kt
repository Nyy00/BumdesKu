package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dony.bumdesku.repository.AccountRepository
import com.dony.bumdesku.repository.AssetRepository
import com.dony.bumdesku.repository.PosRepository
import com.dony.bumdesku.repository.TransactionRepository
import com.dony.bumdesku.repository.UnitUsahaRepository

class AuthViewModelFactory(
    private val unitUsahaRepository: UnitUsahaRepository,
    private val transactionRepository: TransactionRepository,
    private val assetRepository: AssetRepository,
    private val posRepository: PosRepository,
    private val accountRepository: AccountRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(
                unitUsahaRepository,
                transactionRepository,
                assetRepository,
                posRepository,
                accountRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}