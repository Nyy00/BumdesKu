package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val itemsJson: String = "", // Beri nilai default string kosong
    val totalPrice: Double = 0.0, // Beri nilai default 0.0
    val transactionDate: Long = 0L, // Beri nilai default 0L
    val userId: String = "", // Beri nilai default string kosong
    val unitUsahaId: String = "" // Beri nilai default string kosong
) {
    // Anda bisa menambahkan konstruktor kosong secara eksplisit seperti ini,
    // tapi dengan memberi nilai default di atas, ini tidak lagi diperlukan.
    // constructor() : this(0, "", 0.0, 0L, "", "")
}