package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.Account
import com.dony.bumdesku.data.AccountCategory
import com.dony.bumdesku.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class AccountViewModel(private val repository: AccountRepository) : ViewModel() {

    val allAccounts: Flow<List<Account>> = repository.allAccounts

    init {
        // Panggil fungsi untuk membuat akun default saat ViewModel dibuat
        createDefaultAccountsIfEmpty()
    }

    private fun createDefaultAccountsIfEmpty() {
        viewModelScope.launch {
            // Cek apakah sudah ada akun di database
            val existingAccounts = repository.allAccounts.first()
            if (existingAccounts.isEmpty()) {
                // Jika kosong, buat akun-akun standar
                val defaultAccounts = listOf(
                    // --- ASET LANCAR (Awalan 11x) ---
                    Account(accountNumber = "111", accountName = "Kas Tunai", category = AccountCategory.ASET),
                    Account(accountNumber = "112", accountName = "Bank", category = AccountCategory.ASET),
                    Account(accountNumber = "113", accountName = "Piutang Usaha", category = AccountCategory.ASET),

                    // --- ASET TETAP (Awalan 12x) ---
                    Account(accountNumber = "121", accountName = "Peralatan", category = AccountCategory.ASET),

                    // --- KEWAJIBAN LANCAR (Awalan 21x) ---
                    Account(accountNumber = "211", accountName = "Utang Usaha", category = AccountCategory.KEWAJIBAN),

                    // --- MODAL (Awalan 3xx) ---
                    Account(accountNumber = "311", accountName = "Modal Disetor", category = AccountCategory.MODAL),
                    Account(accountNumber = "312", accountName = "Prive", category = AccountCategory.MODAL),


                    // --- PENDAPATAN (Awalan 4xx) ---
                    Account(accountNumber = "411", accountName = "Pendapatan Jasa", category = AccountCategory.PENDAPATAN),
                    Account(accountNumber = "412", accountName = "Pendapatan Sewa", category = AccountCategory.PENDAPATAN),

                    // --- BEBAN (Awalan 5xx) ---
                    Account(accountNumber = "511", accountName = "Beban Gaji", category = AccountCategory.BEBAN),
                    Account(accountNumber = "512", accountName = "Beban Listrik & Air", category = AccountCategory.BEBAN)
                )

                // Simpan semua akun default ke database
                defaultAccounts.forEach { insert(it) }
            }
        }
    }

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