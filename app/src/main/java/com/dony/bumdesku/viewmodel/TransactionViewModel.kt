package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
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

// Data class untuk menampung filter laporan
private data class ReportFilters(
    val startDate: Long,
    val endDate: Long,
    val unitUsaha: UnitUsaha? = null,
    val trigger: Long = 0L // Pemicu untuk generate laporan
)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val unitUsahaRepository: UnitUsahaRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    // --- ALIRAN DATA MENTAH DARI REPOSITORY ---
    val allAccounts: Flow<List<Account>> = accountRepository.allAccounts
    val allUnitUsaha: Flow<List<UnitUsaha>> = unitUsahaRepository.allUnitUsaha
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // --- ALIRAN DATA TRANSAKSI (dengan filter pencarian) ---
    val allTransactions: StateFlow<List<Transaction>> = _searchQuery.flatMapLatest { query ->
        transactionRepository.allTransactions.map { list ->
            if (query.isBlank()) list else list.filter {
                it.description.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- KALKULASI DATA DASHBOARD YANG REAKTIF ---
    val dashboardData: StateFlow<DashboardData> = combine(allAccounts, allTransactions) { accounts, transactions ->
        if (accounts.isEmpty()) {
            DashboardData()
        } else {
            val pendapatanIds = accounts.filter { it.category == AccountCategory.PENDAPATAN }.map { it.id }
            val bebanIds = accounts.filter { it.category == AccountCategory.BEBAN }.map { it.id }
            val totalIncome = transactions.filter { it.creditAccountId in pendapatanIds }.sumOf { it.amount }
            val totalExpenses = transactions.filter { it.debitAccountId in bebanIds }.sumOf { it.amount }
            DashboardData(totalIncome, totalExpenses, totalIncome - totalExpenses)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardData())

    val chartData: StateFlow<ChartData> = combine(allAccounts, allTransactions) { accounts, transactions ->
        if (accounts.isEmpty()) ChartData() else {
            val calendar = Calendar.getInstance()
            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Ags", "Sep", "Okt", "Nov", "Des")
            val pendapatanIds = accounts.filter { it.category == AccountCategory.PENDAPATAN }.map { it.id }
            val bebanIds = accounts.filter { it.category == AccountCategory.BEBAN }.map { it.id }
            val incomeByMonth = transactions.filter { it.creditAccountId in pendapatanIds }
                .groupBy { calendar.apply { timeInMillis = it.date }.get(Calendar.MONTH) }
                .mapValues { it.value.sumOf { tx -> tx.amount }.toFloat() }
            val expensesByMonth = transactions.filter { it.debitAccountId in bebanIds }
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
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChartData())


    // --- LOGIKA LAPORAN KEUANGAN ---
    private val _reportFilters = MutableStateFlow<ReportFilters?>(null)

    fun generateReport(startDate: Long, endDate: Long, unitUsaha: UnitUsaha?) {
        _reportFilters.value = ReportFilters(startDate, endDate, unitUsaha, System.currentTimeMillis())
    }

    val reportData: StateFlow<ReportData> = combine(
        _reportFilters, allAccounts, allTransactions
    ) { filters, accounts, transactions ->
        if (filters == null || accounts.isEmpty()) {
            return@combine ReportData(isGenerated = false)
        }

        val filteredTransactions = transactions.filter { tx ->
            val unitMatch = filters.unitUsaha == null || tx.unitUsahaId == filters.unitUsaha.id
            tx.date in filters.startDate..filters.endDate && unitMatch
        }

        val pendapatanIds = accounts.filter { it.category == AccountCategory.PENDAPATAN }.map { it.id }
        val bebanIds = accounts.filter { it.category == AccountCategory.BEBAN }.map { it.id }

        val totalIncome = filteredTransactions.filter { it.creditAccountId in pendapatanIds }.sumOf { it.amount }
        val totalExpenses = filteredTransactions.filter { it.debitAccountId in bebanIds }.sumOf { it.amount }

        ReportData(
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netProfit = totalIncome - totalExpenses,
            isGenerated = true,
            startDate = filters.startDate,
            endDate = filters.endDate,
            unitUsahaName = filters.unitUsaha?.name ?: "Semua Unit Usaha",
            unitUsahaId = filters.unitUsaha?.id
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportData())

    val filteredReportTransactions: StateFlow<List<Transaction>> = reportData.map { data ->
        if (!data.isGenerated) {
            emptyList()
        } else {
            allTransactions.value.filter { tx ->
                val unitMatch = data.unitUsahaId == null || tx.unitUsahaId == data.unitUsahaId
                tx.date in data.startDate..data.endDate && unitMatch
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearReport() {
        _reportFilters.value = null
    }

    // --- FUNGSI-FUNGSI LAIN ---
    val neracaData: StateFlow<NeracaData> = combine(allTransactions, allAccounts) { transactions, accounts ->
        val asetItems = mutableListOf<NeracaItem>()
        val kewajibanItems = mutableListOf<NeracaItem>()
        val modalItems = mutableListOf<NeracaItem>()

        val debits = transactions.groupBy { it.debitAccountId }
        val credits = transactions.groupBy { it.creditAccountId }

        // [PERBAIKAN 1] Pisahkan kalkulasi Laba Bersih
        val pendapatanIds = accounts.filter { it.category == AccountCategory.PENDAPATAN }.map { it.id }
        val bebanIds = accounts.filter { it.category == AccountCategory.BEBAN }.map { it.id }

        val totalPendapatan = transactions.filter { it.creditAccountId in pendapatanIds }.sumOf { it.amount }
        val totalBeban = transactions.filter { it.debitAccountId in bebanIds }.sumOf { it.amount }
        val labaBersih = totalPendapatan - totalBeban

        for (account in accounts) {
            val totalDebit = debits[account.id]?.sumOf { it.amount } ?: 0.0
            val totalKredit = credits[account.id]?.sumOf { it.amount } ?: 0.0

            // Hanya proses akun neraca (Aset, Kewajiban, Modal)
            if (account.category in listOf(AccountCategory.ASET, AccountCategory.KEWAJIBAN, AccountCategory.MODAL)) {
                val saldoAkhir = when (account.category) {
                    AccountCategory.ASET -> totalDebit - totalKredit
                    else -> totalKredit - totalDebit // Kewajiban & Modal
                }

                if(saldoAkhir != 0.0) {
                    val neracaItem = NeracaItem(account.accountName, saldoAkhir)
                    when (account.category) {
                        AccountCategory.ASET -> asetItems.add(neracaItem)
                        AccountCategory.KEWAJIBAN -> kewajibanItems.add(neracaItem)
                        AccountCategory.MODAL -> modalItems.add(neracaItem)
                        else -> {}
                    }
                }
            }
        }

        // [PERBAIKAN 2] Tambahkan Laba Bersih sebagai item di dalam Modal
        if (labaBersih != 0.0) {
            modalItems.add(NeracaItem("Laba/Rugi Tahun Berjalan", labaBersih))
        }

        val totalAset = asetItems.sumOf { it.balance }
        val totalKewajiban = kewajibanItems.sumOf { it.balance }
        // [PERBAIKAN 3] Total Modal sekarang adalah jumlah dari akun modal + laba bersih
        val totalModal = modalItems.sumOf { it.balance }

        NeracaData(
            asetItems = asetItems,
            kewajibanItems = kewajibanItems,
            modalItems = modalItems,
            totalAset = totalAset,
            totalKewajiban = totalKewajiban,
            totalModal = totalModal
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NeracaData())

    val financialHealthData: StateFlow<FinancialHealthData> = combine(allTransactions, allAccounts) { transactions, accounts ->
        // --- 1. Logika Perhitungan Laba Rugi (diambil dari `dashboardData`) ---
        val pendapatanIds = accounts.filter { it.category == AccountCategory.PENDAPATAN }.map { it.id }
        val bebanIds = accounts.filter { it.category == AccountCategory.BEBAN }.map { it.id }

        val totalPendapatan = transactions.filter { it.creditAccountId in pendapatanIds }.sumOf { it.amount }
        val totalBeban = transactions.filter { it.debitAccountId in bebanIds }.sumOf { it.amount }
        val labaBersih = totalPendapatan - totalBeban

        // --- 2. Logika Perhitungan Rasio Keuangan (yang sudah ada) ---
        val asetLancarIds = accounts.filter { it.category == AccountCategory.ASET && it.accountNumber.startsWith("11") }.map { it.id }
        val kewajibanLancarIds = accounts.filter { it.category == AccountCategory.KEWAJIBAN && it.accountNumber.startsWith("21") }.map { it.id }

        val totalAsetLancar = accounts.filter { it.id in asetLancarIds }.sumOf { acc ->
            val totalDebit = transactions.filter { it.debitAccountId == acc.id }.sumOf { it.amount }
            val totalKredit = transactions.filter { it.creditAccountId == acc.id }.sumOf { it.amount }
            totalDebit - totalKredit
        }
        val totalKewajibanLancar = accounts.filter { it.id in kewajibanLancarIds }.sumOf { acc ->
            val totalDebit = transactions.filter { it.debitAccountId == acc.id }.sumOf { it.amount }
            val totalKredit = transactions.filter { it.creditAccountId == acc.id }.sumOf { it.amount }
            totalKredit - totalDebit
        }

        val currentRatio = if (totalKewajibanLancar > 0) (totalAsetLancar / totalKewajibanLancar).toFloat() else 0f

        val status = when {
            totalAsetLancar == 0.0 && totalKewajibanLancar == 0.0 -> HealthStatus.TIDAK_TERDEFINISI
            currentRatio >= 1.5f -> HealthStatus.SEHAT
            currentRatio >= 1.0f -> HealthStatus.WASPADA
            else -> HealthStatus.TIDAK_SEHAT
        }

        // --- 3. Buat objek FinancialHealthData dengan SEMUA data yang sudah dihitung ---
        FinancialHealthData(
            currentRatio = currentRatio,
            status = status,
            labaRugi = labaBersih,
            totalPendapatan = totalPendapatan,
            totalBeban = totalBeban
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialHealthData())

    private val _lpeData = MutableStateFlow(LpeData())
    val lpeData: StateFlow<LpeData> = _lpeData.asStateFlow()

    fun generateLpe(startDate: Long, endDate: Long) {
        viewModelScope.launch {
            val allTx = allTransactions.first()
            val allAcc = allAccounts.first()
            val pendapatanIds = allAcc.filter { it.category == AccountCategory.PENDAPATAN }.map { it.id }
            val bebanIds = allAcc.filter { it.category == AccountCategory.BEBAN }.map { it.id }
            val transactionsInPeriod = allTx.filter { it.date in startDate..endDate }
            val totalPendapatan = transactionsInPeriod.filter { it.creditAccountId in pendapatanIds }.sumOf { it.amount }
            val totalBeban = transactionsInPeriod.filter { it.debitAccountId in bebanIds }.sumOf { it.amount }
            val labaBersih = totalPendapatan - totalBeban
            val priveAccountId = allAcc.find { it.accountNumber == "312" }?.id
            val totalPrive = if (priveAccountId != null) {
                transactionsInPeriod.filter { it.debitAccountId == priveAccountId }.sumOf { it.amount }
            } else {
                0.0
            }
            val modalIds = allAcc.filter { it.category == AccountCategory.MODAL }.map { it.id }
            val transactionsBeforePeriod = allTx.filter { it.date < startDate }
            val modalAwal = transactionsBeforePeriod.sumOf {
                when {
                    it.creditAccountId in modalIds -> it.amount
                    it.debitAccountId in modalIds -> -it.amount
                    else -> 0.0
                }
            }
            val modalAkhir = modalAwal + labaBersih - totalPrive
            _lpeData.value = LpeData(
                modalAwal = modalAwal,
                labaBersih = labaBersih,
                prive = totalPrive,
                modalAkhir = modalAkhir,
                isGenerated = true
            )
        }
    }
    fun lockTransactionsUpTo(date: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            transactionRepository.lockTransactionsUpTo(date)
            onComplete()
        }
    }

    val neracaSaldoItems: StateFlow<List<NeracaSaldoItem>> = combine(allTransactions, allAccounts) { transactions, accounts ->
        val saldoItems = mutableListOf<NeracaSaldoItem>()
        for (account in accounts) {
            val totalDebit = transactions.filter { it.debitAccountId == account.id }.sumOf { it.amount }
            val totalKredit = transactions.filter { it.creditAccountId == account.id }.sumOf { it.amount }
            if (totalDebit > 0 || totalKredit > 0) {
                saldoItems.add(
                    NeracaSaldoItem(
                        accountId = account.id, // <-- PERBARUI DI SINI
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

    fun getBukuPembantuData(accountId: String, accountCategory: AccountCategory): Flow<BukuPembantuData> {
        return allTransactions.map { transactions ->
            val filteredTx = transactions
                .filter { it.debitAccountId == accountId || it.creditAccountId == accountId }
                .sortedBy { it.date }

            // ✅ PERBAIKAN 1: Ubah tipe kunci Map dari Int menjadi String
            val runningBalances = mutableMapOf<String, Double>()
            var currentBalance = 0.0

            for (tx in filteredTx) {
                val debit = if (tx.debitAccountId == accountId) tx.amount else 0.0
                val credit = if (tx.creditAccountId == accountId) tx.amount else 0.0
                currentBalance += when(accountCategory) {
                    AccountCategory.ASET, AccountCategory.BEBAN -> debit - credit
                    else -> credit - debit
                }
                // ✅ PERBAIKAN 2: Gunakan tx.id sebagai kunci, bukan tx.localId
                runningBalances[tx.id] = currentBalance
            }
            BukuPembantuData(filteredTx, runningBalances)
        }
    }
    fun onSearchQueryChange(newQuery: String) { _searchQuery.value = newQuery }
    fun getTransactionById(id: String): Flow<Transaction?> { // <-- UBAH TIPE DATA DI SINI
        return transactionRepository.getTransactionById(id)
    }
    fun insert(transaction: Transaction) = viewModelScope.launch { transactionRepository.insert(transaction.copy(id = UUID.randomUUID().toString())) }
    fun update(transaction: Transaction) = viewModelScope.launch { transactionRepository.update(transaction) }
    fun delete(transaction: Transaction) = viewModelScope.launch { transactionRepository.delete(transaction) }
    fun insert(unitUsaha: UnitUsaha) = viewModelScope.launch { unitUsahaRepository.insert(unitUsaha.copy(id = UUID.randomUUID().toString())) }
    fun update(unitUsaha: UnitUsaha) = viewModelScope.launch { unitUsahaRepository.update(unitUsaha) }
    fun delete(unitUsaha: UnitUsaha) = viewModelScope.launch { unitUsahaRepository.delete(unitUsaha) }
}
