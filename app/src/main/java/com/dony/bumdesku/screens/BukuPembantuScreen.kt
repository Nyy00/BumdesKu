package com.dony.bumdesku.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dony.bumdesku.data.Account
import com.dony.bumdesku.data.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BukuPembantuScreen(
    account: Account?,
    transactions: List<Transaction>,
    runningBalances: Map<Int, Double>,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Tabel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Keterangan", modifier = Modifier.weight(3f), fontWeight = FontWeight.Bold)
                Text("Debit/Kredit", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                Text("Saldo", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }
            Divider()

            // Isi Tabel
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp) // Beri jarak antar Card
            ) {
                items(transactions, key = { it.localId }) { trx ->
                    BukuPembantuItemCard( // Ganti ke Composable yang baru
                        transaction = trx,
                        saldo = runningBalances[trx.localId] ?: 0.0,
                        selectedAccountId = account?.id ?: ""
                    )
                }
            }
        }
    }
}

@Composable
fun BukuPembantuItemCard(transaction: Transaction, saldo: Double, selectedAccountId: String) {
    val localeID = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }
    val dateFormat = SimpleDateFormat("dd MMM yyyy", localeID)

    val isDebit = transaction.debitAccountId == selectedAccountId
    val amount = transaction.amount
    val amountColor = if (isDebit) MaterialTheme.colorScheme.onSurface else Color.Red

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kolom Kiri: Tanggal dan Keterangan
            Column(modifier = Modifier.weight(3f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(transaction.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Kolom Tengah: Debit atau Kredit
            Text(
                text = currencyFormat.format(amount),
                modifier = Modifier.weight(2f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
                color = amountColor,
                fontWeight = FontWeight.Medium
            )

            // Kolom Kanan: Saldo
            Text(
                text = currencyFormat.format(saldo),
                modifier = Modifier.weight(2f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
