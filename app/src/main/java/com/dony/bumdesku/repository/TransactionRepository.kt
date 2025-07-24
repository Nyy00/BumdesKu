package com.dony.bumdesku.repository

import android.util.Log
import com.dony.bumdesku.data.Transaction
import com.dony.bumdesku.data.TransactionDao
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

class TransactionRepository(
    private val transactionDao: TransactionDao
) {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val scope = CoroutineScope(Dispatchers.IO)

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun syncTransactions(targetUserId: String) {
        scope.launch {
            try {
                val userProfileSnapshot = firestore.collection("users").document(targetUserId).get().await()
                val userProfile = userProfileSnapshot.toObject<UserProfile>()

                if (userProfile == null) {
                    Log.w("TransactionRepository", "Profil untuk $targetUserId tidak ditemukan.")
                    return@launch
                }

                // --- LOGIKA BARU UNTUK PERAN YANG BERBEDA ---
                val query = if (userProfile.role == "manager" || userProfile.role == "auditor") {
                    // Jika manajer atau auditor, ambil SEMUA transaksi
                    Log.d("TransactionRepository", "Mode Manajer/Auditor: Mengambil semua transaksi.")
                    firestore.collection("transactions")
                } else {
                    // Jika pengurus biasa, ambil transaksi berdasarkan unit usaha yang dikelola
                    val managedIds = userProfile.managedUnitUsahaIds
                    if (managedIds.isEmpty()) {
                        Log.w("TransactionRepository", "Pengurus $targetUserId tidak punya unit usaha, tidak ada transaksi untuk disinkronkan.")
                        transactionDao.deleteAll()
                        return@launch
                    }
                    Log.d("TransactionRepository", "Mode Pengurus: Mengambil transaksi untuk unit ${managedIds}.")
                    firestore.collection("transactions").whereIn("unitUsahaId", managedIds.take(10))
                }
                // ---------------------------------------------

                query.addSnapshotListener { snapshots, e ->
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
                        Log.d("TransactionRepository", "Sinkronisasi berhasil: ${firestoreTransactions.size} transaksi diterima.")
                    }
                }

            } catch (e: Exception) {
                Log.e("TransactionRepository", "Gagal melakukan sinkronisasi transaksi: ", e)
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

    fun getTransactionById(id: Int): Flow<Transaction?> {
        return transactionDao.getTransactionById(id)
    }

    suspend fun lockTransactionsUpTo(date: Long) {
        transactionDao.lockTransactionsUpTo(date)
    }
}