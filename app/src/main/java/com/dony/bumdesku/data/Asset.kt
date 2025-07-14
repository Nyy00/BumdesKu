package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0, // ID unik untuk database lokal (Room)

    var id: String = "",          // ID dari Cloud Firestore
    var userId: String = "",      // ID pengguna yang membuat data ini
    val name: String = "",        // Nama aset/barang, cth: "Kursi Plastik"
    val description: String = "", // Deskripsi tambahan
    val quantity: Int = 0,        // Jumlah/stok barang
    val purchasePrice: Double = 0.0, // Harga beli per unit
    val imageUrl: String = ""     // URL gambar dari Firebase Storage
) {
    // Constructor kosong ini wajib ada untuk Firestore
    constructor() : this(0, "", "", "", "", 0, 0.0, "")
}