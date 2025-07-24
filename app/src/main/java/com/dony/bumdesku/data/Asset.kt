package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,

    var id: String = "",
    var userId: String = "",
    val name: String = "",
    val description: String = "",
    val quantity: Int = 0,
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val imageUrl: String = "",
    val category: String = "Lain-lain" // <-- TAMBAHKAN PROPERTI KATEGORI
) {
    // Constructor kosong untuk Firestore
    constructor() : this(0, "", "", "", "", 0, 0.0, 0.0, "", "Lain-lain")
}