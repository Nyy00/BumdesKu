package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.Transaction
import com.dony.bumdesku.repository.TransactionRepository
import kotlinx.coroutines.launch

class TransactionViewModel(private val repository: TransactionRepository) : ViewModel() {

    // Expose (sediakan) Flow dari semua transaksi agar bisa diamati oleh UI
    val allTransactions = repository.allTransactions

    // Fungsi untuk menyisipkan data baru
    // viewModelScope akan menjalankan ini di background thread secara otomatis
    fun insert(transaction: Transaction) = viewModelScope.launch {
        repository.insert(transaction)
    }

    // Fungsi untuk memperbarui data
    fun update(transaction: Transaction) = viewModelScope.launch {
        repository.update(transaction)
    }

    // Fungsi untuk menghapus data
    fun delete(transaction: Transaction) = viewModelScope.launch {
        repository.delete(transaction)
    }
}