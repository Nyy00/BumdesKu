package com.dony.bumdesku.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: Customer)

    @Delete
    suspend fun delete(customer: Customer)

    @Query("SELECT * FROM customers WHERE unitUsahaId = :unitUsahaId ORDER BY name ASC")
    fun getCustomersByUnit(unitUsahaId: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: String): Customer?

    @Transaction
    suspend fun syncCustomers(unitUsahaId: String, customers: List<Customer>) {
        deleteCustomersByUnitId(unitUsahaId)
        insertAllCustomers(customers)
    }

    @Query("DELETE FROM customers WHERE unitUsahaId = :unitUsahaId")
    suspend fun deleteCustomersByUnitId(unitUsahaId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCustomers(customers: List<Customer>)
}