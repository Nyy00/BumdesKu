package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Enum untuk kategori utama akun
enum class AccountCategory {
    ASET,
    KEWAJIBAN,
    MODAL,
    PENDAPATAN,
    BEBAN
}

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,

    var id: String = "",          // ID untuk Firestore
    var userId: String = "",      // ID pengguna yang membuat
    val accountNumber: String = "", // Nomor Akun, cth: "111" untuk Kas
    val accountName: String = "",   // Nama Akun, cth: "Kas Tunai"
    val category: AccountCategory   // Kategori utama dari enum di atas
) {
    // Constructor kosong wajib untuk Firestore
    constructor() : this(0, "", "", "", "", AccountCategory.ASET)
}