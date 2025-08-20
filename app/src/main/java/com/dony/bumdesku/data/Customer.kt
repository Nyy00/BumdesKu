package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val unitUsahaId: String = "" // Menghubungkan pelanggan dengan unit usaha
)