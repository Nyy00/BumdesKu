package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "harvests")
data class Harvest(
    @PrimaryKey
    var id: String = "",

    var userId: String = "", // ✅ Beri nilai default
    val unitUsahaId: String = "", // ✅ Beri nilai default
    val name: String = "", // ✅ Beri nilai default
    val quantity: Double = 0.0, // ✅ Beri nilai default
    val unit: String = "Kg", // ✅ Beri nilai default
    val harvestDate: Long = 0L, // ✅ Beri nilai default
    val costPrice: Double = 0.0, // ✅ Beri nilai default
    val sellingPrice: Double = 0.0 // ✅ Beri nilai default
)