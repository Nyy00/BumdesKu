package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.*
import com.dony.bumdesku.data.AccountCategory
import com.dony.bumdesku.data.NeracaData
import com.dony.bumdesku.data.FinancialHealthData
import com.dony.bumdesku.data.HealthStatus
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
            allTransactions.map { list ->
                list.filter {
                    val dateMatch = it.date in start..end
                    val unitMatch = if (unitId == null) true else it.unitUsahaId == unitId
                    dateMatch && unitMatch
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ✅ --- LOGIKA NERACA YANG SUDAH DETAIL ---
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

            // Tambahkan rincian akun ke daftar yang sesuai
            val neracaItem = NeracaItem(account.accountName, saldoAkhir)
            when (account.category) {
                AccountCategory.ASET -> asetItems.add(neracaItem)
                AccountCategory.KEWAJIBAN -> kewajibanItems.add(neracaItem)
                AccountCategory.MODAL -> modalItems.add(neracaItem)
                else -> { /* Abaikan Pendapatan dan Beban */ }
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NeracaData()
    )

    // ✅ --- LOGIKA BARU UNTUK KESEHATAN KEUANGAN ---
    val financialHealthData: StateFlow<FinancialHealthData> = combine(allTransactions, _allAccounts) { transactions, accounts ->
        // Ambil semua akun Aset Lancar (nomor diawali '11')
        val asetLancarIds = accounts
            .filter { it.category == AccountCategory.ASET && it.accountNumber.startsWith("11") }
            .map { it.id }

        // Ambil semua akun Kewajiban Lancar (nomor diawali '21')
        val kewajibanLancarIds = accounts
            .filter { it.category == AccountCategory.KEWAJIBAN && it.accountNumber.startsWith("21") }
            .map { it.id }

        // Hitung total saldo Aset Lancar
        val totalAsetLancar = transactions.sumOf {
            when {
                it.debitAccountId in asetLancarIds -> it.amount
                it.creditAccountId in asetLancarIds -> -it.amount
                else -> 0.0
            }
        }

        // Hitung total saldo Kewajiban Lancar
        val totalKewajibanLancar = transactions.sumOf {
            when {
                it.creditAccountId in kewajibanLancarIds -> it.amount
                it.debitAccountId in kewajibanLancarIds -> -it.amount
                else -> 0.0
            }
        }

        // Hitung Rasio Lancar
        val currentRatio = if (totalKewajibanLancar > 0) {
            (totalAsetLancar / totalKewajibanLancar).toFloat()
        } else {
            0f // Hindari pembagian dengan nol
        }

        // Tentukan status kesehatan
        val status = when {
            currentRatio >= 1.5f -> HealthStatus.SEHAT
            currentRatio >= 1.0f -> HealthStatus.WASPADA
            else -> HealthStatus.TIDAK_SEHAT
        }

        FinancialHealthData(currentRatio, status)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinancialHealthData()
    )

    // ✅ --- LOGIKA BARU UNTUK NERACA SALDO ---
    val neracaSaldoItems: StateFlow<List<NeracaSaldoItem>> = combine(allTransactions, _allAccounts) { transactions, accounts ->
        // Buat daftar kosong untuk menampung hasil
        val saldoItems = mutableListOf<NeracaSaldoItem>()

        // Untuk setiap akun yang ada di Chart of Accounts...
        for (account in accounts) {
            // Hitung total semua transaksi di mana akun ini ada di sisi DEBIT
            val totalDebit = transactions
                .filter { it.debitAccountId == account.id }
                .sumOf { it.amount }

            // Hitung total semua transaksi di mana akun ini ada di sisi KREDIT
            val totalKredit = transactions
                .filter { it.creditAccountId == account.id }
                .sumOf { it.amount }

            // Tambahkan ke daftar HANYA jika ada transaksi
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
        saldoItems // Kembalikan daftar yang sudah diisi
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
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