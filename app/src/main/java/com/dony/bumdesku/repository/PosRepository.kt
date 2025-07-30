package com.dony.bumdesku.repository

import android.util.Log
import com.dony.bumdesku.data.*
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.tasks.await
import java.io.IOException

class PosRepository(
    private val saleDao: SaleDao,
    private val assetRepository: AssetRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(Dispatchers.IO)

    fun syncSalesForUnit(unitId: String): ListenerRegistration {
        return firestore.collection("sales").whereEqualTo("unitUsahaId", unitId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("PosRepository", "Listen for unit sales failed for $unitId", e)
                    return@addSnapshotListener
                }
                scope.launch {
                    val sales = snapshots?.documents?.mapNotNull { doc ->
                        doc.toObject(Sale::class.java)
                    } ?: emptyList()
                    saleDao.insertAll(sales)
                }
            }
    }

    suspend fun clearLocalSales() {
        saleDao.deleteAll()
    }

    fun getAllSales(): Flow<List<Sale>> {
        return saleDao.getAllSales()
    }

    fun syncSalesForUser(managedUnitIds: List<String>): ListenerRegistration {
        return firestore.collection("sales").whereIn("unitUsahaId", managedUnitIds.take(30))
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("PosRepository", "Listen for sales failed.", e)
                    return@addSnapshotListener
                }
                scope.launch(Dispatchers.IO) {
                    // ✅ PERBAIKAN: Pastikan 'id' dari dokumen diisi ke objek Sale
                    val sales = snapshots?.documents?.mapNotNull { doc ->
                        doc.toObject(Sale::class.java)?.apply { id = doc.id }
                    } ?: emptyList()

                    saleDao.deleteAll()
                    saleDao.insertAll(sales)
                }
            }
    }

    fun syncAllSalesForManager(): ListenerRegistration {
        return firestore.collection("sales")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("PosRepository", "Listen for ALL sales failed.", e)
                    return@addSnapshotListener
                }
                scope.launch(Dispatchers.IO) {
                    // ✅ PERBAIKAN: Pastikan 'id' dari dokumen diisi ke objek Sale
                    val sales = snapshots?.documents?.mapNotNull { doc ->
                        doc.toObject(Sale::class.java)?.apply { id = doc.id }
                    } ?: emptyList()
                    saleDao.deleteAll()
                    saleDao.insertAll(sales)
                }
            }
    }

    suspend fun processSale(
        cartItems: List<CartItem>,
        totalPrice: Double,
        user: UserProfile,
        activeUnitUsaha: UnitUsaha
    ) {
        withContext(Dispatchers.IO) {
            // --- VALIDASI STOK SEBELUM TRANSAKSI ---
            for (cartItem in cartItems) {
                // ✅ PERBAIKAN DI SINI: Gunakan 'asset.id' bukan 'asset.localId'
                val assetInDb = assetRepository.getAssetById(cartItem.asset.id).first()
                if (assetInDb == null || assetInDb.quantity < cartItem.quantity) {
                    throw IOException("Stok untuk ${cartItem.asset.name} tidak mencukupi. Sisa: ${assetInDb?.quantity ?: 0}")
                }
            }

            val allAccounts = accountRepository.allAccounts.first()
            val kasAccount = allAccounts.find { it.accountNumber == "111" }
            val pendapatanAccount = allAccounts.find { it.accountNumber == "411" }

            if (kasAccount == null || pendapatanAccount == null) {
                throw IllegalStateException("Akun Kas atau Pendapatan Penjualan tidak ditemukan.")
            }

            val sale = Sale(
                itemsJson = Gson().toJson(cartItems),
                totalPrice = totalPrice,
                transactionDate = System.currentTimeMillis(),
                userId = user.uid,
                unitUsahaId = activeUnitUsaha.id
            )
            firestore.collection("sales").add(sale).await()

            val transaction = Transaction(
                description = "Penjualan Tunai - ${activeUnitUsaha.name}",
                amount = totalPrice,
                date = System.currentTimeMillis(),
                debitAccountId = kasAccount.id,
                creditAccountId = pendapatanAccount.id,
                debitAccountName = kasAccount.accountName,
                creditAccountName = pendapatanAccount.accountName,
                unitUsahaId = activeUnitUsaha.id,
                userId = user.uid
            )
            transactionRepository.insert(transaction)

            cartItems.forEach { cartItem ->
                val updatedAsset = cartItem.asset.copy(
                    quantity = cartItem.asset.quantity - cartItem.quantity
                )
                assetRepository.update(updatedAsset)
            }
        }
    }
}
