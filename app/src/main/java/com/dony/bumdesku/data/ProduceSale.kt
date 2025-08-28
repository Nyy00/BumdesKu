package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "produce_sales")
data class ProduceSale(
    @PrimaryKey
    var id: String = "",

    val itemsJson: String = "",
    val totalPrice: Double = 0.0,
    val transactionDate: Long = 0L,
    val userId: String = "",
    val unitUsahaId: String = ""
)