package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agri_inventory")
data class AgriInventory(
    @PrimaryKey
    var id: String = "",
    var userId: String = "",
    val unitUsahaId: String = "",
    val name: String = "",
    val quantity: Double = 0.0,
    val unit: String = "Kg",
    val purchaseDate: Long = 0L,
    val cost: Double = 0.0
)