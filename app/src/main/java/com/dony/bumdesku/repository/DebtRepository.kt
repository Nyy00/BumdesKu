package com.dony.bumdesku.repository

import android.util.Log
import com.dony.bumdesku.data.DebtDao
import com.dony.bumdesku.data.Payable
import com.dony.bumdesku.data.Receivable
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Repositori untuk mengelola data utang (Payable) dan piutang (Receivable).
 * Arsitektur ini menggunakan pendekatan "Local First":
 * 1. Setiap aksi (insert, update, delete) akan langsung dieksekusi di database lokal (Room)
 * untuk memastikan UI merespons secara instan.
 * 2. Setelah itu, aksi yang sama dikirim ke Firestore untuk sinkronisasi.
 * 3. Listener sinkronisasi dari Firestore bertugas untuk memperbarui data lokal
 * jika ada perubahan dari sumber lain (misal: perangkat lain atau konsol Firebase).
 */
class DebtRepository(private val debtDao: DebtDao) {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val scope = CoroutineScope(Dispatchers.IO)

    // Sumber data utama untuk UI, selalu dari database lokal.
    val allPayables: Flow<List<Payable>> = debtDao.getAllPayables()
    val allReceivables: Flow<List<Receivable>> = debtDao.getAllReceivables()
    fun getPayableById(id: Int): Flow<Payable?> = debtDao.getPayableById(id)
    fun getReceivableById(id: Int): Flow<Receivable?> = debtDao.getReceivableById(id)

    // --- SINKRONISASI DATA DARI SERVER KE LOKAL ---

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
                    // Timpa data lokal dengan data terbaru dari server.
                    // Ini aman karena aksi lokal sudah dieksekusi terlebih dahulu.
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

    // --- OPERASI CRUD (Create, Read, Update, Delete) ---

    // Utang (Payable)
    suspend fun insert(payable: Payable) {
        val userId = auth.currentUser?.uid ?: throw Exception("User tidak login")
        val docId = firestore.collection("payables").document().id // Buat ID unik di awal
        val newPayable = payable.copy(id = docId, userId = userId)

        // 1. Lakukan aksi di database lokal terlebih dahulu.
        debtDao.insertPayable(newPayable)
        // 2. Kirim aksi ke Firestore.
        firestore.collection("payables").document(newPayable.id).set(newPayable).await()
    }

    suspend fun update(payable: Payable) {
        if (payable.id.isBlank()) return
        // 1. Lakukan aksi di database lokal terlebih dahulu.
        debtDao.updatePayable(payable)
        // 2. Kirim aksi ke Firestore.
        firestore.collection("payables").document(payable.id).set(payable).await()
    }

    suspend fun delete(payable: Payable) {
        if (payable.id.isNotBlank()) {
            // 1. Lakukan aksi di database lokal terlebih dahulu.
            debtDao.deletePayable(payable)
            // 2. Kirim aksi ke Firestore.
            firestore.collection("payables").document(payable.id).delete().await()
        }
    }

    // Piutang (Receivable)
    suspend fun insert(receivable: Receivable) {
        val userId = auth.currentUser?.uid ?: throw Exception("User tidak login")
        val docId = firestore.collection("receivables").document().id
        val newReceivable = receivable.copy(id = docId, userId = userId)

        // 1. Lakukan aksi di database lokal terlebih dahulu.
        debtDao.insertReceivable(newReceivable)
        // 2. Kirim aksi ke Firestore.
        firestore.collection("receivables").document(newReceivable.id).set(newReceivable).await()
    }

    suspend fun update(receivable: Receivable) {
        if (receivable.id.isBlank()) return
        // 1. Lakukan aksi di database lokal terlebih dahulu.
        debtDao.updateReceivable(receivable)
        // 2. Kirim aksi ke Firestore.
        firestore.collection("receivables").document(receivable.id).set(receivable).await()
    }

    suspend fun delete(receivable: Receivable) {
        if (receivable.id.isNotBlank()) {
            // 1. Lakukan aksi di database lokal terlebih dahulu.
            debtDao.deleteReceivable(receivable)
            // 2. Kirim aksi ke Firestore.
            firestore.collection("receivables").document(receivable.id).delete().await()
        }
    }
}