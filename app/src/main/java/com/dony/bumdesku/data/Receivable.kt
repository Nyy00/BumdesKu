package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receivables") // Piutang (yang akan kita terima)
data class Receivable(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,
    var id: String = "",
    var userId: String = "",

    val contactName: String,     // Nama pelanggan/debitor
    val description: String,     // Deskripsi piutang
    val amount: Double,          // Jumlah piutang
    val transactionDate: Long,   // Tanggal piutang dibuat
    val dueDate: Long,           // Tanggal jatuh tempo
    val isPaid: Boolean = false  // Status lunas atau belum
)