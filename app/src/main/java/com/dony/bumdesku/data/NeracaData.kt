package com.dony.bumdesku.data

data class NeracaData(
    val asetItems: List<NeracaItem> = emptyList(),
    val kewajibanItems: List<NeracaItem> = emptyList(),
    val modalItems: List<NeracaItem> = emptyList(),

    val totalAset: Double = 0.0,
    val totalKewajiban: Double = 0.0,
    val totalModal: Double = 0.0
)