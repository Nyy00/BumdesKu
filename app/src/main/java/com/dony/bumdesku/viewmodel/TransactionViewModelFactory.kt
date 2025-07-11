package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dony.bumdesku.repository.TransactionRepository
import com.dony.bumdesku.repository.UnitUsahaRepository // Import baru

// Tambahkan unitUsahaRepository di constructor
class TransactionViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val unitUsahaRepository: UnitUsahaRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Berikan kedua repository saat membuat ViewModel
            return TransactionViewModel(transactionRepository, unitUsahaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}