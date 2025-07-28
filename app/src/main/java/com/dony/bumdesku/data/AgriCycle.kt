package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

// Enum untuk status siklus (tidak ada perubahan)
enum class CycleStatus {
    BERJALAN,
    SELESAI
}

// Data class ProductionCycle (tidak ada perubahan)
@Entity(tableName = "production_cycles")
data class ProductionCycle(
    @PrimaryKey
    var id: String = "",
    var userId: String = "",
    val unitUsahaId: String = "",
    val name: String = "",
    val startDate: Long = 0L,
    var endDate: Long? = null,
    var totalCost: Double = 0.0,
    var totalHarvest: Double = 0.0,
    var hppPerUnit: Double = 0.0,
    @get:PropertyName("isArchived") @set:PropertyName("isArchived")
    var isArchived: Boolean = false,
    val status: CycleStatus = CycleStatus.BERJALAN
)

// Data class CycleCost (DIPERBARUI)
@Entity(tableName = "cycle_costs")
data class CycleCost(
    @PrimaryKey
    var id: String = "",
    var userId: String = "",
    val cycleId: String = "",
    val unitUsahaId: String = "", // <-- TAMBAHKAN FIELD INI
    val date: Long = 0L,
    val description: String = "",
    val amount: Double = 0.0,
    val costCategoryId: String = ""
)
