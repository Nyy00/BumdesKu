package com.dony.bumdesku.repository

import android.util.Log
import com.dony.bumdesku.data.*
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.ceil

class RentalRepository(
    private val rentalDao: RentalDao,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    // ✅ 1. Inisialisasi Firestore secara internal, sama seperti PosRepository
    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(Dispatchers.IO)

    // --- SINKRONISASI DATA DARI FIREBASE KE LOKAL ---

    fun syncDataForUnit(unitId: String): List<ListenerRegistration> {
        val itemListener = firestore.collection("rental_items").whereEqualTo("unitUsahaId", unitId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("RentalRepository", "Listen for rental items failed.", e)
                    return@addSnapshotListener
                }
                scope.launch {
                    val items = snapshots?.documents?.mapNotNull { doc ->
                        doc.toObject(RentalItem::class.java)?.apply { id = doc.id }
                    } ?: emptyList()
                    rentalDao.insertAllRentalItems(items)
                }
            }

        val transactionListener = firestore.collection("rental_transactions").whereEqualTo("unitUsahaId", unitId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("RentalRepository", "Listen for rental transactions failed.", e)
                    return@addSnapshotListener
                }
                scope.launch {
                    val transactions = snapshots?.documents?.mapNotNull { doc ->
                        doc.toObject(RentalTransaction::class.java)?.apply { id = doc.id }
                    } ?: emptyList()
                    rentalDao.insertAllRentalTransactions(transactions)
                }
            }
        return listOf(itemListener, transactionListener)
    }

    // --- AKSES DATA LOKAL UNTUK DITAMPILKAN DI UI ---

    fun getRentalItems(unitId: String): Flow<List<RentalItem>> {
        return rentalDao.getAllRentalItems(unitId)
    }

    fun getRentalTransactions(unitId: String): Flow<List<RentalTransaction>> {
        return rentalDao.getAllRentalTransactions(unitId)
    }

    // --- LOGIKA BISNIS (Menyimpan data ke Firestore) ---

    suspend fun saveItem(item: RentalItem) {
        withContext(Dispatchers.IO) {
            val itemId = if (item.id.isBlank()) UUID.randomUUID().toString() else item.id
            val finalItem = item.copy(id = itemId)
            firestore.collection("rental_items").document(itemId).set(finalItem).await()
        }
    }

    suspend fun processNewRental(transaction: RentalTransaction) {
        withContext(Dispatchers.IO) {
            val itemToRent = rentalDao.getRentalItemById(transaction.rentalItemId)?.let { it } ?: throw IllegalStateException("Item tidak ditemukan di database lokal.")

            if (itemToRent.availableStock < transaction.quantity) {
                throw IllegalStateException("Stok untuk ${transaction.itemName} tidak mencukupi.")
            }

            val transactionId = UUID.randomUUID().toString()
            val finalTransaction = transaction.copy(id = transactionId)
            firestore.collection("rental_transactions").document(transactionId).set(finalTransaction).await()

            val updatedItem = itemToRent.copy(availableStock = itemToRent.availableStock - transaction.quantity)
            firestore.collection("rental_items").document(updatedItem.id).set(updatedItem).await()
        }
    }

    suspend fun processReturn(transaction: RentalTransaction) {
        withContext(Dispatchers.IO) {
            val returnTimestamp = System.currentTimeMillis()

            val itemToReturn = rentalDao.getRentalItemById(transaction.rentalItemId)
                ?: throw IllegalStateException("Item sewa tidak ditemukan di database lokal.")

            // --- LOGIKA PERHITUNGAN BARU ---
            // 1. Hitung durasi sewa aktual (tetap sama)
            val durationInMillis = returnTimestamp - transaction.rentalDate
            val durationInDays = ceil(durationInMillis / (1000.0 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
            val rentalCost = transaction.pricePerDay * transaction.quantity * durationInDays

            // 2. Hitung keterlambatan dengan membandingkan tanggalnya saja
            var lateFee = 0.0

            // Atur waktu ke awal hari untuk perbandingan yang adil
            val calReturn = Calendar.getInstance().apply { timeInMillis = returnTimestamp }
            val calExpected = Calendar.getInstance().apply { timeInMillis = transaction.expectedReturnDate }

            val isSameDayOrBefore = calReturn.get(Calendar.YEAR) < calExpected.get(Calendar.YEAR) ||
                    (calReturn.get(Calendar.YEAR) == calExpected.get(Calendar.YEAR) &&
                            calReturn.get(Calendar.DAY_OF_YEAR) <= calExpected.get(Calendar.DAY_OF_YEAR))

            if (!isSameDayOrBefore) { // Jika tidak sama hari atau sebelumnya (artinya terlambat)
                // Hitung selisih hari keterlambatan
                val lateDurationInMillis = returnTimestamp - transaction.expectedReturnDate
                val lateInDays = ceil(lateDurationInMillis / (1000.0 * 60 * 60 * 24)).toInt()
                if (lateInDays > 0) {
                    lateFee = itemToReturn.lateFeePerDay * lateInDays
                }
            }

            val finalPrice = rentalCost + lateFee
            // ------------------------------------

            val allAccounts = accountRepository.allAccounts.first()
            val kasAccount = allAccounts.find { it.accountNumber == "111" }
            val pendapatanSewaAccount = allAccounts.find { it.accountNumber == "413" }

            if (kasAccount == null || pendapatanSewaAccount == null) {
                throw IllegalStateException("Akun Kas Tunai (111) atau Pendapatan Jasa Sewa (413) tidak ditemukan.")
            }

            val description = "Pendapatan sewa ${transaction.itemName} oleh ${transaction.customerName}" +
                    if (lateFee > 0) " (termasuk denda keterlambatan)" else ""

            val financialTransaction = Transaction(
                description = description,
                amount = finalPrice,
                date = returnTimestamp,
                debitAccountId = kasAccount.id,
                creditAccountId = pendapatanSewaAccount.id,
                debitAccountName = kasAccount.accountName,
                creditAccountName = pendapatanSewaAccount.accountName,
                unitUsahaId = transaction.unitUsahaId,
                userId = ""
            )
            transactionRepository.insert(financialTransaction)

            val updatedTransaction = transaction.copy(
                status = "Selesai",
                returnDate = returnTimestamp,
                totalPrice = finalPrice
            )
            firestore.collection("rental_transactions").document(transaction.id).set(updatedTransaction).await()

            val updatedItemStock = itemToReturn.copy(availableStock = itemToReturn.availableStock + transaction.quantity)
            firestore.collection("rental_items").document(updatedItemStock.id).set(updatedItemStock).await()
        }
    }


    suspend fun deleteItem(item: RentalItem) {
        withContext(Dispatchers.IO) {
            if (item.id.isNotBlank()) {
                firestore.collection("rental_items").document(item.id).delete().await()
                // Data di Room akan otomatis terhapus oleh listener sinkronisasi
            }
        }
    }
}