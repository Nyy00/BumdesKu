package com.dony.bumdesku.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDao {
    // --- Operasi untuk ProductionCycle ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: ProductionCycle)

    @Update
    suspend fun updateCycle(cycle: ProductionCycle)

    @Query("SELECT * FROM production_cycles WHERE unitUsahaId = :unitUsahaId ORDER BY startDate DESC")
    fun getCyclesForUnit(unitUsahaId: String): Flow<List<ProductionCycle>>

    @Query("SELECT * FROM production_cycles WHERE id = :cycleId")
    fun getCycleById(cycleId: String): Flow<ProductionCycle?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCycles(cycles: List<ProductionCycle>)

    @Query("DELETE FROM production_cycles")
    suspend fun deleteAllCycles()


    // --- Operasi untuk CycleCost ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCost(cost: CycleCost)

    @Delete
    suspend fun deleteCost(cost: CycleCost)

    @Query("SELECT * FROM cycle_costs WHERE cycleId = :cycleId ORDER BY date DESC")
    fun getCostsForCycle(cycleId: String): Flow<List<CycleCost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCosts(costs: List<CycleCost>)

    @Query("DELETE FROM cycle_costs")
    suspend fun deleteAllCosts()
}