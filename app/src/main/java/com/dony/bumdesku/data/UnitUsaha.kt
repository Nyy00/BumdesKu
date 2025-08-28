package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Enum untuk kategori unit usaha
enum class UnitUsahaType {
    TOKO,
    WARKOP,
    JASA_SEWA,
    JASA_PEMBAYARAN,
    AGRIBISNIS,
    UMUM // Default
}

@Entity(tableName = "unit_usaha")
data class UnitUsaha(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,
    var id: String = "",
    var userId: String = "",
    val name: String = "",
    val type: UnitUsahaType = UnitUsahaType.UMUM
){
    // Constructor kosong untuk Firestore
    constructor() : this(0, "", "", "", UnitUsahaType.UMUM)
}