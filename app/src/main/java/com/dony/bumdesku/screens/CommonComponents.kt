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

    @Composable
    fun FinancialHealthCard(data: FinancialHealthData) {
        val statusColor = when (data.status) {
            HealthStatus.SEHAT -> Color(0xFF2E7D32) // Hijau Tua
            HealthStatus.WASPADA -> Color(0xFFF9A825) // Kuning Tua
            HealthStatus.TIDAK_SEHAT -> Color(0xFFC62828) // Merah Tua
            HealthStatus.TIDAK_TERDEFINISI -> Color.Gray
        }

        val statusText = when (data.status) {
            HealthStatus.SEHAT -> "Sehat"
            HealthStatus.WASPADA -> "Waspada"
            HealthStatus.TIDAK_SEHAT -> "Tidak Sehat"
            HealthStatus.TIDAK_TERDEFINISI -> "Data Kurang"
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = statusColor)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Kesehatan Keuangan", color = Color.White)
                    Text(
                        statusText,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    String.format("%.2f:1", data.currentRatio),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


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

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { if (userRole == "pengurus") onItemClick() },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                        IconButton(onClick = onDeleteClick) {
                            Icon(Icons.Default.Delete, "Hapus Transaksi", tint = Color.Gray)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = transaction.debitAccountName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formattedAmount,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = transaction.creditAccountName,
                        modifier = Modifier.weight(1f).padding(start = 24.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formattedAmount,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }