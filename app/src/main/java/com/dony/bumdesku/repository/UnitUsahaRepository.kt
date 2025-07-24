package com.dony.bumdesku.repository

import android.util.Log
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.data.UnitUsahaDao
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
import java.util.*

class UnitUsahaRepository(private val unitUsahaDao: UnitUsahaDao) {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val scope = CoroutineScope(Dispatchers.IO)

    val allUnitUsaha: Flow<List<UnitUsaha>> = unitUsahaDao.getAllUnitUsaha()

    fun syncAllUnitUsahaForManager(): ListenerRegistration {
        return firestore.collection("unit_usaha")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("UnitUsahaRepository", "Listen for all units failed.", e)
                    return@addSnapshotListener
                }

                val firestoreUnitUsaha = snapshots?.documents?.mapNotNull { doc ->
                    doc.toObject(UnitUsaha::class.java)?.apply { id = doc.id }
                } ?: emptyList()

                scope.launch {
                    unitUsahaDao.deleteAll()
                    unitUsahaDao.insertAll(firestoreUnitUsaha)
                }
            }
    }

    fun syncUnitUsahaForUser(managedUnitIds: List<String>): ListenerRegistration {
        return firestore.collection("unit_usaha").whereIn("id", managedUnitIds.take(30))
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("UnitUsahaRepository", "Listen failed.", e)
                    return@addSnapshotListener
                }
                scope.launch {
                    val firestoreUnitUsaha = snapshots?.mapNotNull { doc ->
                        doc.toObject<UnitUsaha>()?.apply { id = doc.id }
                    } ?: emptyList()
                    unitUsahaDao.deleteAll()
                    unitUsahaDao.insertAll(firestoreUnitUsaha)
                }
            }
    }

    suspend fun insert(unitUsaha: UnitUsaha) {
        val userId = auth.currentUser?.uid ?: throw Exception("User tidak login")
        val newUnitUsaha = unitUsaha.copy(
            userId = userId
        )
        val docRef = firestore.collection("unit_usaha").add(newUnitUsaha).await()
        update(newUnitUsaha.copy(id = docRef.id))
    }

    suspend fun update(unitUsaha: UnitUsaha) {
        if (unitUsaha.id.isBlank()) return
        firestore.collection("unit_usaha").document(unitUsaha.id).set(unitUsaha).await()
    }

    suspend fun delete(unitUsaha: UnitUsaha) {
        if (unitUsaha.id.isNotBlank()) {
            firestore.collection("unit_usaha").document(unitUsaha.id).delete().await()
        }
    }
}