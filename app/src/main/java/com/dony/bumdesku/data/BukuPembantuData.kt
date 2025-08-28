package com.dony.bumdesku.data

data class BukuPembantuData(
    val transactions: List<Transaction> = emptyList(),
    val runningBalances: Map<String, Double> = emptyMap()
)