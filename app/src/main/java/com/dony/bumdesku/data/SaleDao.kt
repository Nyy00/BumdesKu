package com.dony.bumdesku.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sale: Sale)

    // --- FUNGSI BARU ---
    @Query("SELECT * FROM sales ORDER BY transactionDate DESC")
    fun getAllSales(): Flow<List<Sale>>
    // -------------------
}