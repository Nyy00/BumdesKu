package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payables") // Utang (yang harus kita bayar)
data class Payable(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,
    var id: String = "",
    var userId: String = "",

    val contactName: String,     // Nama pemasok/kreditor
    val description: String,     // Deskripsi utang
    val amount: Double,          // Jumlah utang
    val transactionDate: Long,   // Tanggal utang dibuat
    val dueDate: Long,           // Tanggal jatuh tempo
    val isPaid: Boolean = false  // Status lunas atau belum
)