package com.dony.bumdesku.repository

import com.dony.bumdesku.data.Asset
import com.dony.bumdesku.data.AssetDao
import kotlinx.coroutines.flow.Flow

class AssetRepository(private val assetDao: AssetDao) {

    val allAssets: Flow<List<Asset>> = assetDao.getAllAssets()

    fun getAssetById(id: Int): Flow<Asset?> {
        return assetDao.getAssetById(id)
    }

    suspend fun insert(asset: Asset) {
        assetDao.insert(asset)
    }

    suspend fun update(asset: Asset) {
        assetDao.update(asset)
    }

    suspend fun delete(asset: Asset) {
        assetDao.delete(asset)
    }
}