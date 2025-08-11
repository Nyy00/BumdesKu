package com.dony.bumdesku.repository

import com.dony.bumdesku.data.RentalDao
import com.dony.bumdesku.data.RentalItem
import com.dony.bumdesku.data.RentalTransaction
import java.util.UUID

// Hanya butuh RentalDao, sama seperti repository Anda yang lain
class RentalRepository(private val rentalDao: RentalDao) {

    // --- Logika untuk Barang Sewaan (RentalItem) ---
    fun getRentalItems(unitUsahaId: String) = rentalDao.getAllRentalItems(unitUsahaId)

    suspend fun saveRentalItem(item: RentalItem) {
        val itemId = if (item.id.isEmpty()) UUID.randomUUID().toString() else item.id
        val newItem = item.copy(id = itemId)
        rentalDao.insertRentalItem(newItem)
        // Tidak ada interaksi Firestore di sini
    }

    // --- Logika untuk Transaksi Sewa (RentalTransaction) ---
    fun getRentalTransactions(unitUsahaId: String) = rentalDao.getAllRentalTransactions(unitUsahaId)

    suspend fun createRentalTransaction(transaction: RentalTransaction) {
        val item = rentalDao.getRentalItemById(transaction.rentalItemId)
        if (item != null && item.availableStock >= transaction.quantity) {
            val updatedItem = item.copy(availableStock = item.availableStock - transaction.quantity)
            rentalDao.updateRentalItem(updatedItem)

            val transactionId = UUID.randomUUID().toString()
            val newTransaction = transaction.copy(id = transactionId)
            rentalDao.insertRentalTransaction(newTransaction)
        } else {
            throw Exception("Stok tidak mencukupi.")
        }
    }

    suspend fun completeRentalTransaction(transaction: RentalTransaction) {
        val item = rentalDao.getRentalItemById(transaction.rentalItemId)
        if (item != null) {
            val updatedItem = item.copy(availableStock = item.availableStock + transaction.quantity)
            rentalDao.updateRentalItem(updatedItem)

            val updatedTransaction = transaction.copy(
                status = "Selesai",
                returnDate = System.currentTimeMillis()
            )
            rentalDao.updateRentalTransaction(updatedTransaction)
        }
    }
}