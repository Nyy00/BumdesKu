package com.dony.bumdesku.data

enum class HealthStatus {
    SEHAT,
    WASPADA,
    TIDAK_SEHAT,
    TIDAK_TERDEFINISI
}

data class FinancialHealthData(
    val currentRatio: Float = 0f,
    val status: HealthStatus = HealthStatus.TIDAK_TERDEFINISI
)