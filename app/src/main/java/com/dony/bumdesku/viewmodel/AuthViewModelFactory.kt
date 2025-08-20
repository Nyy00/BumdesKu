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
    private val agriRepository: AgriRepository,
    private val agriCycleRepository: AgriCycleRepository,
    private val fixedAssetRepository: FixedAssetRepository,
    private val rentalRepository: RentalRepository,
    private val customerRepository: CustomerRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(
                unitUsahaRepository,
                transactionRepository,
                assetRepository,
                posRepository,
                accountRepository,
                debtRepository,
                agriRepository,
                agriCycleRepository,
                fixedAssetRepository,
                rentalRepository,
                customerRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}