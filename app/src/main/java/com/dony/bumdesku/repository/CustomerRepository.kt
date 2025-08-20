package com.dony.bumdesku.repository

import android.util.Log
import com.dony.bumdesku.data.Customer
import com.dony.bumdesku.data.CustomerDao
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class CustomerRepository(
    private val customerDao: CustomerDao
) {
    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(Dispatchers.IO)

    fun syncDataForUnit(unitId: String): ListenerRegistration {
        val customerListener = firestore.collection("customers").whereEqualTo("unitUsahaId", unitId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("CustomerRepository", "Listen for customers failed.", e)
                    return@addSnapshotListener
                }
                Log.d("CustomerRepository", "Snapshot listener for customers received ${snapshots?.size() ?: 0} documents.")
                scope.launch {
                    val customers = snapshots?.documents?.mapNotNull { doc ->
                        doc.toObject(Customer::class.java)?.apply { id = doc.id }
                    } ?: emptyList()
                    customerDao.syncCustomers(unitId, customers)
                }
            }
        return customerListener
    }

    fun getCustomers(unitId: String): Flow<List<Customer>> {
        return customerDao.getCustomersByUnit(unitId)
    }

    suspend fun saveCustomer(customer: Customer) {
        withContext(Dispatchers.IO) {
            val customerId = if (customer.id.isBlank()) UUID.randomUUID().toString() else customer.id
            val finalCustomer = customer.copy(id = customerId)
            firestore.collection("customers").document(customerId).set(finalCustomer).await()
        }
    }

    suspend fun deleteCustomer(customer: Customer) {
        withContext(Dispatchers.IO) {
            if (customer.id.isNotBlank()) {
                firestore.collection("customers").document(customer.id).delete().await()
            }
        }
    }
}