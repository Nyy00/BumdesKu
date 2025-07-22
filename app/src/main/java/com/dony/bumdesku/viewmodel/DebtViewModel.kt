package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.*
import com.dony.bumdesku.repository.AccountRepository
import com.dony.bumdesku.repository.DebtRepository
import com.dony.bumdesku.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class DebtSummary(
    val totalPayable: Double = 0.0,
    val totalReceivable: Double = 0.0
)

class DebtViewModel(
    private val repository: DebtRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    val allPayables: Flow<List<Payable>> = repository.allPayables
    val allReceivables: Flow<List<Receivable>> = repository.allReceivables
    val allAccounts: Flow<List<Account>> = accountRepository.allAccounts

    val debtSummary: StateFlow<DebtSummary> = combine(allPayables, allReceivables) { payables, receivables ->
        val unpaidPayables = payables.filter { !it.isPaid }.sumOf { it.amount }
        val unpaidReceivables = receivables.filter { !it.isPaid }.sumOf { it.amount }
        DebtSummary(totalPayable = unpaidPayables, totalReceivable = unpaidReceivables)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebtSummary()
    )

    // ✅ FUNGSI BARU UNTUK MENDAPATKAN DATA SPESIFIK UNTUK DIEDIT
    fun getPayableById(id: Int): Flow<Payable?> = repository.getPayableById(id)
    fun getReceivableById(id: Int): Flow<Receivable?> = repository.getReceivableById(id)

    // --- FUNGSI CRUD ---
    fun insertPayableWithJournal(payable: Payable, debitAccount: Account) = viewModelScope.launch {
        repository.insert(payable)
        val utangUsahaAccount = accountRepository.allAccounts.first().find { it.accountNumber == "211" }
        if (utangUsahaAccount != null) {
            val newTransaction = Transaction(
                description = "Pencatatan utang: ${payable.description}",
                amount = payable.amount,
                date = payable.transactionDate,
                debitAccountId = debitAccount.id,
                creditAccountId = utangUsahaAccount.id,
                debitAccountName = debitAccount.accountName,
                creditAccountName = utangUsahaAccount.accountName
            )
            transactionRepository.insert(newTransaction)
        }
    }

    fun insertReceivableWithJournal(receivable: Receivable, creditAccount: Account) = viewModelScope.launch {
        repository.insert(receivable)
        val piutangUsahaAccount = accountRepository.allAccounts.first().find { it.accountNumber == "113" }
        if (piutangUsahaAccount != null) {
            val newTransaction = Transaction(
                description = "Pencatatan piutang: ${receivable.description}",
                amount = receivable.amount,
                date = receivable.transactionDate,
                debitAccountId = piutangUsahaAccount.id,
                creditAccountId = creditAccount.id,
                debitAccountName = piutangUsahaAccount.accountName,
                creditAccountName = creditAccount.accountName
            )
            transactionRepository.insert(newTransaction)
        }
    }

    // ✅ FUNGSI UPDATE UNTUK MENYIMPAN HASIL EDIT
    fun update(payable: Payable) = viewModelScope.launch { repository.update(payable) }
    fun update(receivable: Receivable) = viewModelScope.launch { repository.update(receivable) }

    // ✅ FUNGSI DELETE UNTUK MENGHAPUS DATA
    fun delete(payable: Payable) = viewModelScope.launch { repository.delete(payable) }
    fun delete(receivable: Receivable) = viewModelScope.launch { repository.delete(receivable) }


    fun markPayableAsPaid(payable: Payable) = viewModelScope.launch {
        if (payable.isPaid) return@launch
        repository.update(payable.copy(isPaid = true))
        val allAccounts = accountRepository.allAccounts.first()
        val kasAccount = allAccounts.find { it.accountNumber == "111" }
        val utangAccount = allAccounts.find { it.accountNumber == "211" }

        if (kasAccount != null && utangAccount != null) {
            val newTransaction = Transaction(
                description = "Pelunasan utang: ${payable.description}",
                amount = payable.amount,
                date = System.currentTimeMillis(),
                debitAccountId = utangAccount.id,
                creditAccountId = kasAccount.id,
                debitAccountName = utangAccount.accountName,
                creditAccountName = kasAccount.accountName
            )
            transactionRepository.insert(newTransaction)
        }
    }

    fun markReceivableAsPaid(receivable: Receivable) = viewModelScope.launch {
        if (receivable.isPaid) return@launch
        repository.update(receivable.copy(isPaid = true))
        val allAccounts = accountRepository.allAccounts.first()
        val kasAccount = allAccounts.find { it.accountNumber == "111" }
        val piutangAccount = allAccounts.find { it.accountNumber == "113" }

        if (kasAccount != null && piutangAccount != null) {
            val newTransaction = Transaction(
                description = "Pelunasan piutang: ${receivable.description}",
                amount = receivable.amount,
                date = System.currentTimeMillis(),
                debitAccountId = kasAccount.id,
                creditAccountId = piutangAccount.id,
                debitAccountName = kasAccount.accountName,
                creditAccountName = piutangAccount.accountName
            )
            transactionRepository.insert(newTransaction)
        }
    }
}
