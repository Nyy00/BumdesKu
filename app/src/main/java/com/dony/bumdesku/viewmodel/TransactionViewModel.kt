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

class TransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val unitUsahaRepository: UnitUsahaRepository,
) : ViewModel() {

    // --- Logika Transaksi & Dashboard ---
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

    fun getTransactionById(id: Int): Flow<Transaction?> = transactionRepository.getTransactionById(id)
    fun insert(transaction: Transaction) = viewModelScope.launch { transactionRepository.insert(transaction) }
    fun update(transaction: Transaction) = viewModelScope.launch { transactionRepository.update(transaction) }
    fun delete(transaction: Transaction) = viewModelScope.launch { transactionRepository.delete(transaction) }


    // --- Logika Unit Usaha ---
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

    // --- LOGIKA LAPORAN YANG DIPERBARUI ---

    // State untuk menyimpan hasil kalkulasi laporan
    private val _reportData = MutableStateFlow(ReportData())
    val reportData: StateFlow<ReportData> = _reportData.asStateFlow()

    // State BARU untuk menyimpan daftar transaksi laporan
    private val _reportTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val reportTransactions: StateFlow<List<Transaction>> = _reportTransactions.asStateFlow()


    // Fungsi yang dipanggil UI untuk membuat laporan
    fun generateReport(startDate: Long, endDate: Long) {
        viewModelScope.launch {
            // Ambil data kalkulasi
            val (income, expenses) = transactionRepository.getReportData(startDate, endDate)
            _reportData.value = ReportData(
                totalIncome = income,
                totalExpenses = expenses,
                netProfit = income - expenses,
                isGenerated = true
            )

            // Ambil daftar transaksi untuk periode yang sama dan update StateFlow
            transactionRepository.getTransactionsByDateRange(startDate, endDate)
                .collect { transactions ->
                    _reportTransactions.value = transactions
                }
        }
    }

    // Fungsi untuk mereset laporan saat halaman ditutup/dibuka kembali
    fun clearReport() {
        _reportData.value = ReportData()
        _reportTransactions.value = emptyList() // Reset juga daftar transaksinya
    }
}