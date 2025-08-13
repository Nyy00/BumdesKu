package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "rental_items")
data class RentalItem(
    @PrimaryKey
    var id: String = "",
    val name: String = "",
    val description: String = "",
    val rentalPricePerDay: Double = 0.0,
    val lateFeePerDay: Double = 0.0,

    // --- KOLOM LAMA DIGANTI DENGAN INI ---
    var stockBaik: Int = 0,         // Stok kondisi "Baik" (bisa disewakan)
    var stockRusakRingan: Int = 0,  // Stok "Rusak Ringan"
    var stockPerluPerbaikan: Int = 0, // Stok "Perlu Perbaikan"
    // ------------------------------------

    val unitUsahaId: String = ""
) {
    // Properti bantuan untuk kemudahan, diabaikan oleh Room & Firestore
    @Ignore
    fun getTotalStock(): Int {
        return stockBaik + stockRusakRingan + stockPerluPerbaikan
    }

    // Hanya stok "Baik" yang bisa disewakan
    @Ignore
    fun getAvailableStock(): Int {
        return stockBaik
    }
}
