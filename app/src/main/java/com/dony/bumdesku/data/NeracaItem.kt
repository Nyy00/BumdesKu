package com.dony.bumdesku.data

// Mewakili satu baris di laporan neraca, cth: "Kas Tunai ....... Rp 5.000.000"
data class NeracaItem(
    val accountName: String,
    val balance: Double
)