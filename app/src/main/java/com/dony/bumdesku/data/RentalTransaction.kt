package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude

enum class PaymentStatus {
    LUNAS,
    BELUM_LUNAS,
    DP
}

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
    val unitUsahaId: String = "",
    val userId: String = "",
    val customerId: String = "",
    val paymentStatus: PaymentStatus = PaymentStatus.BELUM_LUNAS,
    val downPayment: Double = 0.0
) {
    @Ignore
    @Exclude
    fun isOverdue(): Boolean {
        // Hanya cek jika status belum selesai
        return status != "Selesai" && expectedReturnDate < System.currentTimeMillis()
    }

    @Ignore
    @Exclude
    fun isDueSoon(): Boolean {
        val now = System.currentTimeMillis()
        val oneDayInMillis = 24 * 60 * 60 * 1000
        // Cek jika status belum selesai dan tanggal kembali dalam 24 jam ke depan
        return status != "Selesai" && expectedReturnDate > now && expectedReturnDate <= now + oneDayInMillis
    }
}