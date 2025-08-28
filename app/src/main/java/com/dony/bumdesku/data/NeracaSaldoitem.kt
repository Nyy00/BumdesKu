package com.dony.bumdesku.data

data class NeracaSaldoItem(
    val accountId: String,
    val accountNumber: String,
    val accountName: String,
    val totalDebit: Double,
    val totalKredit: Double
)