package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "payables")
data class Payable(
    @PrimaryKey
    var id: String = "", // Jadikan ID Firestore sebagai Primary Key

    var userId: String = "",
    val unitUsahaId: String = "", // ✅ TAMBAHKAN FIELD INI
    val contactName: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val transactionDate: Long = 0L,
    val dueDate: Long = 0L,
    @get:PropertyName("isPaid") @set:PropertyName("isPaid")
    var isPaid: Boolean = false
)