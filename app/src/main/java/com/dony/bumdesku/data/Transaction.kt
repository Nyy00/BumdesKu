package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0, // Properti ini yang paling penting
    var id: String = "",
    var userId: String = "",
    val amount: Double = 0.0,
    val type: String = "",
    val category: String = "",
    val description: String = "",
    val date: Long = 0L,
    val unitUsahaId: String = ""
){
    // Constructor kosong ini wajib ada untuk Firestore
    constructor() : this(0, "", "", 0.0, "", "", "", 0L, "")
}