package com.dony.bumdesku.repository

import com.dony.bumdesku.data.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class PosRepository(
    private val saleDao: SaleDao,
    private val assetRepository: AssetRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {

    // --- FUNGSI BARU ---
    fun getAllSales(): Flow<List<Sale>> {
        return saleDao.getAllSales()
    }
    // -------------------

    suspend fun processSale(
        cartItems: List<CartItem>,
        totalPrice: Double,
        user: UserProfile,
        activeUnitUsaha: UnitUsaha
    ) {
        // Menjalankan semua operasi database dalam satu blok 'withContext'
        // untuk memastikan semuanya terjadi di background thread yang benar.
        withContext(Dispatchers.IO) {
            // 1. Ambil akun-akun yang diperlukan
            val allAccounts = accountRepository.allAccounts.first() // Ambil data akun sekali saja
            val kasAccount = allAccounts.find { it.accountNumber == "111" }
            val pendapatanAccount = allAccounts.find { it.accountNumber == "413" }

            if (kasAccount == null || pendapatanAccount == null) {
                throw IllegalStateException("Akun Kas atau Pendapatan Penjualan tidak ditemukan.")
            }

            // 2. Simpan catatan penjualan ke tabel 'sales'
            val sale = Sale(
                itemsJson = Gson().toJson(cartItems),
                totalPrice = totalPrice,
                transactionDate = System.currentTimeMillis(),
                userId = user.uid,
                unitUsahaId = activeUnitUsaha.id
            )
            saleDao.insert(sale)

            // 3. Buat jurnal akuntansi otomatis
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

            // 4. Kurangi stok setiap aset yang terjual
            cartItems.forEach { cartItem ->
                val updatedAsset = cartItem.asset.copy(
                    quantity = cartItem.asset.quantity - cartItem.quantity
                )
                assetRepository.update(updatedAsset)
            }
        }
    }
}