package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName // 1. Tambahkan import ini

@Entity(tableName = "payables")
data class Payable(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,
    var id: String = "",
    var userId: String = "",
    val contactName: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val transactionDate: Long = 0L,
    val dueDate: Long = 0L,

    // 2. Ubah baris ini
    @get:PropertyName("isPaid") @set:PropertyName("isPaid")
    var isPaid: Boolean = false
)