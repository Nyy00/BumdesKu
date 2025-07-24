package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val itemsJson: String, // Menyimpan daftar item yang terjual dalam format teks JSON
    val totalPrice: Double,
    val transactionDate: Long,
    val userId: String,
    val unitUsahaId: String
)