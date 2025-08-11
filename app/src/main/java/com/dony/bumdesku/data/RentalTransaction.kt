package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rental_transactions")
data class RentalTransaction(
    @PrimaryKey
    val id: String = "",
    val customerName: String = "",
    val rentalItemId: String = "",
    val itemName: String = "", // Denormalisasi untuk kemudahan tampilan
    val quantity: Int = 1,
    val rentalDate: Long = System.currentTimeMillis(),
    var returnDate: Long? = null, // Diisi saat barang dikembalikan
    val pricePerDay: Double = 0.0,
    var totalPrice: Double = 0.0,
    var status: String = "Disewa", // Status: "Disewa" atau "Selesai"
    val unitUsahaId: String = ""
)