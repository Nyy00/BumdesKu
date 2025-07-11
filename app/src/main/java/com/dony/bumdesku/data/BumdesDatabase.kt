package com.dony.bumdesku.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 1. TAMBAHKAN UnitUsaha::class DI DALAM entities
@Database(entities = [Transaction::class, UnitUsaha::class], version = 1, exportSchema = false)
abstract class BumdesDatabase : RoomDatabase() {

    // 2. TAMBAHKAN FUNGSI ABSTRAK UNTUK DAO BARU
    abstract fun transactionDao(): TransactionDao
    abstract fun unitUsahaDao(): UnitUsahaDao

    companion object {
        @Volatile
        private var INSTANCE: BumdesDatabase? = null

        fun getDatabase(context: Context): BumdesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BumdesDatabase::class.java,
                    "bumdes_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}