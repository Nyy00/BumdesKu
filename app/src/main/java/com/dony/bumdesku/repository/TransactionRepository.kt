package com.dony.bumdesku.repository

import android.util.Log
import com.dony.bumdesku.data.Transaction
import com.dony.bumdesku.data.TransactionDao
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TransactionRepository(
    private val transactionDao: TransactionDao
) {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val scope = CoroutineScope(Dispatchers.IO)

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun clearLocalTransactions() {
        transactionDao.deleteAll()
    }

    /**
     * ✅ PERBAIKAN UTAMA DI SINI
     * Tambahkan 'transactionDao.deleteAll()' untuk memastikan data lama
     * dibersihkan sebelum data baru dari server dimasukkan.
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

                    // Hapus semua data transaksi lama di lokal
                    transactionDao.deleteAll()
                    // Masukkan daftar transaksi yang baru dan bersih dari server
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

    // ... sisa kode lainnya di file ini tidak perlu diubah ...

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

    fun getTransactionById(id: String): Flow<Transaction?> {
        return transactionDao.getTransactionById(id)
    }

    suspend fun lockTransactionsUpTo(date: Long) {
        transactionDao.lockTransactionsUpTo(date)
    }
}