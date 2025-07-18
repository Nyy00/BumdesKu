package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,
    var id: String = "",
    var userId: String = "",

    val description: String = "",
    val amount: Double = 0.0,
    val date: Long = 0L,

    // Kolom 'type' dan 'category' diganti dengan ini
    val debitAccountId: String = "",
    val creditAccountId: String = "",

    // Tambahkan ini untuk mempermudah penampilan nama akun
    val debitAccountName: String = "",
    val creditAccountName: String = "",

    val unitUsahaId: String = "",
    val isLocked: Boolean = false
) {
    // Constructor kosong wajib untuk Firestore
    constructor() : this(0, "", "", "", 0.0, 0L, "", "", "", "", "")
}