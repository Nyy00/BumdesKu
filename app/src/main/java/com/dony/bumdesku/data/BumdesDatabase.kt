package com.dony.bumdesku.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.dony.bumdesku.data.FixedAsset
import com.dony.bumdesku.data.FixedAssetDao
import java.util.UUID

@Database(
    entities = [
        Transaction::class,
        UnitUsaha::class,
        Asset::class,
        Account::class,
        Payable::class,
        Receivable::class,
        Sale::class,
        Harvest::class,
        ProduceSale::class,
        ProductionCycle::class,
        AgriInventory::class,
        CycleCost::class,
        FixedAsset::class,
        RentalItem::class,
        RentalTransaction::class
    ],
    version = 23, // NAIKKAN VERSI DATABASE
    exportSchema = false
)
abstract class BumdesDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun unitUsahaDao(): UnitUsahaDao
    abstract fun assetDao(): AssetDao
    abstract fun accountDao(): AccountDao
    abstract fun debtDao(): DebtDao
    abstract fun saleDao(): SaleDao
    abstract fun agriDao(): AgriDao
    abstract fun cycleDao(): CycleDao
    abstract fun fixedAssetDao(): FixedAssetDao
    abstract fun rentalDao(): RentalDao

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
                    .addCallback(BumdesDatabaseCallback(CoroutineScope(Dispatchers.IO)))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class BumdesDatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    val accountDao = database.accountDao()
                    // Pre-populate data if needed
                }
            }
        }
    }
}