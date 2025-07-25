package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey // ✅ UBAH: Jadikan 'id' sebagai Primary Key
    var id: String = "", // Jadikan 'var' dan tipe data String

    // localId tidak lagi dibutuhkan sebagai Primary Key
    var userId: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val date: Long = 0L,
    val debitAccountId: String = "",
    val creditAccountId: String = "",
    val debitAccountName: String = "",
    val creditAccountName: String = "",
    val unitUsahaId: String = "",
    @get:PropertyName("locked") @set:PropertyName("locked")
    var isLocked: Boolean = false
) {
    // Konstruktor kosong tetap diperlukan
    constructor() : this("", "", "", 0.0, 0L, "", "", "", "", "", false)
}