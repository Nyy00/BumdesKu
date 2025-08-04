package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "fixed_assets")
data class FixedAsset(
    @PrimaryKey
    var id: String = "",
    var userId: String = "",
    val name: String = "",
    val description: String = "",
    val purchaseDate: Long = 0L,
    val purchasePrice: Double = 0.0,
    val unitUsahaId: String = "",
    // Nilai buku setelah dikurangi penyusutan
    var bookValue: Double = 0.0,
    // Properti untuk masa manfaat (dalam tahun), untuk penyusutan di masa depan
    val usefulLife: Int = 0,
    @get:PropertyName("isSold") @set:PropertyName("isSold")
    var isSold: Boolean = false
)