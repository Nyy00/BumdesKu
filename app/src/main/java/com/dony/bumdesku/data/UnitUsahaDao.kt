package com.dony.bumdesku.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitUsahaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unitUsaha: UnitUsaha)

    @Update
    suspend fun update(unitUsaha: UnitUsaha)

    @Delete
    suspend fun delete(unitUsaha: UnitUsaha)

    @Query("SELECT * FROM unit_usaha ORDER BY name ASC")
    fun getAllUnitUsaha(): Flow<List<UnitUsaha>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(unitUsaha: List<UnitUsaha>) // Tambahkan ini

    @Query("DELETE FROM unit_usaha") // Tambahkan ini
    suspend fun deleteAll()
}