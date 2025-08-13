package com.dony.bumdesku.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RentalDao {
    // --- Operasi untuk RentalItem ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRentalItem(item: RentalItem)

    // ✅ TAMBAHKAN FUNGSI INI
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRentalItems(items: List<RentalItem>)

    @Update
    suspend fun updateRentalItem(item: RentalItem)

    @Delete
    suspend fun deleteRentalItem(item: RentalItem)

    @Query("SELECT * FROM rental_items WHERE unitUsahaId = :unitUsahaId")
    fun getAllRentalItems(unitUsahaId: String): Flow<List<RentalItem>>

    @Query("SELECT * FROM rental_items WHERE id = :id")
    suspend fun getRentalItemById(id: String): RentalItem?

    // --- Operasi untuk RentalTransaction ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRentalTransaction(transaction: RentalTransaction)

    // ✅ TAMBAHKAN FUNGSI INI
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRentalTransactions(transactions: List<RentalTransaction>)

    @Update
    suspend fun updateRentalTransaction(transaction: RentalTransaction)

    @Query("SELECT * FROM rental_transactions WHERE unitUsahaId = :unitUsahaId ORDER BY rentalDate DESC")
    fun getAllRentalTransactions(unitUsahaId: String): Flow<List<RentalTransaction>>
}