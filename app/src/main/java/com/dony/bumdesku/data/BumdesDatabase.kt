package com.dony.bumdesku.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Transaction::class], version = 1, exportSchema = false)
abstract class BumdesDatabase : RoomDatabase() {

    // Fungsi abstrak yang akan menghubungkan database dengan DAO
    abstract fun transactionDao(): TransactionDao

    companion object {
        // @Volatile memastikan instance ini selalu up-to-date dan aman untuk multithreading
        @Volatile
        private var INSTANCE: BumdesDatabase? = null

        fun getDatabase(context: Context): BumdesDatabase {
            // synchronized memastikan hanya satu thread yang bisa mengakses blok ini pada satu waktu
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BumdesDatabase::class.java,
                    "bumdes_database" // Ini adalah nama file database yang akan dibuat di perangkat
                ).build()
                INSTANCE = instance
                // mengembalikan instance
                instance
            }
        }
    }
}