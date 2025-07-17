package com.dony.bumdesku.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dony.bumdesku.data.Account
import com.dony.bumdesku.data.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BukuPembantuScreen(
    account: Account?, // Akun yang sedang dilihat
    transactions: List<Transaction>, // Daftar transaksi untuk akun ini
    runningBalances: Map<Int, Double>, // Peta saldo berjalan
    onNavigateUp: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Buku Pembantu")
                        Text(
                            text = "${account?.accountNumber} - ${account?.accountName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Header Tabel
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tanggal", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
                    Text("Debit", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("Kredit", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("Saldo", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                }
                Divider()
            }

            // Isi Tabel
            items(transactions, key = { it.localId }) { trx ->
                BukuPembantuItem(
                    transaction = trx,
                    saldo = runningBalances[trx.localId] ?: 0.0,
                    selectedAccountId = account?.id ?: ""
                )
                Divider()
            }
        }
    }
}

@Composable
fun BukuPembantuItem(transaction: Transaction, saldo: Double, selectedAccountId: String) {
    val localeID = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }
    val dateFormat = SimpleDateFormat("dd/MM/yy", localeID)

    val debitAmount = if (transaction.debitAccountId == selectedAccountId) transaction.amount else 0.0
    val creditAmount = if (transaction.creditAccountId == selectedAccountId) transaction.amount else 0.0

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(dateFormat.format(Date(transaction.date)), modifier = Modifier.weight(1.5f))
        Text(currencyFormat.format(debitAmount), modifier = Modifier.weight(1f))
        Text(currencyFormat.format(creditAmount), modifier = Modifier.weight(1f))
        Text(currencyFormat.format(saldo), modifier = Modifier.weight(1f))
    }
}