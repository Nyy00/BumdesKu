package com.dony.bumdesku.repository

import android.util.Log
import com.dony.bumdesku.data.Account
import com.dony.bumdesku.data.AccountDao
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AccountRepository(private val accountDao: AccountDao) {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val scope = CoroutineScope(Dispatchers.IO)

    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()

    // --- FUNGSI SINKRONISASI DIPERBARUI TOTAL ---
    fun syncAccounts(targetUserId: String) {
        // Langkah 1: Cari ID pengguna pertama yang memiliki peran 'manager' atau 'pengurus'.
        // Ini akan menjadi sumber kebenaran (source of truth) untuk Chart of Accounts (COA).
        firestore.collection("users")
            .whereIn("role", listOf("manager", "pengurus"))
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val mainOwnerId = querySnapshot.documents.firstOrNull()?.id
                if (mainOwnerId == null) {
                    Log.e("AccountRepository", "Tidak ditemukan manajer/pengurus utama untuk dijadikan sumber COA.")
                    return@addOnSuccessListener
                }

                // Langkah 2: Dengarkan (listen) perubahan pada daftar akun milik 'mainOwnerId'.
                firestore.collection("accounts").whereEqualTo("userId", mainOwnerId)
                    .addSnapshotListener { snapshots, e ->
                        if (e != null) {
                            Log.w("AccountRepository", "Listen for accounts failed.", e)
                            return@addSnapshotListener
                        }

                        scope.launch(Dispatchers.IO) {
                            val firestoreAccounts = snapshots?.mapNotNull { doc ->
                                doc.toObject<Account>().apply { id = doc.id }
                            } ?: emptyList()

                            // Langkah 3: Hapus semua akun lokal dan ganti dengan daftar terbaru.
                            // Ini memastikan semua pengguna (manajer, pengurus toko, dll.)
                            // memiliki daftar akun (COA) yang sama dan terbaru.
                            accountDao.deleteAll()
                            accountDao.insertAll(firestoreAccounts)
                            Log.d("AccountRepository", "Sinkronisasi COA berhasil, ${firestoreAccounts.size} akun diterima.")
                        }
                    }
            }
            .addOnFailureListener {
                Log.e("AccountRepository", "Gagal mencari pengurus utama.", it)
            }
    }
    // ---------------------------------------------------------

    suspend fun insert(account: Account) {
        val userId = auth.currentUser?.uid ?: throw Exception("User tidak login")
        val newAccount = account.copy(userId = userId)
        val docId = if (account.id.isNotBlank()) account.id else UUID.randomUUID().toString()
        firestore.collection("accounts").document(docId).set(newAccount.copy(id = docId)).await()
    }

    suspend fun update(account: Account) {
        if (account.id.isNotBlank()) {
            firestore.collection("accounts").document(account.id).set(account).await()
        }
    }

    suspend fun delete(account: Account) {
        if (account.id.isNotBlank()) {
            firestore.collection("accounts").document(account.id).delete().await()
        }
    }
}