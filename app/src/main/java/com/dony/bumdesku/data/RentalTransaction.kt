package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rental_transactions")
data class RentalTransaction(
    @PrimaryKey
    var id: String = "",
    val customerName: String = "",
    val rentalItemId: String = "",
    val itemName: String = "",
    val quantity: Int = 0,
    val rentalDate: Long = 0L,
    val expectedReturnDate: Long = 0L,
    val returnDate: Long? = null,
    val totalPrice: Double = 0.0,
    val pricePerDay: Double = 0.0,
    val status: String = "",
    val notesOnReturn: String = "",

    // Tambahkan dua field ini
    val unitUsahaId: String = "",
    val userId: String = ""
)