package com.dony.bumdesku.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// ✅ NAIKKAN VERSI MENJADI 6 DAN TAMBAHKAN ENTITAS BARU
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
    abstract fun debtDao(): DebtDao // <-- Tambahkan DAO baru

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
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}