package com.dony.bumdesku.repository

import android.util.Log
import com.dony.bumdesku.data.AgriDao
import com.dony.bumdesku.data.Harvest
import com.dony.bumdesku.data.ProduceSale
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AgriRepository(
    private val agriDao: AgriDao,
    // Kita akan membutuhkan ini nanti untuk membuat jurnal otomatis
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val scope = CoroutineScope(Dispatchers.IO)

    // --- Sumber Data untuk UI ---
    val allHarvests: Flow<List<Harvest>> = agriDao.getAllHarvests()
    val allProduceSales: Flow<List<ProduceSale>> = agriDao.getAllProduceSales()

    // --- Logika Sinkronisasi ---

    fun syncAllHarvestsForManager(): ListenerRegistration {
        return firestore.collection("harvests")
            .addSnapshotListener { snapshots, e ->
                handleFirestoreUpdate(e, snapshots, agriDao::deleteAllHarvests, agriDao::insertAllHarvests, Harvest::class.java)
            }
    }

    fun syncHarvestsForUser(managedUnitIds: List<String>): ListenerRegistration {
        return firestore.collection("harvests").whereIn("unitUsahaId", managedUnitIds.take(30))
            .addSnapshotListener { snapshots, e ->
                handleFirestoreUpdate(e, snapshots, agriDao::deleteAllHarvests, agriDao::insertAllHarvests, Harvest::class.java)
            }
    }

    // Lakukan hal yang sama untuk ProduceSale
    fun syncAllProduceSalesForManager(): ListenerRegistration {
        return firestore.collection("produce_sales")
            .addSnapshotListener { snapshots, e ->
                handleFirestoreUpdate(e, snapshots, agriDao::deleteAllProduceSales, agriDao::insertAllProduceSales, ProduceSale::class.java)
            }
    }

    fun syncProduceSalesForUser(managedUnitIds: List<String>): ListenerRegistration {
        return firestore.collection("produce_sales").whereIn("unitUsahaId", managedUnitIds.take(30))
            .addSnapshotListener { snapshots, e ->
                handleFirestoreUpdate(e, snapshots, agriDao::deleteAllProduceSales, agriDao::insertAllProduceSales, ProduceSale::class.java)
            }
    }

    // --- Operasi Tulis (Create, Update, Delete) ---
    suspend fun insert(harvest: Harvest) {
        val userId = auth.currentUser?.uid ?: throw Exception("User tidak login")
        val docRef = firestore.collection("harvests").document()
        val newHarvest = harvest.copy(id = docRef.id, userId = userId)
        docRef.set(newHarvest).await()
    }

    // ... (Fungsi untuk update & delete harvest bisa ditambahkan nanti jika perlu)
    // ... (Fungsi untuk insert ProduceSale akan kita buat saat membangun layar kasir)


    // --- Fungsi Bantuan Generik ---
    private inline fun <T : Any> handleFirestoreUpdate(
        e: Exception?,
        snapshots: com.google.firebase.firestore.QuerySnapshot?,
        crossinline deleteAll: suspend () -> Unit,
        crossinline insertAll: suspend (List<T>) -> Unit,
        clazz: Class<T>
    ) {
        if (e != null) {
            Log.w("AgriRepository", "Listen failed.", e)
            return
        }
        scope.launch {
            val firestoreData = snapshots?.mapNotNull { doc ->
                val obj = doc.toObject(clazz)
                // Set ID secara manual
                when (obj) {
                    is Harvest -> obj.id = doc.id
                    is ProduceSale -> obj.id = doc.id
                }
                obj
            } ?: emptyList()
            deleteAll()
            insertAll(firestoreData)
        }
    }
}