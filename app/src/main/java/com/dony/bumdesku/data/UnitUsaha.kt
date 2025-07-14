package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unit_usaha")
data class UnitUsaha(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0, // Properti ini yang dibutuhkan
    var id: String = "",
    var userId: String = "",
    val name: String = ""
){
    // Constructor kosong ini wajib ada untuk Firestore
    constructor() : this(0, "", "", "")
}