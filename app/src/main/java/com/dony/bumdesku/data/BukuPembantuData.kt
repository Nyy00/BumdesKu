package com.dony.bumdesku.data

data class BukuPembantuData(
    val transactions: List<Transaction> = emptyList(),
    // ✅ PERBAIKAN DI SINI: Ubah tipe kunci Map dari Int menjadi String
    val runningBalances: Map<String, Double> = emptyMap()
)