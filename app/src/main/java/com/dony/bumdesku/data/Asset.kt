package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey // ✅ UBAH: Jadikan 'id' sebagai Primary Key
    var id: String = "", // Jadikan 'var' agar bisa diisi dari Firestore

    // localId tidak lagi dibutuhkan sebagai Primary Key
    var userId: String = "",
    val unitUsahaId: String = "",
    val name: String = "",
    val description: String = "",
    val quantity: Int = 0,
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val imageUrl: String = "",
    val category: String = "Lain-lain"
) {
    // Konstruktor kosong tetap diperlukan untuk Firestore
    constructor() : this("", "", "", "", "", 0, 0.0, 0.0, "", "Lain-lain")
}