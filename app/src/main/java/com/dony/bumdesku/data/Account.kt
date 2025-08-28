package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountCategory {
    ASET,
    KEWAJIBAN,
    MODAL,
    PENDAPATAN,
    BEBAN
}

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey
    var id: String = "",

    var userId: String = "",
    val accountNumber: String = "",
    val accountName: String = "",

    val category: AccountCategory
) {
    constructor() : this("", "", "", "", AccountCategory.ASET)
}