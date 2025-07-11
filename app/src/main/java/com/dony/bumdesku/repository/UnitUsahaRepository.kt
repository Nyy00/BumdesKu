package com.dony.bumdesku.repository

import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.data.UnitUsahaDao
import kotlinx.coroutines.flow.Flow

class UnitUsahaRepository(private val unitUsahaDao: UnitUsahaDao) {

    val allUnitUsaha: Flow<List<UnitUsaha>> = unitUsahaDao.getAllUnitUsaha()

    suspend fun insert(unitUsaha: UnitUsaha) {
        unitUsahaDao.insert(unitUsaha)
    }

    suspend fun update(unitUsaha: UnitUsaha) {
        unitUsahaDao.update(unitUsaha)
    }

    suspend fun delete(unitUsaha: UnitUsaha) {
        unitUsahaDao.delete(unitUsaha)
    }
}