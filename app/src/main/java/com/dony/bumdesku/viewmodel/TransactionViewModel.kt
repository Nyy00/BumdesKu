package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.*
import com.dony.bumdesku.repository.AccountRepository
import com.dony.bumdesku.repository.TransactionRepository
import com.dony.bumdesku.repository.UnitUsahaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

// Data class untuk menampung data grafik
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
            if (query.isBlank()) list else list.filter {
                it.description.contains(query, ignoreCase = true) ||
                        it.debitAccountName.contains(query, ignoreCase = true) ||
                        it.creditAccountName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Data Kalkulasi Otomatis ---
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

    val neracaData: StateFlow<NeracaData> = combine(allTransactions, _allAccounts) { transactions, accounts ->
        val asetItems = mutableListOf<NeracaItem>()
        val kewajibanItems = mutableListOf<NeracaItem>()
        val modalItems = mutableListOf<NeracaItem>()
        val debits = transactions.groupBy { it.debitAccountId }
        val credits = transactions.groupBy { it.creditAccountId }
        for (account in accounts) {
            val totalDebit = debits[account.id]?.sumOf { it.amount } ?: 0.0
            val totalKredit = credits[account.id]?.sumOf { it.amount } ?: 0.0
            val saldoAkhir = when (account.category) {
                AccountCategory.ASET, AccountCategory.BEBAN -> totalDebit - totalKredit
                else -> totalKredit - totalDebit
            }
            val neracaItem = NeracaItem(account.accountName, saldoAkhir)
            when (account.category) {
                AccountCategory.ASET -> asetItems.add(neracaItem)
                AccountCategory.KEWAJIBAN -> kewajibanItems.add(neracaItem)
                AccountCategory.MODAL -> modalItems.add(neracaItem)
                else -> {}
            }
        }
        NeracaData(
            asetItems = asetItems,
            kewajibanItems = kewajibanItems,
            modalItems = modalItems,
            totalAset = asetItems.sumOf { it.balance },
            totalKewajiban = kewajibanItems.sumOf { it.balance },
            totalModal = modalItems.sumOf { it.balance }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NeracaData())

    val financialHealthData: StateFlow<FinancialHealthData> = combine(allTransactions, _allAccounts) { transactions, accounts ->
        val asetLancarIds = accounts.filter { it.category == AccountCategory.ASET && it.accountNumber.startsWith("11") }.map { it.id }
        val kewajibanLancarIds = accounts.filter { it.category == AccountCategory.KEWAJIBAN && it.accountNumber.startsWith("21") }.map { it.id }
        val totalAsetLancar = transactions.sumOf {
            when {
                it.debitAccountId in asetLancarIds -> it.amount
                it.creditAccountId in asetLancarIds -> -it.amount
                else -> 0.0
            }
        }
        val totalKewajibanLancar = transactions.sumOf {
            when {
                it.creditAccountId in kewajibanLancarIds -> it.amount
                it.debitAccountId in kewajibanLancarIds -> -it.amount
                else -> 0.0
            }
        }
        val currentRatio = if (totalKewajibanLancar > 0) (totalAsetLancar / totalKewajibanLancar).toFloat() else 0f
        val status = when {
            currentRatio >= 1.5f -> HealthStatus.SEHAT
            currentRatio >= 1.0f -> HealthStatus.WASPADA
            else -> HealthStatus.TIDAK_SEHAT
        }
        FinancialHealthData(currentRatio, status)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialHealthData())

    val neracaSaldoItems: StateFlow<List<NeracaSaldoItem>> = combine(allTransactions, _allAccounts) { transactions, accounts ->
        val saldoItems = mutableListOf<NeracaSaldoItem>()
        for (account in accounts) {
            val totalDebit = transactions.filter { it.debitAccountId == account.id }.sumOf { it.amount }
            val totalKredit = transactions.filter { it.creditAccountId == account.id }.sumOf { it.amount }
            if (totalDebit > 0 || totalKredit > 0) {
                saldoItems.add(
                    NeracaSaldoItem(
                        accountNumber = account.accountNumber,
                        accountName = account.accountName,
                        totalDebit = totalDebit,
                        totalKredit = totalKredit
                    )
                )
            }
        }
        saldoItems
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Logika Laporan ---
    private val _reportData = MutableStateFlow(ReportData())
    val reportData: StateFlow<ReportData> = _reportData.asStateFlow()

    private val _filteredReportTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val filteredReportTransactions: StateFlow<List<Transaction>> = _filteredReportTransactions.asStateFlow()

    private var filterJob: Job? = null

    fun generateReport(startDate: Long, endDate: Long, unitUsaha: UnitUsaha?) {
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            val accounts = _allAccounts.first()
            val pendapatanAccountIds = accounts.filter { it.category == AccountCategory.PENDAPATAN }.map { it.id }
            val bebanAccountIds = accounts.filter { it.category == AccountCategory.BEBAN }.map { it.id }

            // Panggil fungsi getReportData dari repository
            val (income, expenses) = transactionRepository.getReportData(
                startDate,
                endDate,
                unitUsaha?.id,
                pendapatanAccountIds,
                bebanAccountIds
            )

            _reportData.value = ReportData(
                totalIncome = income,
                totalExpenses = expenses,
                netProfit = income - expenses,
                isGenerated = true,
                startDate = startDate,
                endDate = endDate,
                unitUsahaName = unitUsaha?.name ?: "Semua Unit Usaha",
                unitUsahaId = unitUsaha?.id
            )

            // Panggil fungsi getFilteredTransactions dari repository
            transactionRepository.getFilteredTransactions(startDate, endDate, unitUsaha?.id)
                .collect { filteredList ->
                    _filteredReportTransactions.value = filteredList
                }
        }
    }

    private fun calculateReportTotals(transactions: List<Transaction>): Pair<Double, Double> {
        val accounts = _allAccounts.value
        val pendapatanAccountIds = accounts.filter { it.category == AccountCategory.PENDAPATAN }.map { it.id }
        val bebanAccountIds = accounts.filter { it.category == AccountCategory.BEBAN }.map { it.id }
        val income = transactions.filter { it.creditAccountId in pendapatanAccountIds }.sumOf { it.amount }
        val expenses = transactions.filter { it.debitAccountId in bebanAccountIds }.sumOf { it.amount }
        return Pair(income, expenses)
    }

    fun clearReport() {
        _reportData.value = ReportData()
        _filteredReportTransactions.value = emptyList()
        filterJob?.cancel()
    }

    fun getBukuPembantuData(accountId: String, accountCategory: AccountCategory): Flow<BukuPembantuData> {
        return allTransactions.map { transactions ->
            val filteredTx = transactions
                .filter { it.debitAccountId == accountId || it.creditAccountId == accountId }
                .sortedBy { it.date }
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