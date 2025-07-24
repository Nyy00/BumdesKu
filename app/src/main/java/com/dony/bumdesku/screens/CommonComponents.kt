package com.dony.bumdesku.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dony.bumdesku.data.DashboardData
import com.dony.bumdesku.data.Transaction
import com.dony.bumdesku.data.FinancialHealthData
import com.dony.bumdesku.data.HealthStatus
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// [PERBAIKAN] Pindahkan FinancialHealthCard ke luar dari DashboardCard

@Composable
fun DashboardCard(data: DashboardData) {
    val localeID = Locale("in", "ID")
    val currencyFormat =
        NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Ringkasan Keuangan", style = MaterialTheme.typography.titleLarge)
            Divider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Pemasukan:")
                Text(
                    text = currencyFormat.format(data.totalIncome),
                    color = Color(0xFF008800),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Pengeluaran:")
                Text(
                    text = currencyFormat.format(data.totalExpenses),
                    color = Color.Red,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Divider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Saldo Akhir",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = currencyFormat.format(data.finalBalance),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

// Definisikan sebagai fungsi terpisah di sini
@Composable
fun TransactionItem(
    transaction: Transaction,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit,
    userRole: String
) {
    val localeID = Locale("in", "ID")
    val currencyFormat =
        NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", localeID)
    val formattedAmount = currencyFormat.format(transaction.amount)
    val formattedDate = dateFormat.format(Date(transaction.date))

    // Tentukan apakah item bisa diklik berdasarkan peran dan status kunci
    val isClickable = userRole == "pengurus" && !transaction.isLocked

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = isClickable,
                onClick = onItemClick
            ), // Gunakan variabel isClickable
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                top = 16.dp,
                bottom = 16.dp,
                end = 8.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                if (userRole == "pengurus") {
                    IconButton(
                        onClick = onDeleteClick,
                        enabled = !transaction.isLocked // Nonaktifkan tombol jika terkunci
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            "Hapus Transaksi",
                            // Ubah warna ikon menjadi abu-abu jika terkunci
                            tint = if (transaction.isLocked) Color.LightGray else Color.Gray
                        )
                    }
                }
            }
        }
    }
}