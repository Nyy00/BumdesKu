package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "produce_sales")
data class ProduceSale(
    @PrimaryKey
    var id: String = "",

    val itemsJson: String = "", // ✅ Beri nilai default
    val totalPrice: Double = 0.0, // ✅ Beri nilai default
    val transactionDate: Long = 0L, // ✅ Beri nilai default
    val userId: String = "", // ✅ Beri nilai default
    val unitUsahaId: String = "" // ✅ Beri nilai default
)