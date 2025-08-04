package com.dony.bumdesku.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedAssetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fixedAsset: FixedAsset)

    @Update
    suspend fun update(fixedAsset: FixedAsset)

    @Delete
    suspend fun delete(fixedAsset: FixedAsset)

    @Query("SELECT * FROM fixed_assets WHERE id = :id")
    fun getAssetById(id: String): Flow<FixedAsset?>

    @Query("SELECT * FROM fixed_assets ORDER BY purchaseDate DESC")
    fun getAllAssets(): Flow<List<FixedAsset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assets: List<FixedAsset>)

    @Query("DELETE FROM fixed_assets")
    suspend fun deleteAll()
}