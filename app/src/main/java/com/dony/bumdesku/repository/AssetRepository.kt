package com.dony.bumdesku.repository

import android.net.Uri
import android.util.Log
import com.dony.bumdesku.data.Asset
import com.dony.bumdesku.data.AssetDao
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AssetRepository(private val assetDao: AssetDao) {

    private val firestore = Firebase.firestore
    private val storage = Firebase.storage
    private val auth = Firebase.auth
    private val scope = CoroutineScope(Dispatchers.IO)

    val allAssets: Flow<List<Asset>> = assetDao.getAllAssets()

    fun syncAssets(targetUserId: String) {
        firestore.collection("assets").whereEqualTo("userId", targetUserId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("AssetRepository", "Listen failed.", e)
                    return@addSnapshotListener
                }

                scope.launch {
                    val firestoreAssets = snapshots?.mapNotNull { doc ->
                        doc.toObject<Asset>().apply { id = doc.id }
                    } ?: emptyList()
                    assetDao.deleteAll()
                    assetDao.insertAll(firestoreAssets)
                }
            }
    }

    // ... sisa kode (insert, update, delete, dll.) tetap sama
    fun getAssetById(id: Int): Flow<Asset?> {
        return assetDao.getAssetById(id)
    }

    suspend fun insert(asset: Asset, imageUri: Uri?) {
        val userId = auth.currentUser?.uid ?: throw Exception("User tidak login")
        var imageUrl = ""

        if (imageUri != null) {
            try {
                imageUrl = uploadAssetImage(imageUri)
            } catch (e: Exception) {
                Log.e("AssetRepository", "Image upload failed", e)
                throw e
            }
        }

        val newAsset = asset.copy(
            userId = userId,
            imageUrl = imageUrl
        )

        val docRef = firestore.collection("assets").add(newAsset).await()
        update(newAsset.copy(id = docRef.id))
    }

    suspend fun update(asset: Asset) {
        if (asset.id.isBlank()) return
        firestore.collection("assets").document(asset.id).set(asset).await()
    }

    suspend fun delete(asset: Asset) {
        if (asset.id.isNotBlank()) {
            firestore.collection("assets").document(asset.id).delete().await()
        }
    }

    private suspend fun uploadAssetImage(imageUri: Uri): String {
        val userId = auth.currentUser?.uid ?: throw Exception("User tidak login")
        val fileName = "asset_${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child("assets/$userId/$fileName")
        storageRef.putFile(imageUri).await()
        return storageRef.downloadUrl.await().toString()
    }
}
