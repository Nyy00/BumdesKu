package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.*
import com.dony.bumdesku.repository.AccountRepository
import com.dony.bumdesku.repository.DebtRepository
import com.dony.bumdesku.repository.TransactionRepository
import com.dony.bumdesku.repository.UnitUsahaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DebtSummary(
    val totalPayable: Double = 0.0,
    val totalReceivable: Double = 0.0
)

class DebtViewModel(
    private val repository: DebtRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val unitUsahaRepository: UnitUsahaRepository
) : ViewModel() {

    private val _selectedUnitFilter = MutableStateFlow<UnitUsaha?>(null)
    val selectedUnitFilter: StateFlow<UnitUsaha?> = _selectedUnitFilter.asStateFlow()

    val allUnitUsaha: Flow<List<UnitUsaha>> = unitUsahaRepository.allUnitUsaha

    val filteredPayables: StateFlow<List<Payable>> = combine(
        repository.allPayables,
        _selectedUnitFilter
    ) { payables, selectedUnit ->
        if (selectedUnit == null) payables else payables.filter { it.unitUsahaId == selectedUnit.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredReceivables: StateFlow<List<Receivable>> = combine(
        repository.allReceivables,
        _selectedUnitFilter
    ) { receivables, selectedUnit ->
        if (selectedUnit == null) receivables else receivables.filter { it.unitUsahaId == selectedUnit.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val allAccounts: Flow<List<Account>> = accountRepository.allAccounts

    val debtSummary: StateFlow<DebtSummary> = combine(filteredPayables, filteredReceivables) { payables, receivables ->
        val unpaidPayables = payables.filter { !it.isPaid }.sumOf { it.amount }
        val unpaidReceivables = receivables.filter { !it.isPaid }.sumOf { it.amount }
        DebtSummary(totalPayable = unpaidPayables, totalReceivable = unpaidReceivables)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DebtSummary())

    fun selectUnitFilter(unitUsaha: UnitUsaha?) {
        _selectedUnitFilter.value = unitUsaha
    }

    fun getPayableById(id: String): Flow<Payable?> = repository.getPayableById(id)
    fun getReceivableById(id: String): Flow<Receivable?> = repository.getReceivableById(id)

    // --- FUNGSI CRUD (Diperbarui) ---
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
                creditAccountName = utangUsahaAccount.accountName,
                unitUsahaId = payable.unitUsahaId // ✅ PERBAIKAN UTAMA DI SINI
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
                creditAccountName = creditAccount.accountName,
                unitUsahaId = receivable.unitUsahaId // ✅ PERBAIKAN UTAMA DI SINI
            )
            transactionRepository.insert(newTransaction)
        }
    }

    fun update(payable: Payable) = viewModelScope.launch { repository.update(payable) }
    fun update(receivable: Receivable) = viewModelScope.launch { repository.update(receivable) }

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
                creditAccountName = kasAccount.accountName,
                unitUsahaId = payable.unitUsahaId // ✅ Pastikan disertakan juga di sini
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
                creditAccountName = piutangAccount.accountName,
                unitUsahaId = receivable.unitUsahaId // ✅ Pastikan disertakan juga di sini
            )
            transactionRepository.insert(newTransaction)
        }
    }
}