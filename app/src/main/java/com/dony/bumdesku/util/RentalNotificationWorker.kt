package com.dony.bumdesku.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dony.bumdesku.data.BumdesDatabase
import com.dony.bumdesku.data.UserProfile
import com.dony.bumdesku.repository.AccountRepository
import com.dony.bumdesku.repository.RentalRepository
import com.dony.bumdesku.repository.TransactionRepository
import com.dony.bumdesku.R // Pastikan Anda memiliki resource string (R.string.app_name)
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RentalNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    private val database = BumdesDatabase.getDatabase(appContext)

    companion object {
        const val CHANNEL_ID = "rental_notification_channel"
        const val CHANNEL_NAME = "Notifikasi Jasa Sewa"
    }

    override suspend fun doWork(): Result {
        Log.d("RentalWorker", "Pekerjaan notifikasi dimulai...")
        val userId = auth.currentUser?.uid ?: return Result.failure()

        val managedUnitIds = try {
            val userProfileSnapshot = firestore.collection("users").document(userId).get().await()
            userProfileSnapshot.toObject(UserProfile::class.java)?.managedUnitUsahaIds ?: emptyList()
        } catch (e: Exception) {
            Log.e("RentalWorker", "Gagal mengambil profil pengguna", e)
            return Result.failure()
        }

        if (managedUnitIds.isEmpty()) {
            Log.d("RentalWorker", "Tidak ada unit usaha yang dikelola.")
            return Result.success()
        }

        val rentalRepository = RentalRepository(
            rentalDao = database.rentalDao(),
            transactionRepository = TransactionRepository(database.transactionDao()),
            accountRepository = AccountRepository(database.accountDao())
        )

        createNotificationChannel()

        withContext(Dispatchers.IO) {
            managedUnitIds.forEach { unitId ->
                val transactions = rentalRepository.getRentalTransactions(unitId).first()

                val overdueTransactions = transactions.filter { it.isOverdue() }
                val dueSoonTransactions = transactions.filter { it.isDueSoon() }

                if (overdueTransactions.isNotEmpty()) {
                    showNotification(
                        title = "Transaksi Lewat Jatuh Tempo!",
                        message = "Ada ${overdueTransactions.size} transaksi yang belum dikembalikan.",
                        notificationId = unitId.hashCode() // ID unik untuk setiap unit
                    )
                    Log.d("RentalWorker", "Ada ${overdueTransactions.size} transaksi yang jatuh tempo di unit $unitId.")
                }

                if (dueSoonTransactions.isNotEmpty()) {
                    showNotification(
                        title = "Pengingat Transaksi Sewa",
                        message = "Ada ${dueSoonTransactions.size} transaksi yang akan jatuh tempo hari ini.",
                        notificationId = unitId.hashCode() + 1 // ID unik lainnya
                    )
                    Log.d("RentalWorker", "Ada ${dueSoonTransactions.size} transaksi yang akan jatuh tempo di unit $unitId.")
                }
            }
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi untuk pengingat transaksi jasa sewa yang jatuh tempo."
            }
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, message: String, notificationId: Int) {
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ganti dengan ikon notifikasi Anda
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }
}