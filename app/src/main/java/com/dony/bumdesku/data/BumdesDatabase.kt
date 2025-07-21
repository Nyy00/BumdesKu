package com.dony.bumdesku.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        Transaction::class,
        UnitUsaha::class,
        Asset::class,
        Account::class,
        Payable::class,
        Receivable::class
    ],
    version = 6,
    exportSchema = false
)
abstract class BumdesDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun unitUsahaDao(): UnitUsahaDao
    abstract fun assetDao(): AssetDao
    abstract fun accountDao(): AccountDao
    abstract fun debtDao(): DebtDao

    companion object {
        @Volatile
        private var INSTANCE: BumdesDatabase? = null

        fun getDatabase(context: Context): BumdesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BumdesDatabase::class.java,
                    "bumdes_database"
                )
                    .fallbackToDestructiveMigration()
                    // [PERBAIKAN] Tambahkan callback di sini
                    .addCallback(BumdesDatabaseCallback(CoroutineScope(Dispatchers.IO)))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    // [PERBAIKAN] Buat kelas Callback di sini
    private class BumdesDatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    populateDatabase(database.accountDao())
                }
            }
        }

        suspend fun populateDatabase(accountDao: AccountDao) {
            val defaultAccounts = listOf(
                // ASET LANCAR
                Account(id = UUID.randomUUID().toString(), accountNumber = "111", accountName = "Kas Tunai", category = AccountCategory.ASET),
                Account(id = UUID.randomUUID().toString(), accountNumber = "112", accountName = "Bank", category = AccountCategory.ASET),
                Account(id = UUID.randomUUID().toString(), accountNumber = "113", accountName = "Piutang Usaha", category = AccountCategory.ASET),
                // ASET TETAP
                Account(id = UUID.randomUUID().toString(), accountNumber = "121", accountName = "Peralatan", category = AccountCategory.ASET),
                // KEWAJIBAN
                Account(id = UUID.randomUUID().toString(), accountNumber = "211", accountName = "Utang Usaha", category = AccountCategory.KEWAJIBAN),
                // MODAL
                Account(id = UUID.randomUUID().toString(), accountNumber = "311", accountName = "Modal Disetor", category = AccountCategory.MODAL),
                Account(id = UUID.randomUUID().toString(), accountNumber = "312", accountName = "Prive", category = AccountCategory.MODAL),
                // PENDAPATAN
                Account(id = UUID.randomUUID().toString(), accountNumber = "411", accountName = "Pendapatan Jasa", category = AccountCategory.PENDAPATAN),
                Account(id = UUID.randomUUID().toString(), accountNumber = "412", accountName = "Pendapatan Sewa", category = AccountCategory.PENDAPATAN),
                // BEBAN
                Account(id = UUID.randomUUID().toString(), accountNumber = "511", accountName = "Beban Gaji", category = AccountCategory.BEBAN),
                Account(id = UUID.randomUUID().toString(), accountNumber = "512", accountName = "Beban Listrik & Air", category = AccountCategory.BEBAN)
            )
            defaultAccounts.forEach { accountDao.insert(it) }
        }
    }
}