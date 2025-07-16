package com.dony.bumdesku.data

data class ReportData(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val isGenerated: Boolean = false,
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val unitUsahaName: String = "Semua Unit Usaha",
    val unitUsahaId: String? = null
)