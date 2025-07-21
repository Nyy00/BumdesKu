package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.Account
import com.dony.bumdesku.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

class AccountViewModel(private val repository: AccountRepository) : ViewModel() {

    val allAccounts: Flow<List<Account>> = repository.allAccounts

    // [PENGHAPUSAN] Seluruh blok init dan fungsi createDefaultAccountsIfEmpty() dihapus.

    fun insert(account: Account) = viewModelScope.launch {
        // Berikan ID unik sebelum menyimpan
        val newAccount = account.copy(id = UUID.randomUUID().toString())
        repository.insert(newAccount)
    }

    fun update(account: Account) = viewModelScope.launch {
        repository.update(account)
    }

    fun delete(account: Account) = viewModelScope.launch {
        repository.delete(account)
    }
}

class AccountViewModelFactory(private val repository: AccountRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}