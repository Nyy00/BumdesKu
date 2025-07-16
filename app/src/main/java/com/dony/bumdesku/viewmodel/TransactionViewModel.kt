package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.DashboardData
import com.dony.bumdesku.data.ReportData
import com.dony.bumdesku.data.Transaction
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.repository.TransactionRepository
import com.dony.bumdesku.repository.UnitUsahaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.LinkedHashMap
import java.util.UUID

// Data class ChartData tidak perlu diubah
data class ChartData(
    val monthlyIncome: Map<String, Float> = emptyMap(),
    val monthlyExpenses: Map<String, Float> = emptyMap()
)

class TransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val unitUsahaRepository: UnitUsahaRepository,
) : ViewModel() {

    // --- StateFlow lain tidak perlu diubah ---
    val allTransactions: Flow<List<Transaction>> = transactionRepository.allTransactions
    val dashboardData: StateFlow<DashboardData> = combine(
        transactionRepository.getTotalIncome(),
        transactionRepository.getTotalExpenses()
    ) { income, expenses ->
        val totalIncome = income ?: 0.0
        val totalExpenses = expenses ?: 0.0
        val finalBalance = totalIncome - totalExpenses
        DashboardData(totalIncome, totalExpenses, finalBalance)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardData()
    )
    val chartData: StateFlow<ChartData> = allTransactions.map { transactions ->
        val calendar = Calendar.getInstance()
        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Ags", "Sep", "Okt", "Nov", "Des")
        val incomeByMonthIndex = transactions
            .filter { it.type == "PEMASUKAN" }
            .groupBy { calendar.apply { timeInMillis = it.date }.get(Calendar.MONTH) }
            .mapValues { it.value.sumOf { tx -> tx.amount }.toFloat() }
        val expensesByMonthIndex = transactions
            .filter { it.type == "PENGELUARAN" }
            .groupBy { calendar.apply { timeInMillis = it.date }.get(Calendar.MONTH) }
            .mapValues { it.value.sumOf { tx -> tx.amount }.toFloat() }
        val allMonthIndexesWithData = (incomeByMonthIndex.keys + expensesByMonthIndex.keys).distinct().sorted()
        val finalMonthlyIncome = LinkedHashMap<String, Float>()
        val finalMonthlyExpenses = LinkedHashMap<String, Float>()
        for (monthIndex in allMonthIndexesWithData) {
            val monthName = monthNames[monthIndex]
            finalMonthlyIncome[monthName] = incomeByMonthIndex[monthIndex] ?: 0f
            finalMonthlyExpenses[monthName] = expensesByMonthIndex[monthIndex] ?: 0f
        }
        ChartData(finalMonthlyIncome, finalMonthlyExpenses)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChartData()
    )

    fun insert(unitUsaha: UnitUsaha) = viewModelScope.launch {
        // Buat salinan objek dan berikan ID unik sebelum disimpan
        val newUnitUsaha = unitUsaha.copy(id = UUID.randomUUID().toString())
        unitUsahaRepository.insert(newUnitUsaha)
    }

    // --- LOGIKA LAPORAN YANG DIPERBAIKI SECARA TOTAL ---
    private val _reportData = MutableStateFlow(ReportData())
    val reportData: StateFlow<ReportData> = _reportData.asStateFlow()

    private val _filteredReportTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val filteredReportTransactions: StateFlow<List<Transaction>> = _filteredReportTransactions.asStateFlow()

    private var filterJob: Job? = null

    fun generateReport(startDate: Long, endDate: Long, unitUsaha: UnitUsaha?) {
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            val (income, expenses) = transactionRepository.getReportData(startDate, endDate, unitUsaha?.id)
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

            transactionRepository.getFilteredTransactions(startDate, endDate, unitUsaha?.id)
                .collect { filteredList ->
                    _filteredReportTransactions.value = filteredList
                }
        }
    }

    fun clearReport() {
        _reportData.value = ReportData()
        _filteredReportTransactions.value = emptyList()
        filterJob?.cancel()
    }

    fun getTransactionById(id: Int): Flow<Transaction?> = transactionRepository.getTransactionById(id)
    fun insert(transaction: Transaction) = viewModelScope.launch { transactionRepository.insert(transaction) }
    fun update(transaction: Transaction) = viewModelScope.launch { transactionRepository.update(transaction) }
    fun delete(transaction: Transaction) = viewModelScope.launch { transactionRepository.delete(transaction) }
    val allUnitUsaha: Flow<List<UnitUsaha>> = unitUsahaRepository.allUnitUsaha
    fun update(unitUsaha: UnitUsaha) = viewModelScope.launch { unitUsahaRepository.update(unitUsaha) }
    fun delete(unitUsaha: UnitUsaha) = viewModelScope.launch { unitUsahaRepository.delete(unitUsaha) }
}