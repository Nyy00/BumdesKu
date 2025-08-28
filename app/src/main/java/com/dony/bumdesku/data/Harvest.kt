package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "harvests")
data class Harvest(
    @PrimaryKey
    var id: String = "",

    var userId: String = "",
    val unitUsahaId: String = "",
    val name: String = "",
    val quantity: Double = 0.0,
    val unit: String = "Kg",
    val harvestDate: Long = 0L,
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0
)