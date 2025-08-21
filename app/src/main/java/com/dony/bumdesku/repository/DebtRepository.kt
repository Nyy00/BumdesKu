package com.dony.bumdesku.repository

import android.util.Log
import com.dony.bumdesku.data.DebtDao
import com.dony.bumdesku.data.Payable
import com.dony.bumdesku.data.Receivable
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DebtRepository(private val debtDao: DebtDao) {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val scope = CoroutineScope(Dispatchers.IO)

    val allPayables: Flow<List<Payable>> = debtDao.getAllPayables()
    val allReceivables: Flow<List<Receivable>> = debtDao.getAllReceivables()
    fun getPayableById(id: String): Flow<Payable?> = debtDao.getPayableById(id)
    fun getReceivableById(id: String): Flow<Receivable?> = debtDao.getReceivableById(id)

    fun syncPayablesForUser(managedUnitIds: List<String>): ListenerRegistration {
        return firestore.collection("payables").whereIn("unitUsahaId", managedUnitIds.take(30))
            .addSnapshotListener { snapshots, e ->
                handleFirestoreUpdate(e, snapshots, debtDao::deleteAllPayables, debtDao::insertAllPayables, Payable::class.java)
            }
    }

    fun syncReceivablesForUser(managedUnitIds: List<String>): ListenerRegistration {
        return firestore.collection("receivables").whereIn("unitUsahaId", managedUnitIds.take(30))
            .addSnapshotListener { snapshots, e ->
                handleFirestoreUpdate(e, snapshots, debtDao::deleteAllReceivables, debtDao::insertAllReceivables, Receivable::class.java)
            }
    }

    fun syncAllPayablesForManager(): ListenerRegistration {
        return firestore.collection("payables")
            .addSnapshotListener { snapshots, e ->
                handleFirestoreUpdate(e, snapshots, debtDao::deleteAllPayables, debtDao::insertAllPayables, Payable::class.java)
            }
    }

    fun syncAllReceivablesForManager(): ListenerRegistration {
        return firestore.collection("receivables")
            .addSnapshotListener { snapshots, e ->
                handleFirestoreUpdate(e, snapshots, debtDao::deleteAllReceivables, debtDao::insertAllReceivables, Receivable::class.java)
            }
    }

    private inline fun <T : Any> handleFirestoreUpdate(
        e: Exception?,
        snapshots: com.google.firebase.firestore.QuerySnapshot?,
        crossinline deleteAll: suspend () -> Unit,
        crossinline insertAll: suspend (List<T>) -> Unit,
        clazz: Class<T>
    ) {
        if (e != null) {
            Log.w("DebtRepository", "Listen failed.", e)
            return
        }
        scope.launch {
            val firestoreData = snapshots?.mapNotNull { doc ->
                val obj = doc.toObject(clazz)
                when (obj) {
                    is Payable -> obj.id = doc.id
                    is Receivable -> obj.id = doc.id
                }
                obj
            } ?: emptyList()
            deleteAll()
            insertAll(firestoreData)
        }
    }

    suspend fun deleteReceivableById(id: String) {
        withContext(Dispatchers.IO) {
            debtDao.deleteReceivableById(id)

            if (id.isNotBlank()) {
                firestore.collection("receivables").document(id).delete().await()
            }
        }
    }

    suspend fun insert(payable: Payable) {
        val userId = auth.currentUser?.uid ?: throw Exception("User tidak login")
        val docRef = firestore.collection("payables").document()
        val newPayable = payable.copy(id = docRef.id, userId = userId)
        docRef.set(newPayable).await()
    }

    suspend fun update(payable: Payable) {
        if (payable.id.isBlank()) return
        firestore.collection("payables").document(payable.id).set(payable).await()
    }

    suspend fun delete(payable: Payable) {
        if (payable.id.isNotBlank()) {
            firestore.collection("payables").document(payable.id).delete().await()
        }
    }

    suspend fun insert(receivable: Receivable) {
        val userId = auth.currentUser?.uid ?: throw Exception("User tidak login")
        val finalReceivable = receivable.copy(userId = userId)

        firestore.collection("receivables").document(finalReceivable.id).set(finalReceivable).await()
        debtDao.insertReceivable(finalReceivable)
    }

    suspend fun update(receivable: Receivable) {
        if (receivable.id.isBlank()) return

        firestore.collection("receivables").document(receivable.id).set(receivable).await()
        debtDao.updateReceivable(receivable)
    }

    suspend fun delete(receivable: Receivable) {
        if (receivable.id.isNotBlank()) {
            firestore.collection("receivables").document(receivable.id).delete().await()
        }
    }
}