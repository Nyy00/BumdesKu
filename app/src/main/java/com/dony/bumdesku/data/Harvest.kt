package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "harvests")
data class Harvest(
    @PrimaryKey
    var id: String = "", // ID unik dari Firestore

    var userId: String = "",
    val unitUsahaId: String = "",
    val name: String = "", // Nama hasil panen, cth: "Padi Pandan Wangi"
    val quantity: Double = 0.0, // Jumlah (misal: dalam Kg atau Ton)
    val unit: String = "Kg", // Satuan, cth: "Kg", "Ton", "Ikat"
    val harvestDate: Long = 0L, // Tanggal panen
    val costPrice: Double = 0.0, // Modal per satuan (jika ada)
    val sellingPrice: Double = 0.0 // Harga jual per satuan
)