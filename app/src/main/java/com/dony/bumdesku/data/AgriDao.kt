package com.dony.bumdesku.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AgriDao {
    // --- Operasi untuk Stok Panen (Harvest) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHarvests(harvests: List<Harvest>)

    @Query("SELECT * FROM harvests ORDER BY harvestDate DESC")
    fun getAllHarvests(): Flow<List<Harvest>>

    @Query("DELETE FROM harvests")
    suspend fun deleteAllHarvests()

    // --- Operasi untuk Penjualan Hasil Panen (ProduceSale) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProduceSales(sales: List<ProduceSale>)

    @Query("SELECT * FROM produce_sales ORDER BY transactionDate DESC")
    fun getAllProduceSales(): Flow<List<ProduceSale>>

    @Query("DELETE FROM produce_sales")
    suspend fun deleteAllProduceSales()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAgriInventory(inventory: List<AgriInventory>)

    @Query("SELECT * FROM agri_inventory ORDER BY purchaseDate DESC")
    fun getAllAgriInventory(): Flow<List<AgriInventory>>

    @Query("DELETE FROM agri_inventory")
    suspend fun deleteAllAgriInventory()

    @Query("SELECT * FROM agri_inventory WHERE id = :id")
    fun getInventoryById(id: String): Flow<AgriInventory?>
}