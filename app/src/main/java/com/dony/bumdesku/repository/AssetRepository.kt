package com.dony.bumdesku.repository

import android.net.Uri
import android.util.Log
import com.dony.bumdesku.data.Asset
import com.dony.bumdesku.data.AssetDao
import com.dony.bumdesku.data.UserProfile
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration // PASTIKAN IMPORT INI ADA
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class AssetRepository(private val assetDao: AssetDao) {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val scope = CoroutineScope(Dispatchers.IO)

    val allAssets: Flow<List<Asset>> = assetDao.getAllAssets()

    fun syncAssetsForUnit(unitId: String): ListenerRegistration {
        return firestore.collection("assets").whereEqualTo("unitUsahaId", unitId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("AssetRepository", "Listen for unit assets failed for $unitId", e)
                    return@addSnapshotListener
                }
                scope.launch {
                    val assets = snapshots?.documents?.mapNotNull { doc ->
                        doc.toObject<Asset>()?.apply { id = doc.id }
                    } ?: emptyList()
                    assetDao.insertAll(assets)
                }
            }
    }

    suspend fun clearLocalAssets() {
        assetDao.deleteAll()
    }


    fun syncAssetsForUser(managedUnitIds: List<String>): ListenerRegistration {
        return firestore.collection("assets").whereIn("unitUsahaId", managedUnitIds.take(30))
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("AssetRepository", "Listen for user assets failed.", e)
                    return@addSnapshotListener
                }
                scope.launch(Dispatchers.IO) {
                    val assets = snapshots?.documents?.mapNotNull { doc ->
                        doc.toObject(Asset::class.java)?.apply { id = doc.id }
                    } ?: emptyList()

                    // Hapus semua data aset lama di lokal
                    assetDao.deleteAll()
                    // Masukkan daftar aset yang baru dan bersih dari server
                    assetDao.insertAll(assets)
                }
            }
    }

    fun syncAllAssetsForManager(): ListenerRegistration {
        return firestore.collection("assets")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("AssetRepository", "Listen for ALL assets failed.", e)
                    return@addSnapshotListener
                }
                scope.launch(Dispatchers.IO) {
                    val assets = snapshots?.documents?.mapNotNull { doc ->
                        doc.toObject(Asset::class.java)?.apply { id = doc.id }
                    } ?: emptyList()
                    assetDao.deleteAll()
                    assetDao.insertAll(assets)
                }
            }
    }

    fun getAssetById(id: String): Flow<Asset?> {
        return assetDao.getAssetById(id)
    }

    suspend fun insert(asset: Asset) { // Hapus parameter imageUri
        val userId = auth.currentUser?.uid ?: throw Exception("User tidak login")

        // Langsung buat objek asset baru tanpa imageUrl
        val newAsset = asset.copy(
            userId = userId,
            imageUrl = "" // Selalu kosong
        )

        // Simpan ke Firestore
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

}