package com.dony.bumdesku.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asset: Asset)

    @Update
    suspend fun update(asset: Asset)

    @Delete
    suspend fun delete(asset: Asset)

    @Query("SELECT * FROM assets ORDER BY name ASC")
    fun getAllAssets(): Flow<List<Asset>>

    @Query("SELECT * FROM assets WHERE localId = :id")
    fun getAssetById(id: Int): Flow<Asset?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assets: List<Asset>)

    @Query("DELETE FROM assets")
    suspend fun deleteAll()

}