package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountCategory {
    ASET,
    KEWAJIBAN,
    MODAL,
    PENDAPATAN,
    BEBAN
}

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey // ✅ UBAH: Jadikan 'id' sebagai Primary Key
    var id: String = "",

    // localId tidak lagi dibutuhkan
    var userId: String = "",
    val accountNumber: String = "",
    val accountName: String = "",

    val category: AccountCategory
) {
    // Konstruktor kosong tetap diperlukan untuk Firestore
    constructor() : this("", "", "", "", AccountCategory.ASET)
}