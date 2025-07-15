package com.dony.bumdesku.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Transaction::class, UnitUsaha::class, Asset::class], version = 2, exportSchema = false)
abstract class BumdesDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun unitUsahaDao(): UnitUsahaDao
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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}