package com.dony.bumdesku.repository

import android.util.Log
import com.dony.bumdesku.repository.AccountRepository
import com.dony.bumdesku.data.Transaction
import com.dony.bumdesku.data.TransactionDao
import com.dony.bumdesku.data.UnitUsahaDao
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val unitUsahaDao: UnitUsahaDao,
    private val accountRepository: AccountRepository
) {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val scope = CoroutineScope(Dispatchers.IO)

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun syncTransactions(targetUserId: String) {
        firestore.collection("transactions").whereEqualTo("userId", targetUserId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("TransactionRepository", "Listen failed.", e)
                    return@addSnapshotListener
                }

                scope.launch {
                    val firestoreTransactions = snapshots?.mapNotNull { doc ->
                        doc.toObject<Transaction>().apply { id = doc.id }
                    } ?: emptyList()
                    transactionDao.deleteAll()
                    transactionDao.insertAll(firestoreTransactions)
                }
            }
    }

    // ... sisa kode (insert, update, delete, dll.) tetap sama
    suspend fun insert(transaction: Transaction) {
        val userId = auth.currentUser?.uid ?: throw Exception("User tidak login")
        val newTransaction = transaction.copy(
            userId = userId
        )
        val docRef = firestore.collection("transactions").add(newTransaction).await()
        update(newTransaction.copy(id = docRef.id))
    }

    suspend fun update(transaction: Transaction) {
        if (transaction.id.isBlank()) return
        firestore.collection("transactions").document(transaction.id).set(transaction).await()
    }

    suspend fun delete(transaction: Transaction) {
        if (transaction.id.isNotBlank()) {
            firestore.collection("transactions").document(transaction.id).delete().await()
        }
    }

    fun getTransactionById(id: Int): Flow<Transaction?> {
        return transactionDao.getTransactionById(id)
    }

    suspend fun lockTransactionsUpTo(date: Long) {
        transactionDao.lockTransactionsUpTo(date)
    }

    suspend fun getReportData(
        startDate: Long,
        endDate: Long,
        unitUsahaId: String?,
        pendapatanAccountIds: List<String>,
        bebanAccountIds: List<String>
    ): Pair<Double, Double> {
        val income: Double
        val expenses: Double

        if (unitUsahaId == null) {
            income = transactionDao.getCreditTotalByDateRange(pendapatanAccountIds, startDate, endDate) ?: 0.0
            expenses = transactionDao.getDebitTotalByDateRange(bebanAccountIds, startDate, endDate) ?: 0.0
        } else {
            income = transactionDao.getCreditTotalByDateRangeAndUnit(pendapatanAccountIds, unitUsahaId, startDate, endDate) ?: 0.0
            expenses = transactionDao.getDebitTotalByDateRangeAndUnit(bebanAccountIds, unitUsahaId, startDate, endDate) ?: 0.0
        }
        return Pair(income, expenses)
    }

    fun getFilteredTransactions(startDate: Long, endDate: Long, unitUsahaId: String?): Flow<List<Transaction>> {
        return if (unitUsahaId == null) {
            transactionDao.getTransactionsByDateRange(startDate, endDate)
        } else {
            transactionDao.getTransactionsByDateAndUnit(unitUsahaId, startDate, endDate)
        }
    }
}
