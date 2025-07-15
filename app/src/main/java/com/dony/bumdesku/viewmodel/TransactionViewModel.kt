package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.DashboardData
import com.dony.bumdesku.data.ReportData
import com.dony.bumdesku.data.Transaction
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.repository.TransactionRepository
import com.dony.bumdesku.repository.UnitUsahaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.LinkedHashMap // Diperlukan untuk peta yang menjaga urutan

// Data class untuk menampung data grafik
data class ChartData(
    val monthlyIncome: Map<String, Float> = emptyMap(),
    val monthlyExpenses: Map<String, Float> = emptyMap()
)

class TransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val unitUsahaRepository: UnitUsahaRepository,
) : ViewModel() {

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

    // --- LOGIKA GRAFIK YANG SUDAH DIPERBAIKI ---
    val chartData: StateFlow<ChartData> = allTransactions.map { transactions ->
        val calendar = Calendar.getInstance()
        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Ags", "Sep", "Okt", "Nov", "Des")

        // 1. Kelompokkan data berdasarkan indeks bulan (0-11)
        val incomeByMonthIndex = transactions
            .filter { it.type == "PEMASUKAN" }
            .groupBy {
                calendar.timeInMillis = it.date
                calendar.get(Calendar.MONTH)
            }.mapValues { it.value.sumOf { tx -> tx.amount }.toFloat() }

        val expensesByMonthIndex = transactions
            .filter { it.type == "PENGELUARAN" }
            .groupBy {
                calendar.timeInMillis = it.date
                calendar.get(Calendar.MONTH)
            }.mapValues { it.value.sumOf { tx -> tx.amount }.toFloat() }

        // 2. Gabungkan semua bulan yang memiliki data dan urutkan
        val allMonthIndexesWithData = (incomeByMonthIndex.keys + expensesByMonthIndex.keys).distinct().sorted()

        // 3. Buat peta data final dengan urutan yang benar
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
    // --- END OF CHART LOGIC ---


    // --- Other functions (no changes needed) ---
    fun getTransactionById(id: Int): Flow<Transaction?> = transactionRepository.getTransactionById(id)

    fun insert(transaction: Transaction) = viewModelScope.launch {
        transactionRepository.insert(transaction)
    }

    fun update(transaction: Transaction) = viewModelScope.launch {
        transactionRepository.update(transaction)
    }

    fun delete(transaction: Transaction) = viewModelScope.launch {
        transactionRepository.delete(transaction)
    }

    val allUnitUsaha: Flow<List<UnitUsaha>> = unitUsahaRepository.allUnitUsaha

    fun insert(unitUsaha: UnitUsaha) = viewModelScope.launch {
        unitUsahaRepository.insert(unitUsaha)
    }

    fun update(unitUsaha: UnitUsaha) = viewModelScope.launch {
        unitUsahaRepository.update(unitUsaha)
    }

    fun delete(unitUsaha: UnitUsaha) = viewModelScope.launch {
        unitUsahaRepository.delete(unitUsaha)
    }

    private val _reportData = MutableStateFlow(ReportData())
    val reportData: StateFlow<ReportData> = _reportData.asStateFlow()

    fun generateReport(startDate: Long, endDate: Long) {
        viewModelScope.launch {
            val (income, expenses) = transactionRepository.getReportData(startDate, endDate)
            _reportData.value = ReportData(
                totalIncome = income,
                totalExpenses = expenses,
                netProfit = income - expenses,
                isGenerated = true,
                startDate = startDate,
                endDate = endDate
            )
        }
    }

    fun clearReport() {
        _reportData.value = ReportData()
    }
}