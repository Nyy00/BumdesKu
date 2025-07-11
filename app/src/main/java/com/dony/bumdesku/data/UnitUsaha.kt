package com.dony.bumdesku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unit_usaha")
data class UnitUsaha(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
)