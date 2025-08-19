package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude

@Entity(tableName = "rental_items")
data class RentalItem(
    @PrimaryKey
    var id: String = "",
    val name: String = "",
    val description: String = "",
    val rentalPricePerDay: Double = 0.0,
    val lateFeePerDay: Double = 0.0,

    var stockBaik: Int = 0,
    var stockRusakRingan: Int = 0,
    var stockPerluPerbaikan: Int = 0,

    val unitUsahaId: String = ""
) {
    @Ignore
    @Exclude
    fun getTotalStock(): Int {
        return stockBaik + stockRusakRingan + stockPerluPerbaikan
    }

    @Ignore
    @Exclude
    fun getAvailableStock(): Int {
        return stockBaik
    }
}