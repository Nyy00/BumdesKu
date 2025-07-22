package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName // ✅ TAMBAHKAN IMPORT INI

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,
    var id: String = "",
    var userId: String = "",

    val description: String = "",
    val amount: Double = 0.0,
    val date: Long = 0L,

    val debitAccountId: String = "",
    val creditAccountId: String = "",

    val debitAccountName: String = "",
    val creditAccountName: String = "",

    val unitUsahaId: String = "",

    // ✅ TAMBAHKAN ANOTASI INI
    @get:PropertyName("locked") @set:PropertyName("locked")
    var isLocked: Boolean = false
) {
    // Constructor kosong ini sekarang bisa dihapus karena sudah ada nilai default,
    // tapi tidak masalah jika tetap ada.
    constructor() : this(0, "", "", "", 0.0, 0L, "", "", "", "", "", false)
}
