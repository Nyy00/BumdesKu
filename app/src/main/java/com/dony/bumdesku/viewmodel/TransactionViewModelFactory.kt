package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dony.bumdesku.repository.TransactionRepository

class TransactionViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Cek apakah class yang diminta adalah TransactionViewModel
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            // Jika ya, buat dan kembalikan instance-nya, berikan repository
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(repository) as T
        }
        // Jika tidak, lemparkan error
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}