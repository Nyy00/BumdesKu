package com.dony.bumdesku.data

// Mewakili satu baris di laporan Neraca Saldo
data class NeracaSaldoItem(
    val accountId: String, // TAMBAHKAN BARIS INI
    val accountNumber: String,
    val accountName: String,
    val totalDebit: Double,
    val totalKredit: Double
)