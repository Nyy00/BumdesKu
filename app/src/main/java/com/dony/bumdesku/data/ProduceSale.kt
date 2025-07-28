package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "produce_sales")
data class ProduceSale(
    @PrimaryKey
    var id: String = "", // ID unik dari Firestore

    val itemsJson: String, // Detail panen yang terjual dalam format JSON
    val totalPrice: Double,
    val transactionDate: Long,
    val userId: String,
    val unitUsahaId: String
)