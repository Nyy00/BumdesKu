package com.dony.bumdesku.repository

import android.util.Log
import androidx.room.util.copy
import com.dony.bumdesku.data.*
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import java.util.Calendar
import kotlin.math.ceil

class RentalRepository(
    private val rentalDao: RentalDao,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val debtRepository: DebtRepository
) {
    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(Dispatchers.IO)

    val allRentalItems: Flow<List<RentalItem>> = rentalDao.getAllItems()

    fun syncDataForUnit(unitId: String): List<ListenerRegistration> {
        val itemListener = firestore.collection("rental_items").whereEqualTo("unitUsahaId", unitId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("RentalRepository", "Listen for rental items failed.", e)
                    return@addSnapshotListener
                }
                Log.d("RentalRepository", "Snapshot listener for rental_items received ${snapshots?.size() ?: 0} documents.")
                scope.launch {
                    val items = snapshots?.documents?.mapNotNull { doc ->
                        doc.toObject(RentalItem::class.java)?.apply { id = doc.id }
                    } ?: emptyList()
                    rentalDao.syncItems(unitId, items)
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
                    rentalDao.syncTransactions(unitId, transactions)
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

    fun getRentalTransactionById(id: String): Flow<RentalTransaction?> {
        return rentalDao.getRentalTransactionById(id)
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

            val availableStockOnDate = checkAvailability(
                transaction.rentalItemId,
                transaction.unitUsahaId,
                transaction.rentalDate,
                transaction.expectedReturnDate
            )
            if (transaction.quantity > availableStockOnDate) {
                throw IllegalStateException("Stok tidak mencukupi untuk tanggal yang dipilih. Sisa: $availableStockOnDate")
            }

            val newStockBaik = itemToRent.stockBaik - transaction.quantity
            val updatedItem = itemToRent.copy(stockBaik = newStockBaik)

            rentalDao.updateRentalItem(updatedItem)
            firestore.collection("rental_items").document(updatedItem.id).set(updatedItem).await()

            val allAccounts = accountRepository.allAccounts.first()
            val kasAccount = allAccounts.find { it.accountNumber == "111" }
            val piutangAccount = allAccounts.find { it.accountNumber == "113" }
            val pendapatanSewaAccount = allAccounts.find { it.accountNumber == "413" }

            if (kasAccount == null || piutangAccount == null || pendapatanSewaAccount == null) {
                throw IllegalStateException("Akun Kas (111), Piutang Usaha (113), atau Pendapatan Jasa Sewa (413) tidak ditemukan.")
            }

            // Sekarang memeriksa `transaction.paymentStatus`, bukan `transaction.status`
            if (transaction.paymentStatus == PaymentStatus.LUNAS) {
                // Jika status 'Lunas', langsung catat semua sebagai pendapatan ke kas
                val lunasTransaction = Transaction(
                    description = "Penerimaan kas sewa ${transaction.itemName} (Lunas)",
                    amount = transaction.totalPrice,
                    date = transaction.rentalDate,
                    debitAccountId = kasAccount.id,
                    creditAccountId = pendapatanSewaAccount.id,
                    debitAccountName = kasAccount.accountName,
                    creditAccountName = pendapatanSewaAccount.accountName,
                    unitUsahaId = transaction.unitUsahaId,
                    userId = transaction.userId
                )
                transactionRepository.insert(lunasTransaction)
            } else {
                // Jika status 'Belum Lunas' atau 'DP', gunakan logika piutang
                // Jurnal 1: Mencatat seluruh total biaya sebagai piutang
                val piutangTransaction = Transaction(
                    description = "Piutang sewa ${transaction.itemName} oleh ${transaction.customerName}",
                    amount = transaction.totalPrice,
                    date = transaction.rentalDate,
                    debitAccountId = piutangAccount.id,
                    creditAccountId = pendapatanSewaAccount.id,
                    debitAccountName = piutangAccount.accountName,
                    creditAccountName = pendapatanSewaAccount.accountName,
                    unitUsahaId = transaction.unitUsahaId,
                    userId = transaction.userId
                )
                transactionRepository.insert(piutangTransaction)

                // Jurnal 2: Jika ada DP, catat sebagai pembayaran piutang
                if (transaction.downPayment > 0) {
                    val dpTransaction = Transaction(
                        description = "Penerimaan DP sewa ${transaction.itemName} oleh ${transaction.customerName}",
                        amount = transaction.downPayment,
                        date = transaction.rentalDate,
                        debitAccountId = kasAccount.id,
                        creditAccountId = piutangAccount.id,
                        debitAccountName = kasAccount.accountName,
                        creditAccountName = piutangAccount.accountName,
                        unitUsahaId = transaction.unitUsahaId,
                        userId = transaction.userId
                    )
                    transactionRepository.insert(dpTransaction)
                }

                // Catat sisa piutang di tabel Receivable
                val sisaPiutang = transaction.totalPrice - transaction.downPayment
                if (sisaPiutang > 0) {
                    val receivableEntry = Receivable(
                        id = transaction.id,
                        userId = transaction.userId,
                        unitUsahaId = transaction.unitUsahaId,
                        contactName = transaction.customerName,
                        description = "Sisa pembayaran sewa ${transaction.itemName}",
                        amount = sisaPiutang,
                        transactionDate = transaction.rentalDate,
                        dueDate = transaction.expectedReturnDate,
                        isPaid = false
                    )
                    debtRepository.insert(receivableEntry)
                }
            }

            firestore.collection("rental_transactions").document(transaction.id).set(transaction).await()
        }
    }

    suspend fun processPayment(
        transactionId: String,
        paymentAmount: Double
    ) {
        withContext(Dispatchers.IO) {
            val transaction = rentalDao.getRentalTransactionById(transactionId).first()
                ?: throw IllegalStateException("Transaksi tidak ditemukan.")

            val sisaPiutang = transaction.totalPrice - transaction.downPayment
            if (paymentAmount > sisaPiutang) {
                throw IllegalStateException("Jumlah pembayaran melebihi sisa piutang.")
            }

            val newDownPayment = transaction.downPayment + paymentAmount
            val newPaymentStatus = if (newDownPayment >= transaction.totalPrice) {
                PaymentStatus.LUNAS
            } else {
                PaymentStatus.DP
            }

            // 1. Buat Jurnal Pembayaran (Kas di Debit, Piutang di Kredit)
            val allAccounts = accountRepository.allAccounts.first()
            val kasAccount = allAccounts.find { it.accountNumber == "111" }
            val piutangAccount = allAccounts.find { it.accountNumber == "113" }

            if (kasAccount == null || piutangAccount == null) {
                throw IllegalStateException("Akun Kas (111) atau Piutang Usaha (113) tidak ditemukan.")
            }

            val paymentTransaction = Transaction(
                description = "Pembayaran sewa ${transaction.itemName} oleh ${transaction.customerName}",
                amount = paymentAmount,
                date = System.currentTimeMillis(),
                debitAccountId = kasAccount.id,
                creditAccountId = piutangAccount.id,
                debitAccountName = kasAccount.accountName,
                creditAccountName = piutangAccount.accountName,
                unitUsahaId = transaction.unitUsahaId,
                userId = transaction.userId
            )
            transactionRepository.insert(paymentTransaction)

            // 2. Perbarui Receivable di DebtRepository
            val sisaPiutangBaru = transaction.totalPrice - newDownPayment
            if (sisaPiutangBaru <= 0.0) { // Jika sudah lunas
                debtRepository.deleteReceivableById(transactionId)
            } else {
                val updatedReceivable = debtRepository.getReceivableById(transactionId).first()?.copy(
                    amount = sisaPiutangBaru
                )
                if (updatedReceivable != null) {
                    debtRepository.update(updatedReceivable)
                }
            }

            // 3. Perbarui RentalTransaction di Firestore
            val updatedRentalTransaction = transaction.copy(
                paymentStatus = newPaymentStatus,
                downPayment = newDownPayment
            )
            firestore.collection("rental_transactions").document(transactionId).set(updatedRentalTransaction).await()
        }
    }

    suspend fun updateRentalItemStock(itemId: String, newStockBaik: Int, newStockRusak: Int, newStockPerbaikan: Int) {
        val item = rentalDao.getRentalItemById(itemId)
        if (item != null) {
            val updatedItem = item.copy(
                stockBaik = newStockBaik,
                stockRusakRingan = newStockRusak,
                stockPerluPerbaikan = newStockPerbaikan
            )
            rentalDao.updateRentalItem(updatedItem)
        }
    }

    suspend fun updateRentalItem(item: RentalItem) {
        rentalDao.updateRentalItem(item)
    }

    fun getTransactionsForCustomer(customerId: String): Flow<List<RentalTransaction>> {
        return firestore.collection("rental_transactions")
            .whereEqualTo("customerId", customerId)
            .orderBy("rentalDate", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { document ->
                    val transaction = document.toObject(RentalTransaction::class.java)
                    transaction?.copy(id = document.id)
                }
            }
    }

    suspend fun checkAvailability(itemId: String, unitUsahaId: String, startDate: Long, endDate: Long): Int {
        return withContext(Dispatchers.IO) {
            val item = rentalDao.getRentalItemById(itemId)
            val totalStockBaik = item?.getAvailableStock() ?: 0

            val overlappingTransactions = firestore.collection("rental_transactions")
                .whereEqualTo("rentalItemId", itemId)
                .whereEqualTo("unitUsahaId", unitUsahaId)
                .whereIn("status", listOf("Disewa", "Dipesan"))
                .get()
                .await()

            var bookedQuantity = 0
            for (doc in overlappingTransactions.documents) {
                val transaction = doc.toObject(RentalTransaction::class.java)
                if (transaction != null) {
                    val rentalStarts = transaction.rentalDate
                    val rentalEnds = transaction.expectedReturnDate
                    if (startDate < rentalEnds && rentalStarts < endDate) {
                        bookedQuantity += transaction.quantity
                    }
                }
            }

            val availableStock = totalStockBaik - bookedQuantity
            if (availableStock < 0) 0 else availableStock
        }
    }

    suspend fun processReturn(
        transaction: RentalTransaction,
        returnedConditions: Map<String, Int>,
        damageCost: Double,
        notes: String,
        remainingPayment: Double
    ) {
        withContext(Dispatchers.IO) {
            val returnTimestamp = System.currentTimeMillis()
            val itemToReturn = rentalDao.getRentalItemById(transaction.rentalItemId)
                ?: throw IllegalStateException("Item sewa tidak ditemukan.")

            var lateFee = 0.0
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = returnTimestamp
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            val normalizedReturnTimestamp = calendar.timeInMillis
            calendar.timeInMillis = transaction.expectedReturnDate
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            val normalizedExpectedReturnTimestamp = calendar.timeInMillis
            if (normalizedReturnTimestamp > normalizedExpectedReturnTimestamp) {
                val lateDurationInMillis = normalizedReturnTimestamp - normalizedExpectedReturnTimestamp
                val lateInDays = (lateDurationInMillis / (1000 * 60 * 60 * 24)).toInt()
                if (lateInDays > 0) {
                    lateFee = itemToReturn.lateFeePerDay * lateInDays
                }
            }

            val totalFinalCost = transaction.totalPrice + lateFee + damageCost
            val allAccounts = accountRepository.allAccounts.first()
            val kasAccount = allAccounts.find { it.accountNumber == "111" }
            val piutangAccount = allAccounts.find { it.accountNumber == "113" }
            val pendapatanSewaAccount = allAccounts.find { it.accountNumber == "413" }
            val pendapatanLainAccount = allAccounts.find { it.accountNumber == "421" }

            if (kasAccount == null || piutangAccount == null || pendapatanSewaAccount == null || pendapatanLainAccount == null) {
                throw IllegalStateException("Akun Kas (111), Piutang (113), Pendapatan Sewa (413), atau Pendapatan Lain (421) tidak ditemukan.")
            }

            if (remainingPayment > 0) {
                val finalPaymentTransaction = Transaction(
                    description = "Pelunasan sisa sewa ${transaction.itemName} oleh ${transaction.customerName}",
                    amount = remainingPayment,
                    date = returnTimestamp,
                    debitAccountId = kasAccount.id,
                    creditAccountId = piutangAccount.id,
                    debitAccountName = kasAccount.accountName,
                    creditAccountName = piutangAccount.accountName,
                    unitUsahaId = transaction.unitUsahaId,
                    userId = transaction.userId
                )
                transactionRepository.insert(finalPaymentTransaction)
            }

            if (damageCost > 0) {
                val damageTransaction = Transaction(
                    description = "Denda kerusakan ${transaction.itemName} oleh ${transaction.customerName}",
                    amount = damageCost,
                    date = returnTimestamp,
                    debitAccountId = kasAccount.id,
                    creditAccountId = pendapatanLainAccount.id,
                    debitAccountName = kasAccount.accountName,
                    creditAccountName = pendapatanLainAccount.accountName,
                    unitUsahaId = transaction.unitUsahaId,
                    userId = transaction.userId
                )
                transactionRepository.insert(damageTransaction)
            }

            if (transaction.totalPrice > transaction.downPayment) {
                debtRepository.deleteReceivableById(transaction.id)
            }

            val updatedTransaction = transaction.copy(
                status = "Selesai",
                returnDate = returnTimestamp,
                totalPrice = totalFinalCost,
                notesOnReturn = notes,
                paymentStatus = PaymentStatus.LUNAS,
                downPayment = transaction.downPayment + remainingPayment
            )
            firestore.collection("rental_transactions").document(transaction.id).set(updatedTransaction).await()

            val updatedItem = itemToReturn.copy(
                stockBaik = itemToReturn.stockBaik + (returnedConditions["Baik"] ?: 0),
                stockRusakRingan = itemToReturn.stockRusakRingan + (returnedConditions["Rusak Ringan"] ?: 0),
                stockPerluPerbaikan = itemToReturn.stockPerluPerbaikan + (returnedConditions["Perlu Perbaikan"] ?: 0)
            )
            rentalDao.updateRentalItem(updatedItem)
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