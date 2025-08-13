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
    val quantity: Int = 1,
    val rentalDate: Long = System.currentTimeMillis(),
    val expectedReturnDate: Long = 0L,
    var returnDate: Long? = null,
    val pricePerDay: Double = 0.0,
    var totalPrice: Double = 0.0,
    var status: String = "Disewa",
    val unitUsahaId: String = ""
)