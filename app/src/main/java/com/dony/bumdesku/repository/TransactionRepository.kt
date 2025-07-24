package com.dony.bumdesku.repository

import android.util.Log
import com.dony.bumdesku.data.Transaction
import com.dony.bumdesku.data.TransactionDao
import com.dony.bumdesku.data.UserProfile
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration // PASTIKAN IMPORT INI ADA
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TransactionRepository(
    private val transactionDao: TransactionDao
) {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val scope = CoroutineScope(Dispatchers.IO)

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun syncTransactionsForUnit(unitId: String): ListenerRegistration {
        return firestore.collection("transactions").whereEqualTo("unitUsahaId", unitId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("TransactionRepository", "Listen for unit transactions failed.", e)
                    return@addSnapshotListener
                }
                scope.launch {
                    val firestoreTransactions = snapshots?.documents?.mapNotNull { doc ->
                        doc.toObject<Transaction>()?.apply { id = doc.id }
                    } ?: emptyList()
                    transactionDao.insertAll(firestoreTransactions)
                }
            }
    }

    suspend fun clearLocalTransactions() {
        transactionDao.deleteAll()
    }

    /**
     * ✅ FUNGSI BARU UNTUK PENGGUNA (PENGURUS)
     */
    fun syncTransactionsForUser(managedUnitIds: List<String>): ListenerRegistration {
        return firestore.collection("transactions").whereIn("unitUsahaId", managedUnitIds.take(30))
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("TransactionRepository", "Listen failed.", e)
                    return@addSnapshotListener
                }
                scope.launch(Dispatchers.IO) {
                    val firestoreTransactions = snapshots?.documents?.mapNotNull { doc ->
                        doc.toObject<Transaction>()?.apply { id = doc.id }
                    } ?: emptyList()

                    transactionDao.deleteAll()
                    transactionDao.insertAll(firestoreTransactions)
                }
            }
    }

    fun syncAllTransactionsForManager(): ListenerRegistration {
        return firestore.collection("transactions")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("TransactionRepository", "Listen for ALL transactions failed.", e)
                    return@addSnapshotListener
                }
                scope.launch(Dispatchers.IO) {
                    val transactions = snapshots?.documents?.mapNotNull { doc ->
                        doc.toObject<Transaction>()?.apply { id = doc.id }
                    } ?: emptyList()
                    transactionDao.deleteAll()
                    transactionDao.insertAll(transactions)
                }
            }
    }

    // FUNGSI syncTransactions(targetUserId: String) YANG LAMA SEKARANG DIHAPUS

    suspend fun insert(transaction: Transaction) {
        val userId = auth.currentUser?.uid ?: throw Exception("User tidak login")
        val newTransaction = transaction.copy(userId = userId)
        firestore.collection("transactions").add(newTransaction).await()
    }

    suspend fun update(transaction: Transaction) {
        if (transaction.id.isNotBlank()) {
            firestore.collection("transactions").document(transaction.id).set(transaction).await()
        }
    }

    suspend fun delete(transaction: Transaction) {
        if (transaction.id.isNotBlank()) {
            firestore.collection("transactions").document(transaction.id).delete().await()
        }
    }

    fun getTransactionById(id: Int): Flow<Transaction?> {
        return transactionDao.getTransactionById(id)
    }

    suspend fun lockTransactionsUpTo(date: Long) {
        transactionDao.lockTransactionsUpTo(date)
    }
}