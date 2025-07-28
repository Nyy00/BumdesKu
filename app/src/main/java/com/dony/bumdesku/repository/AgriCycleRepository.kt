package com.dony.bumdesku.repository

import android.util.Log
import com.dony.bumdesku.data.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AgriCycleRepository(
    private val cycleDao: CycleDao,
    private val transactionRepository: TransactionRepository
) {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val scope = CoroutineScope(Dispatchers.IO)

    // --- Fungsi untuk Mengambil Data (untuk UI) ---
    fun getCyclesForUnit(unitUsahaId: String): Flow<List<ProductionCycle>> = cycleDao.getCyclesForUnit(unitUsahaId)
    fun getCycleById(cycleId: String): Flow<ProductionCycle?> = cycleDao.getCycleById(cycleId)
    fun getCostsForCycle(cycleId: String): Flow<List<CycleCost>> = cycleDao.getCostsForCycle(cycleId)

    // --- Fungsi Sinkronisasi (akan dipanggil oleh AuthViewModel) ---
    // (Fungsi syncAllCyclesForManager, syncAllCostsForManager, syncCyclesForUser, syncCostsForUser akan ada di sini)
    // Untuk saat ini, kita fokus pada fungsi tulis.

    // --- Fungsi untuk Menyimpan Data (ke Firestore) ---

    suspend fun insertCycle(cycle: ProductionCycle) {
        val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
        val docRef = firestore.collection("production_cycles").document()
        val newCycle = cycle.copy(id = docRef.id, userId = userId)
        docRef.set(newCycle).await()
    }

    suspend fun insertCost(cost: CycleCost, account: Account) {
        val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")

        // 1. Buat transaksi jurnal terlebih dahulu (DIPERBARUI)
        val transaction = Transaction(
            description = "Biaya Produksi: ${cost.description}",
            amount = cost.amount,
            date = cost.date,
            debitAccountId = cost.costCategoryId,
            creditAccountId = account.id,
            debitAccountName = "Beban ${cost.description}",
            creditAccountName = account.accountName,
            unitUsahaId = cost.unitUsahaId // <-- PERBAIKAN: Gunakan unitUsahaId dari CycleCost
        )
        transactionRepository.insert(transaction)

        // 2. Simpan data biaya itu sendiri
        val costDocRef = firestore.collection("cycle_costs").document()
        val newCost = cost.copy(id = costDocRef.id, userId = userId)
        costDocRef.set(newCost).await()

        // 3. Update total biaya di siklus produksinya
        firestore.runTransaction { firestoreTransaction ->
            val cycleRef = firestore.collection("production_cycles").document(cost.cycleId)
            val cycleSnapshot = firestoreTransaction.get(cycleRef)
            val currentTotalCost = cycleSnapshot.getDouble("totalCost") ?: 0.0
            val newTotalCost = currentTotalCost + cost.amount
            firestoreTransaction.update(cycleRef, "totalCost", newTotalCost)
        }.await()
    }
    suspend fun updateCycle(cycle: ProductionCycle) {
        if (cycle.id.isBlank()) return
        firestore.collection("production_cycles").document(cycle.id).set(cycle).await()
    }

    suspend fun finishCycle(cycle: ProductionCycle, totalHarvest: Double) {
        if (cycle.id.isBlank() || cycle.status == CycleStatus.SELESAI) return
        val hpp = if (totalHarvest > 0) cycle.totalCost / totalHarvest else 0.0
        val finishedCycle = cycle.copy(
            status = CycleStatus.SELESAI,
            endDate = System.currentTimeMillis(),
            totalHarvest = totalHarvest,
            hppPerUnit = hpp
        )
        updateCycle(finishedCycle)
    }

    fun syncAllCyclesForManager(): ListenerRegistration {
        return firestore.collection("production_cycles").addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("AgriCycleRepo", "Listen failed.", e)
                return@addSnapshotListener
            }
            scope.launch {
                val cycles = snapshots?.toObjects(ProductionCycle::class.java) ?: emptyList()
                cycleDao.deleteAllCycles()
                cycleDao.insertAllCycles(cycles)
            }
        }
    }

    fun syncAllCostsForManager(): ListenerRegistration {
        return firestore.collection("cycle_costs").addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("AgriCycleRepo", "Listen failed.", e)
                return@addSnapshotListener
            }
            scope.launch {
                val costs = snapshots?.toObjects(CycleCost::class.java) ?: emptyList()
                cycleDao.deleteAllCosts()
                cycleDao.insertAllCosts(costs)
            }
        }
    }

    fun syncCyclesForUser(unitUsahaIds: List<String>): ListenerRegistration {
        return firestore.collection("production_cycles")
            .whereIn("unitUsahaId", unitUsahaIds)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("AgriCycleRepo", "Listen failed.", e)
                    return@addSnapshotListener
                }
                scope.launch {
                    val cycles = snapshots?.toObjects(ProductionCycle::class.java) ?: emptyList()
                    // Hanya hapus dan masukkan data yang relevan dengan unit usaha pengguna
                    cycleDao.deleteAllCycles() // Ini aman karena data lain sudah terfilter
                    cycleDao.insertAllCycles(cycles)
                }
            }
    }
    fun syncCostsForUser(userId: String): ListenerRegistration {
        return firestore.collection("cycle_costs")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("AgriCycleRepo", "Listen failed.", e)
                    return@addSnapshotListener
                }
                scope.launch {
                    val costs = snapshots?.toObjects(CycleCost::class.java) ?: emptyList()
                    cycleDao.deleteAllCosts()
                    cycleDao.insertAllCosts(costs)
                }
            }
    }

    /**
     * Fungsi baru untuk mengarsipkan siklus.
     * Ini hanya mengubah properti 'isArchived' menjadi true.
     */
    suspend fun archiveCycle(cycle: ProductionCycle) {
        if (cycle.id.isBlank()) return
        val updatedCycle = cycle.copy(isArchived = true)
        updateCycle(updatedCycle)
    }
}
