package com.dony.bumdesku.repository

import android.util.Log
import com.dony.bumdesku.data.*
import com.dony.bumdesku.features.agribisnis.AgriCartItem
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException

class AgriRepository(
    private val agriDao: AgriDao,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val scope = CoroutineScope(Dispatchers.IO)

    // --- Sumber Data untuk UI ---
    val allHarvests: Flow<List<Harvest>> = agriDao.getAllHarvests()
    val allProduceSales: Flow<List<ProduceSale>> = agriDao.getAllProduceSales()
    val allAgriInventory: Flow<List<AgriInventory>> = agriDao.getAllAgriInventory()

    // --- Logika Sinkronisasi ---

    fun syncAllHarvestsForManager(): ListenerRegistration {
        return firestore.collection("harvests")
            .addSnapshotListener { snapshots, e ->
                handleFirestoreUpdate(
                    e,
                    snapshots,
                    agriDao::deleteAllHarvests,
                    agriDao::insertAllHarvests,
                    Harvest::class.java
                )
            }
    }

    fun syncHarvestsForUser(managedUnitIds: List<String>): ListenerRegistration {
        return firestore.collection("harvests").whereIn("unitUsahaId", managedUnitIds.take(30))
            .addSnapshotListener { snapshots, e ->
                handleFirestoreUpdate(
                    e,
                    snapshots,
                    agriDao::deleteAllHarvests,
                    agriDao::insertAllHarvests,
                    Harvest::class.java
                )
            }
    }

    // Lakukan hal yang sama untuk ProduceSale
    fun syncAllProduceSalesForManager(): ListenerRegistration {
        return firestore.collection("produce_sales")
            .addSnapshotListener { snapshots, e ->
                handleFirestoreUpdate(
                    e,
                    snapshots,
                    agriDao::deleteAllProduceSales,
                    agriDao::insertAllProduceSales,
                    ProduceSale::class.java
                )
            }
    }

    fun syncProduceSalesForUser(managedUnitIds: List<String>): ListenerRegistration {
        return firestore.collection("produce_sales").whereIn("unitUsahaId", managedUnitIds.take(30))
            .addSnapshotListener { snapshots, e ->
                handleFirestoreUpdate(
                    e,
                    snapshots,
                    agriDao::deleteAllProduceSales,
                    agriDao::insertAllProduceSales,
                    ProduceSale::class.java
                )
            }
    }

    fun syncAllAgriInventoryForManager(): ListenerRegistration {
        return firestore.collection("agri_inventory")
            .addSnapshotListener { snapshots, e ->
                handleFirestoreUpdate(
                    e,
                    snapshots,
                    agriDao::deleteAllAgriInventory,
                    agriDao::insertAllAgriInventory,
                    AgriInventory::class.java
                )
            }
    }

    fun syncAgriInventoryForUser(managedUnitIds: List<String>): ListenerRegistration {
        return firestore.collection("agri_inventory")
            .whereIn("unitUsahaId", managedUnitIds.take(30))
            .addSnapshotListener { snapshots, e ->
                handleFirestoreUpdate(
                    e,
                    snapshots,
                    agriDao::deleteAllAgriInventory,
                    agriDao::insertAllAgriInventory,
                    AgriInventory::class.java
                )
            }
    }

    fun getInventoryById(id: String): Flow<AgriInventory?> = agriDao.getInventoryById(id)

    suspend fun update(inventory: AgriInventory) {
        if (inventory.id.isBlank()) return
        firestore.collection("agri_inventory").document(inventory.id).set(inventory).await()
    }


    // --- Operasi Tulis (Create, Update, Delete) ---
    suspend fun insert(harvest: Harvest) {
        val userId = auth.currentUser?.uid ?: throw Exception("User tidak login")
        val docRef = firestore.collection("harvests").document()
        val newHarvest = harvest.copy(id = docRef.id, userId = userId)
        docRef.set(newHarvest).await()
    }

    suspend fun insert(inventory: AgriInventory) {
        val userId = auth.currentUser?.uid ?: throw Exception("User tidak login")
        val docRef = firestore.collection("agri_inventory").document()
        val newInventory = inventory.copy(id = docRef.id, userId = userId)
        docRef.set(newInventory).await()
    }

    suspend fun processProduceSale(
        cartItems: List<AgriCartItem>,
        totalPrice: Double,
        user: UserProfile,
        activeUnitUsaha: UnitUsaha
    ) {
        withContext(Dispatchers.IO) {
            // 1. Validasi Stok (Sama seperti di Toko)
            for (cartItem in cartItems) {
                val harvestInDb = allHarvests.first().find { it.id == cartItem.harvest.id }
                if (harvestInDb == null || harvestInDb.quantity < cartItem.quantity) {
                    throw IOException("Stok untuk ${cartItem.harvest.name} tidak mencukupi.")
                }
            }

            // 2. Ambil Akun yang Diperlukan untuk Jurnal
            val allAccounts = accountRepository.allAccounts.first()
            val kasAccount = allAccounts.find { it.accountNumber == "111" }
            // Asumsi kita menggunakan akun pendapatan yang sama dengan toko
            val pendapatanAccount = allAccounts.find { it.accountNumber == "412" }

            if (kasAccount == null || pendapatanAccount == null) {
                throw IllegalStateException("Akun Kas (111) atau Pendapatan Penjualan (412) tidak ditemukan.")
            }

            // 3. Buat dan Simpan Data Penjualan (ProduceSale)
            val saleDocRef = firestore.collection("produce_sales").document()
            val produceSale = ProduceSale(
                id = saleDocRef.id,
                itemsJson = Gson().toJson(cartItems),
                totalPrice = totalPrice,
                transactionDate = System.currentTimeMillis(),
                userId = user.uid,
                unitUsahaId = activeUnitUsaha.id
            )
            saleDocRef.set(produceSale).await()

            // 4. Buat dan Simpan Jurnal Transaksi Otomatis
            val transaction = Transaction(
                description = "Penjualan Hasil Panen - ${activeUnitUsaha.name}",
                amount = totalPrice,
                date = produceSale.transactionDate,
                debitAccountId = kasAccount.id,
                creditAccountId = pendapatanAccount.id,
                debitAccountName = kasAccount.accountName,
                creditAccountName = pendapatanAccount.accountName,
                unitUsahaId = activeUnitUsaha.id,
                userId = user.uid
            )
            transactionRepository.insert(transaction)

            // 5. Perbarui (Kurangi) Stok Hasil Panen
            cartItems.forEach { cartItem ->
                val updatedHarvest = cartItem.harvest.copy(
                    quantity = cartItem.harvest.quantity - cartItem.quantity
                )
                // Langsung update ke Firestore
                firestore.collection("harvests").document(updatedHarvest.id).set(updatedHarvest)
                    .await()
            }
        }
    }

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
                    is AgriInventory -> obj.id = doc.id
                }
                obj
            } ?: emptyList()
            deleteAll()
            insertAll(firestoreData)
        }
    }

    suspend fun addHarvestToCycle(harvest: Harvest, cycleId: String) {
        // Langkah 1: Simpan catatan panen terperinci ke sub-koleksi siklus (logika saat ini)
        val harvestDocRef = firestore.collection("production_cycles").document(cycleId)
            .collection("harvests").document()
        val newHarvest = harvest.copy(id = harvestDocRef.id)
        harvestDocRef.set(newHarvest).await()

        // Langkah 2: Perbarui jumlah total panen di dokumen siklus induk
        firestore.runTransaction { transaction ->
            val cycleRef = firestore.collection("production_cycles").document(cycleId)
            val cycleSnapshot = transaction.get(cycleRef)
            val currentTotalHarvest = cycleSnapshot.getDouble("totalHarvest") ?: 0.0
            val newTotalHarvest = currentTotalHarvest + harvest.quantity
            transaction.update(cycleRef, "totalHarvest", newTotalHarvest)
        }.await()

        // Langkah 3: LOGIKA BARU - Tambahkan jumlah yang dipanen ke stok penjualan utama
        updateMainHarvestStock(harvest)
    }

    private suspend fun updateMainHarvestStock(harvestToAdd: Harvest) {
        val stockCollection = firestore.collection("harvests")

        val querySnapshot = stockCollection
            .whereEqualTo("name", harvestToAdd.name)
            .whereEqualTo("unit", harvestToAdd.unit)
            .whereEqualTo("unitUsahaId", harvestToAdd.unitUsahaId)
            .limit(1)
            .get()
            .await()

        if (querySnapshot.isEmpty) {
            Log.d("AgriRepository", "Stok untuk ${harvestToAdd.name} tidak ditemukan. Membuat baru.")
            insert(harvestToAdd)
        } else {
            val existingDoc = querySnapshot.documents.first()
            val existingHarvest = existingDoc.toObject(Harvest::class.java)

            if (existingHarvest != null) {
                Log.d("AgriRepository", "Stok untuk ${existingHarvest.name} ditemukan. Memperbarui kuantitas dan harga.")
                val stockRef = stockCollection.document(existingDoc.id)

                // ✅✅✅ PERBAIKAN FINAL DAN PALING AMAN ADA DI SINI ✅✅✅
                // Menggabungkan pembaruan kuantitas dan harga menjadi SATU perintah
                val updates = mapOf(
                    "quantity" to FieldValue.increment(harvestToAdd.quantity),
                    "sellingPrice" to harvestToAdd.sellingPrice
                )

                stockRef.update(updates).await()
            }
        }
    }
}