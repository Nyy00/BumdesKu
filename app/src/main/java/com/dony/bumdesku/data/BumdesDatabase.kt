package com.dony.bumdesku.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 1. TAMBAHKAN Asset::class DI DALAM entities
@Database(entities = [Transaction::class, UnitUsaha::class, Asset::class], version = 2, exportSchema = false)
abstract class BumdesDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun unitUsahaDao(): UnitUsahaDao
    // 2. TAMBAHKAN FUNGSI ABSTRAK UNTUK DAO BARU
    abstract fun assetDao(): AssetDao

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
                    // PENTING: Karena kita mengubah versi database, kita butuh ini
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}