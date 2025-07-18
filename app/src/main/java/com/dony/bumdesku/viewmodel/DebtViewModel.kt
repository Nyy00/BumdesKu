package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.*
import com.dony.bumdesku.repository.AccountRepository
import com.dony.bumdesku.repository.DebtRepository
import com.dony.bumdesku.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class DebtViewModel(
    private val repository: DebtRepository,
    // Tambahkan repository baru
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

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

    fun markPayableAsPaid(payable: Payable) = viewModelScope.launch {
        // 1. Update status utang menjadi lunas
        repository.update(payable.copy(isPaid = true))

        // 2. Buat jurnal otomatis
        val allAccounts = accountRepository.allAccounts.first()
        val kasAccount = allAccounts.find { it.accountNumber == "111" } // Asumsi Kas Tunai
        val utangAccount = allAccounts.find { it.accountNumber == "211" } // Asumsi Utang Usaha

        if (kasAccount != null && utangAccount != null) {
            val newTransaction = Transaction(
                description = "Pelunasan utang: ${payable.description}",
                amount = payable.amount,
                date = System.currentTimeMillis(),
                debitAccountId = utangAccount.id, // Utang (debit)
                creditAccountId = kasAccount.id, // Kas (kredit)
                debitAccountName = utangAccount.accountName,
                creditAccountName = kasAccount.accountName
            )
            transactionRepository.insert(newTransaction.copy(id = UUID.randomUUID().toString()))
        }
    }

    fun markReceivableAsPaid(receivable: Receivable) = viewModelScope.launch {
        // 1. Update status piutang menjadi lunas
        repository.update(receivable.copy(isPaid = true))

        // 2. Buat jurnal otomatis
        val allAccounts = accountRepository.allAccounts.first()
        val kasAccount = allAccounts.find { it.accountNumber == "111" } // Asumsi Kas Tunai
        val piutangAccount = allAccounts.find { it.accountNumber == "113" } // Asumsi Piutang Usaha

        if (kasAccount != null && piutangAccount != null) {
            val newTransaction = Transaction(
                description = "Pelunasan piutang: ${receivable.description}",
                amount = receivable.amount,
                date = System.currentTimeMillis(),
                debitAccountId = kasAccount.id, // Kas (debit)
                creditAccountId = piutangAccount.id, // Piutang (kredit)
                debitAccountName = kasAccount.accountName,
                creditAccountName = piutangAccount.accountName
            )
            transactionRepository.insert(newTransaction.copy(id = UUID.randomUUID().toString()))
        }
    }
}