package com.dony.bumdesku.repository

import android.util.Log
import com.dony.bumdesku.data.*
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

class PosRepository(
    private val saleDao: SaleDao,
    private val assetRepository: AssetRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getAllSales(): Flow<List<Sale>> {
        return saleDao.getAllSales()
    }

    fun syncSales(targetUserId: String) {
        scope.launch {
            try {
                val userProfileSnapshot = firestore.collection("users").document(targetUserId).get().await()
                val userProfile = userProfileSnapshot.toObject(UserProfile::class.java)

                if (userProfile == null) {
                    Log.w("PosRepository", "Profil untuk $targetUserId tidak ditemukan.")
                    return@launch
                }

                val query = if (userProfile.role == "manager" || userProfile.role == "auditor") {
                    firestore.collection("sales")
                } else {
                    val managedIds = userProfile.managedUnitUsahaIds
                    if (managedIds.isEmpty()) {
                        saleDao.deleteAll()
                        return@launch
                    }
                    firestore.collection("sales").whereIn("unitUsahaId", managedIds.take(10))
                }

                query.addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w("PosRepository", "Listen for sales failed.", e)
                        return@addSnapshotListener
                    }
                    scope.launch(Dispatchers.IO) {
                        val sales = snapshots?.documents?.mapNotNull { doc ->
                            // --- PERBAIKI BAGIAN INI ---
                            val sale = doc.toObject(Sale::class.java)
                            // Karena 'id' di Sale adalah 'val', kita tidak bisa mengubahnya.
                            // Kita tidak perlu mengubah ID-nya di sini, karena ID dari Room (auto-generate) sudah cukup.
                            sale
                            // -------------------------
                        } ?: emptyList()
                        saleDao.deleteAll()
                        saleDao.insertAll(sales)
                        Log.d("PosRepository", "Sinkronisasi penjualan berhasil: ${sales.size} data diterima.")
                    }
                }
            } catch (e: Exception) {
                Log.e("PosRepository", "Gagal sinkronisasi penjualan: ", e)
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
            val allAccounts = accountRepository.allAccounts.first()
            val kasAccount = allAccounts.find { it.accountNumber == "111" }
            val pendapatanAccount = allAccounts.find { it.accountNumber == "413" }

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
            // Simpan ke firestore, bukan DAO lokal
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