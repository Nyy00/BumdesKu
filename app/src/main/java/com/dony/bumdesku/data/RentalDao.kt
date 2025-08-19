package com.dony.bumdesku.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RentalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRentalItem(item: RentalItem)

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRentalTransaction(transaction: RentalTransaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRentalTransactions(transactions: List<RentalTransaction>)

    @Update
    suspend fun updateRentalTransaction(transaction: RentalTransaction)

    @Query("SELECT * FROM rental_transactions WHERE unitUsahaId = :unitUsahaId ORDER BY rentalDate DESC")
    fun getAllRentalTransactions(unitUsahaId: String): Flow<List<RentalTransaction>>

    @Transaction
    suspend fun syncItems(unitUsahaId: String, items: List<RentalItem>) {
        deleteItemsByUnitId(unitUsahaId)
        insertAllRentalItems(items)
    }

    @Transaction
    suspend fun syncTransactions(unitUsahaId: String, transactions: List<RentalTransaction>) {
        deleteTransactionsByUnitId(unitUsahaId)
        insertAllRentalTransactions(transactions)
    }

    @Query("DELETE FROM rental_items WHERE unitUsahaId = :unitUsahaId")
    suspend fun deleteItemsByUnitId(unitUsahaId: String)

    @Query("DELETE FROM rental_transactions WHERE unitUsahaId = :unitUsahaId")
    suspend fun deleteTransactionsByUnitId(unitUsahaId: String)

    @Query("SELECT * FROM rental_transactions WHERE id = :id")
    fun getRentalTransactionById(id: String): Flow<RentalTransaction?>
}