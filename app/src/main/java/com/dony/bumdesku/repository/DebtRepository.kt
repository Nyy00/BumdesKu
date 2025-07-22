package com.dony.bumdesku.repository

import android.util.Log
import com.dony.bumdesku.data.DebtDao
import com.dony.bumdesku.data.Payable
import com.dony.bumdesku.data.Receivable
import com.dony.bumdesku.data.UserProfile
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class DebtRepository(private val debtDao: DebtDao) {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val scope = CoroutineScope(Dispatchers.IO)

    val allPayables: Flow<List<Payable>> = debtDao.getAllPayables()
    val allReceivables: Flow<List<Receivable>> = debtDao.getAllReceivables()

    // ✅ FUNGSI BARU UNTUK EDIT
    fun getPayableById(id: Int): Flow<Payable?> = debtDao.getPayableById(id)
    fun getReceivableById(id: Int): Flow<Receivable?> = debtDao.getReceivableById(id)

    // ... sisa kode repositori tetap sama ...
    fun syncPayables(targetUserId: String) {
        firestore.collection("payables").whereEqualTo("userId", targetUserId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("DebtRepository", "Payable listen failed.", e)
                    return@addSnapshotListener
                }

                scope.launch {
                    val firestorePayables = snapshots?.mapNotNull { doc ->
                        doc.toObject<Payable>().apply { id = doc.id }
                    } ?: emptyList()
                    debtDao.deleteAllPayables()
                    debtDao.insertAllPayables(firestorePayables)
                }
            }
    }

    fun syncReceivables(targetUserId: String) {
        firestore.collection("receivables").whereEqualTo("userId", targetUserId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("DebtRepository", "Receivable listen failed.", e)
                    return@addSnapshotListener
                }

                scope.launch {
                    val firestoreReceivables = snapshots?.mapNotNull { doc ->
                        doc.toObject<Receivable>().apply { id = doc.id }
                    } ?: emptyList()
                    debtDao.deleteAllReceivables()
                    debtDao.insertAllReceivables(firestoreReceivables)
                }
            }
    }

    suspend fun insert(payable: Payable) {
        val userId = auth.currentUser?.uid ?: throw Exception("User tidak login")
        val newPayable = payable.copy(
            userId = userId
        )
        val docRef = firestore.collection("payables").add(newPayable).await()
        update(newPayable.copy(id = docRef.id))
    }

    suspend fun update(payable: Payable) {
        if (payable.id.isBlank()) return
        // Baris yang ditambahkan
        debtDao.updatePayable(payable)
        firestore.collection("payables").document(payable.id).set(payable).await()
    }

    suspend fun delete(payable: Payable) {
        if (payable.id.isNotBlank()) {
            firestore.collection("payables").document(payable.id).delete().await()
        }
    }

    suspend fun insert(receivable: Receivable) {
        val userId = auth.currentUser?.uid ?: throw Exception("User tidak login")
        val newReceivable = receivable.copy(
            userId = userId
        )
        val docRef = firestore.collection("receivables").add(newReceivable).await()
        update(newReceivable.copy(id = docRef.id))
    }

    suspend fun update(receivable: Receivable) {
        if (receivable.id.isBlank()) return
        // Baris yang ditambahkan
        debtDao.updateReceivable(receivable)
        firestore.collection("receivables").document(receivable.id).set(receivable).await()
    }

    suspend fun delete(receivable: Receivable) {
        if (receivable.id.isNotBlank()) {
            firestore.collection("receivables").document(receivable.id).delete().await()
        }
    }
}
