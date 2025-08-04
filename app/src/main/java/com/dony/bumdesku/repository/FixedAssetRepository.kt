package com.dony.bumdesku.repository

import android.util.Log
import com.dony.bumdesku.data.FixedAsset
import com.dony.bumdesku.data.FixedAssetDao
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FixedAssetRepository(private val fixedAssetDao: FixedAssetDao) {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val scope = CoroutineScope(Dispatchers.IO)

    val allAssets: Flow<List<FixedAsset>> = fixedAssetDao.getAllAssets()

    fun syncAssetsForUser(managedUnitIds: List<String>): ListenerRegistration {
        return firestore.collection("fixed_assets").whereIn("unitUsahaId", managedUnitIds.take(30))
            .addSnapshotListener { snapshots, e ->
                handleFirestoreUpdate(e, snapshots, "User")
            }
    }

    fun syncAllAssetsForManager(): ListenerRegistration {
        return firestore.collection("fixed_assets")
            .addSnapshotListener { snapshots, e ->
                handleFirestoreUpdate(e, snapshots, "Manager")
            }
    }

    private fun handleFirestoreUpdate(
        e: Exception?,
        snapshots: com.google.firebase.firestore.QuerySnapshot?,
        tag: String
    ) {
        if (e != null) {
            Log.w("FixedAssetRepository", "Listen for assets failed for $tag.", e)
            return
        }
        scope.launch {
            val assets = snapshots?.documents?.mapNotNull { doc ->
                doc.toObject(FixedAsset::class.java)?.apply { id = doc.id }
            } ?: emptyList()
            fixedAssetDao.deleteAll()
            fixedAssetDao.insertAll(assets)
        }
    }

    suspend fun insert(asset: FixedAsset) {
        val userId = auth.currentUser?.uid ?: throw Exception("User tidak login")
        val docRef = firestore.collection("fixed_assets").document()
        val newAsset = asset.copy(id = docRef.id, userId = userId, bookValue = asset.purchasePrice)
        docRef.set(newAsset).await()
    }

    suspend fun update(asset: FixedAsset) {
        if (asset.id.isBlank()) return
        firestore.collection("fixed_assets").document(asset.id).set(asset).await()
    }

    suspend fun delete(asset: FixedAsset) {
        if (asset.id.isNotBlank()) {
            firestore.collection("fixed_assets").document(asset.id).delete().await()
        }
    }

    fun getAssetById(id: String): Flow<FixedAsset?> = fixedAssetDao.getAssetById(id)
}