package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.Payable
import com.dony.bumdesku.data.Receivable
import com.dony.bumdesku.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

class DebtViewModel(private val repository: DebtRepository) : ViewModel() {

    // --- Data Utang (Payable) ---
    val allPayables: Flow<List<Payable>> = repository.allPayables

    fun insert(payable: Payable) = viewModelScope.launch {
        val newPayable = payable.copy(id = UUID.randomUUID().toString())
        repository.insert(newPayable)
    }

    fun update(payable: Payable) = viewModelScope.launch {
        repository.update(payable)
    }

    fun delete(payable: Payable) = viewModelScope.launch {
        repository.delete(payable)
    }


    // --- Data Piutang (Receivable) ---
    val allReceivables: Flow<List<Receivable>> = repository.allReceivables

    fun insert(receivable: Receivable) = viewModelScope.launch {
        val newReceivable = receivable.copy(id = UUID.randomUUID().toString())
        repository.insert(newReceivable)
    }

    fun update(receivable: Receivable) = viewModelScope.launch {
        repository.update(receivable)
    }

    fun delete(receivable: Receivable) = viewModelScope.launch {
        repository.delete(receivable)
    }
}


// --- Factory untuk membuat DebtViewModel ---
class DebtViewModelFactory(private val repository: DebtRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DebtViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DebtViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}