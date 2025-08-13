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
    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(Dispatchers.IO)

    // ... (fungsi syncDataForUnit, getRentalItems, getRentalTransactions tidak berubah)
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

    fun getRentalItems(unitId: String): Flow<List<RentalItem>> {
        return rentalDao.getAllRentalItems(unitId)
    }

    fun getRentalTransactions(unitId: String): Flow<List<RentalTransaction>> {
        return rentalDao.getAllRentalTransactions(unitId)
    }

    suspend fun saveItem(item: RentalItem) {
        withContext(Dispatchers.IO) {
            val itemId = if (item.id.isBlank()) UUID.randomUUID().toString() else item.id
            val finalItem = item.copy(id = itemId)
            firestore.collection("rental_items").document(itemId).set(finalItem).await()
        }
    }

    suspend fun deleteItem(item: RentalItem) {
        withContext(Dispatchers.IO) {
            if (item.id.isNotBlank()) {
                firestore.collection("rental_items").document(item.id).delete().await()
            }
        }
    }

    suspend fun processNewRental(transaction: RentalTransaction) {
        withContext(Dispatchers.IO) {
            val itemToRent = rentalDao.getRentalItemById(transaction.rentalItemId)
                ?: throw IllegalStateException("Item tidak ditemukan di database lokal.")

            if (itemToRent.getAvailableStock() < transaction.quantity) {
                throw IllegalStateException("Stok untuk ${transaction.itemName} tidak mencukupi.")
            }

            val transactionId = UUID.randomUUID().toString()
            val finalTransaction = transaction.copy(id = transactionId)
            firestore.collection("rental_transactions").document(transactionId).set(finalTransaction).await()

            val updatedItem = itemToRent.copy(stockBaik = itemToRent.stockBaik - transaction.quantity)
            firestore.collection("rental_items").document(updatedItem.id).set(updatedItem).await()
        }
    }

    suspend fun processReturn(
        transaction: RentalTransaction,
        returnedConditions: Map<String, Int>,
        notes: String
    ) {
        withContext(Dispatchers.IO) {
            val returnTimestamp = System.currentTimeMillis()
            val itemToReturn = rentalDao.getRentalItemById(transaction.rentalItemId)
                ?: throw IllegalStateException("Item sewa tidak ditemukan.")

            val durationInMillis = returnTimestamp - transaction.rentalDate
            val durationInDays = ceil(durationInMillis / (1000.0 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
            val rentalCost = transaction.pricePerDay * transaction.quantity * durationInDays
            var lateFee = 0.0
            val calReturn = Calendar.getInstance().apply { timeInMillis = returnTimestamp }
            val calExpected = Calendar.getInstance().apply { timeInMillis = transaction.expectedReturnDate }
            val isSameDayOrBefore = calReturn.get(Calendar.YEAR) < calExpected.get(Calendar.YEAR) ||
                    (calReturn.get(Calendar.YEAR) == calExpected.get(Calendar.YEAR) &&
                            calReturn.get(Calendar.DAY_OF_YEAR) <= calExpected.get(Calendar.DAY_OF_YEAR))
            if (!isSameDayOrBefore) {
                val lateDurationInMillis = returnTimestamp - transaction.expectedReturnDate
                val lateInDays = ceil(lateDurationInMillis / (1000.0 * 60 * 60 * 24)).toInt()
                if (lateInDays > 0) {
                    lateFee = itemToReturn.lateFeePerDay * lateInDays
                }
            }
            val finalPrice = rentalCost + lateFee

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
                totalPrice = finalPrice,
                notesOnReturn = notes
            )
            firestore.collection("rental_transactions").document(transaction.id).set(updatedTransaction).await()

            // LOGIKA BARU UNTUK UPDATE STOK BERDASARKAN KONDISI
            val updatedItem = itemToReturn.copy(
                stockBaik = itemToReturn.stockBaik + (returnedConditions["Baik"] ?: 0),
                stockRusakRingan = itemToReturn.stockRusakRingan + (returnedConditions["Rusak Ringan"] ?: 0),
                stockPerluPerbaikan = itemToReturn.stockPerluPerbaikan + (returnedConditions["Perlu Perbaikan"] ?: 0)
            )
            firestore.collection("rental_items").document(updatedItem.id).set(updatedItem).await()
        }
    }

    suspend fun processItemRepair(
        item: RentalItem,
        quantity: Int,
        fromCondition: String,
        repairCost: Double
    ) {
        withContext(Dispatchers.IO) {
            val stockToRepair = when (fromCondition) {
                "Rusak Ringan" -> item.stockRusakRingan
                "Perlu Perbaikan" -> item.stockPerluPerbaikan
                else -> 0
            }
            if (stockToRepair < quantity) {
                throw IllegalStateException("Jumlah yang diperbaiki melebihi stok yang rusak.")
            }

            if (repairCost > 0) {
                val allAccounts = accountRepository.allAccounts.first()
                val kasAccount = allAccounts.find { it.accountNumber == "111" }
                val bebanPerbaikanAccount = allAccounts.find { it.accountNumber == "513" }

                if (kasAccount == null || bebanPerbaikanAccount == null) {
                    throw IllegalStateException("Akun Kas (111) atau Beban Perbaikan (513) tidak ditemukan.")
                }

                val repairTransaction = Transaction(
                    description = "Biaya perbaikan ${item.name} (x$quantity)",
                    amount = repairCost,
                    date = System.currentTimeMillis(),
                    debitAccountId = bebanPerbaikanAccount.id,
                    creditAccountId = kasAccount.id,
                    debitAccountName = bebanPerbaikanAccount.accountName,
                    creditAccountName = kasAccount.accountName,
                    unitUsahaId = item.unitUsahaId,
                    userId = ""
                )
                transactionRepository.insert(repairTransaction)
            }

            val updatedItem = item.copy(
                stockBaik = item.stockBaik + quantity,
                stockRusakRingan = if (fromCondition == "Rusak Ringan") item.stockRusakRingan - quantity else item.stockRusakRingan,
                stockPerluPerbaikan = if (fromCondition == "Perlu Perbaikan") item.stockPerluPerbaikan - quantity else item.stockPerluPerbaikan
            )
            firestore.collection("rental_items").document(updatedItem.id).set(updatedItem).await()
        }
    }
}
