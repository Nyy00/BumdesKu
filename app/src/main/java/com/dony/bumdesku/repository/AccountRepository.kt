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

    // Fungsi sync sekarang menerima targetUserId
    fun syncAccounts(targetUserId: String) {
        firestore.collection("accounts").whereEqualTo("userId", targetUserId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("AccountRepository", "Listen failed.", e)
                    return@addSnapshotListener
                }

                scope.launch {
                    val firestoreAccounts = snapshots?.mapNotNull { doc ->
                        doc.toObject<Account>().apply { id = doc.id }
                    } ?: emptyList()

                    // Cek apakah PENGGUNA SAAT INI adalah pemilik data
                    val isOwner = auth.currentUser?.uid == targetUserId
                    if (firestoreAccounts.isEmpty() && isOwner) {
                        // Jika di server kosong DAN pengguna adalah pemilik (pengurus), upload data default
                        val localAccounts = accountDao.getAllAccounts().first()
                        if (localAccounts.isNotEmpty()) {
                            localAccounts.forEach { acc -> insert(acc) }
                        }
                    } else {
                        // Untuk auditor, atau pengurus yang sudah punya data, sinkronkan saja
                        accountDao.deleteAll()
                        accountDao.insertAll(firestoreAccounts)
                    }
                }
            }
    }

    suspend fun insert(account: Account) {
        val userId = auth.currentUser?.uid ?: throw Exception("User tidak login")
        val newAccount = account.copy(userId = userId)
        val docId = if (account.id.isNotBlank()) account.id else UUID.randomUUID().toString()
        firestore.collection("accounts").document(docId).set(newAccount.copy(id = docId)).await()
    }

    suspend fun update(account: Account) {
        if (account.id.isBlank()) return
        firestore.collection("accounts").document(account.id).set(account).await()
    }

    suspend fun delete(account: Account) {
        if (account.id.isNotBlank()) {
            firestore.collection("accounts").document(account.id).delete().await()
        }
    }
}
