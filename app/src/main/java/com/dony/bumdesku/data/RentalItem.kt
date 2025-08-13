package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rental_items")
data class RentalItem(
    @PrimaryKey
    var id: String = "",
    val name: String = "",
    val description: String = "",
    val rentalPricePerDay: Double = 0.0,
    val lateFeePerDay: Double = 0.0,
    val totalStock: Int = 0,
    var availableStock: Int = 0,
    val unitUsahaId: String = ""
)