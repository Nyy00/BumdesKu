package com.dony.bumdesku.repository

import android.util.Log
import com.dony.bumdesku.data.Account
import com.dony.bumdesku.data.AccountDao
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
import java.util.*

class AccountRepository(private val accountDao: AccountDao) {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val scope = CoroutineScope(Dispatchers.IO)

    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()

    /**
     * ✅ FUNGSI YANG DIPERBAIKI
     * Fungsi ini sekarang mendengarkan perubahan pada SELURUH koleksi 'accounts'
     * karena Chart of Accounts bersifat global dan sama untuk semua pengguna.
     */
    fun syncAccounts(targetUserId: String): ListenerRegistration {
        return firestore.collection("accounts")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("AccountRepository", "Listen for accounts failed.", e)
                    return@addSnapshotListener
                }

                scope.launch(Dispatchers.IO) {
                    val firestoreAccounts = snapshots?.mapNotNull { doc ->
                        doc.toObject<Account>()?.apply { id = doc.id }
                    } ?: emptyList()

                    // Hapus semua akun lama dan masukkan daftar baru yang bersih
                    accountDao.deleteAll()
                    accountDao.insertAll(firestoreAccounts)
                    Log.d("AccountRepository", "Sinkronisasi COA berhasil, ${firestoreAccounts.size} akun diterima.")
                }
            }
    }

    suspend fun insert(account: Account) {
        // Hanya manajer yang bisa melakukan ini (diatur oleh security rules)
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