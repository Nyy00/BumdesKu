package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.*
import com.dony.bumdesku.data.AccountCategory
import com.dony.bumdesku.data.NeracaData
import com.dony.bumdesku.repository.AccountRepository
import com.dony.bumdesku.repository.TransactionRepository
import com.dony.bumdesku.repository.UnitUsahaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class ChartData(
    val monthlyIncome: Map<String, Float> = emptyMap(),
    val monthlyExpenses: Map<String, Float> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val unitUsahaRepository: UnitUsahaRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    // --- State untuk UI ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _allAccounts = accountRepository.allAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allAccounts: Flow<List<Account>> = _allAccounts

    val allUnitUsaha: Flow<List<UnitUsaha>> = unitUsahaRepository.allUnitUsaha

    // --- Alur Data Utama ---
    val allTransactions: StateFlow<List<Transaction>> = _searchQuery.flatMapLatest { query ->
        transactionRepository.allTransactions.map { list ->
            if (query.isBlank()) list else list.filter { it.description.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardData: StateFlow<DashboardData> = combine(allTransactions, _allAccounts) { transactions, accounts ->
        val pendapatanAccountIds = accounts.filter { it.category == AccountCategory.PENDAPATAN }.map { it.id }
        val bebanAccountIds = accounts.filter { it.category == AccountCategory.BEBAN }.map { it.id }
        val totalIncome = transactions.filter { it.creditAccountId in pendapatanAccountIds }.sumOf { it.amount }
        val totalExpenses = transactions.filter { it.debitAccountId in bebanAccountIds }.sumOf { it.amount }
        DashboardData(totalIncome, totalExpenses, totalIncome - totalExpenses)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardData())

    val chartData: StateFlow<ChartData> = combine(allTransactions, _allAccounts) { transactions, accounts ->
        val calendar = Calendar.getInstance()
        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Ags", "Sep", "Okt", "Nov", "Des")
        val pendapatanAccountIds = accounts.filter { it.category == AccountCategory.PENDAPATAN }.map { it.id }
        val bebanAccountIds = accounts.filter { it.category == AccountCategory.BEBAN }.map { it.id }
        val incomeByMonth = transactions
            .filter { it.creditAccountId in pendapatanAccountIds }
            .groupBy { calendar.apply { timeInMillis = it.date }.get(Calendar.MONTH) }
            .mapValues { it.value.sumOf { tx -> tx.amount }.toFloat() }
        val expensesByMonth = transactions
            .filter { it.debitAccountId in bebanAccountIds }
            .groupBy { calendar.apply { timeInMillis = it.date }.get(Calendar.MONTH) }
            .mapValues { it.value.sumOf { tx -> tx.amount }.toFloat() }
        val allMonthIndexes = (incomeByMonth.keys + expensesByMonth.keys).distinct().sorted()
        val finalIncome = LinkedHashMap<String, Float>()
        val finalExpenses = LinkedHashMap<String, Float>()
        for (index in allMonthIndexes) {
            val monthName = monthNames[index]
            finalIncome[monthName] = incomeByMonth[index] ?: 0f
            finalExpenses[monthName] = expensesByMonth[index] ?: 0f
        }
        ChartData(finalIncome, finalExpenses)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChartData())

    // --- LOGIKA LAPORAN YANG DIPERBAIKI ---
    private val _reportData = MutableStateFlow(ReportData())
    val reportData: StateFlow<ReportData> = _reportData.asStateFlow()

    private val _reportFilters = MutableStateFlow<Triple<Long, Long, String?>>(Triple(0L, 0L, null))

    val filteredReportTransactions: StateFlow<List<Transaction>> = _reportFilters.flatMapLatest { (start, end, unitId) ->
        if (start == 0L || end == 0L) {
            flowOf(emptyList())
        } else {
            // ✅ Ambil dari `allTransactions` yang sudah ada, lalu filter
            allTransactions.map { list ->
                list.filter {
                    val dateMatch = it.date in start..end
                    val unitMatch = if (unitId == null) true else it.unitUsahaId == unitId
                    dateMatch && unitMatch
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    //-- NERACA DATA --
    val neracaData: StateFlow<NeracaData> = combine(allTransactions, _allAccounts) { transactions, accounts ->
        var totalAset = 0.0
        var totalKewajiban = 0.0
        var totalModal = 0.0

        // Kelompokkan transaksi berdasarkan ID akun
        val debits = transactions.groupBy { it.debitAccountId }
        val credits = transactions.groupBy { it.creditAccountId }

        // Hitung saldo akhir untuk setiap akun
        for (account in accounts) {
            val totalDebit = debits[account.id]?.sumOf { it.amount } ?: 0.0
            val totalKredit = credits[account.id]?.sumOf { it.amount } ?: 0.0

            val saldoAkhir = when (account.category) {
                // Saldo normal Aset & Beban ada di Debit
                AccountCategory.ASET, AccountCategory.BEBAN -> totalDebit - totalKredit
                // Saldo normal Kewajiban, Modal, Pendapatan ada di Kredit
                AccountCategory.KEWAJIBAN, AccountCategory.MODAL, AccountCategory.PENDAPATAN -> totalKredit - totalDebit
            }

            // Tambahkan saldo akhir ke kategori yang sesuai
            when (account.category) {
                AccountCategory.ASET -> totalAset += saldoAkhir
                AccountCategory.KEWAJIBAN -> totalKewajiban += saldoAkhir
                AccountCategory.MODAL -> totalModal += saldoAkhir
                else -> { /* Abaikan Pendapatan dan Beban untuk Neraca */ }
            }
        }

        NeracaData(totalAset, totalKewajiban, totalModal)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NeracaData()
    )

    //--- BUKU PEMBANTU ---
    fun getBukuPembantuData(accountId: String, accountCategory: AccountCategory): Flow<BukuPembantuData> {
        return allTransactions.map { transactions ->
            // 1. Filter transaksi yang melibatkan akun ini
            val filteredTx = transactions
                .filter { it.debitAccountId == accountId || it.creditAccountId == accountId }
                .sortedBy { it.date }

            // 2. Hitung saldo berjalan
            val runningBalances = mutableMapOf<Int, Double>()
            var currentBalance = 0.0
            for (tx in filteredTx) {
                val debit = if (tx.debitAccountId == accountId) tx.amount else 0.0
                val credit = if (tx.creditAccountId == accountId) tx.amount else 0.0

                currentBalance += when(accountCategory) {
                    AccountCategory.ASET, AccountCategory.BEBAN -> debit - credit
                    else -> credit - debit
                }
                runningBalances[tx.localId] = currentBalance
            }
            BukuPembantuData(filteredTx, runningBalances)
        }
    }

    fun generateReport(startDate: Long, endDate: Long, unitUsaha: UnitUsaha?) {
        // ✅ Fungsi ini sekarang HANYA mengupdate filter.
        //    Sangat sederhana dan anti-gagal.
        _reportFilters.value = Triple(startDate, endDate, unitUsaha?.id)
    }

    fun clearReport() {
        _reportData.value = ReportData()
        _reportFilters.value = Triple(0L, 0L, null)
    }

    // --- CRUD Operations ---
    fun onSearchQueryChange(newQuery: String) { _searchQuery.value = newQuery }
    fun getTransactionById(id: Int): Flow<Transaction?> = transactionRepository.getTransactionById(id)
    fun insert(transaction: Transaction) = viewModelScope.launch { transactionRepository.insert(transaction.copy(id = UUID.randomUUID().toString())) }
    fun update(transaction: Transaction) = viewModelScope.launch { transactionRepository.update(transaction) }
    fun delete(transaction: Transaction) = viewModelScope.launch { transactionRepository.delete(transaction) }
    fun insert(unitUsaha: UnitUsaha) = viewModelScope.launch { unitUsahaRepository.insert(unitUsaha.copy(id = UUID.randomUUID().toString())) }
    fun update(unitUsaha: UnitUsaha) = viewModelScope.launch { unitUsahaRepository.update(unitUsaha) }
    fun delete(unitUsaha: UnitUsaha) = viewModelScope.launch { unitUsahaRepository.delete(unitUsaha) }
}