package com.dony.bumdesku.features.jasa_sewa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dony.bumdesku.data.RentalTransaction
import com.dony.bumdesku.viewmodel.RentalViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalHistoryScreen(
    viewModel: RentalViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Transaksi Sewa") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            if (uiState.completedTransactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada transaksi yang sudah selesai.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("Daftar Transaksi Selesai", style = MaterialTheme.typography.titleLarge)
                        Divider(modifier = Modifier.padding(top = 8.dp))
                    }
                    items(uiState.completedTransactions) { transaction ->
                        CompletedRentalTransactionView(
                            transaction = transaction,
                            onClick = { onNavigateToDetail(transaction.id) }
                        )
                    }
                }
            }
        }
    }
}

// Composable untuk menampilkan detail transaksi selesai
@Composable
fun CompletedRentalTransactionView(
    transaction: RentalTransaction,
    onClick: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(transaction.id) }
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Penyewa: ${transaction.customerName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Barang: ${transaction.itemName} (x${transaction.quantity})", style = MaterialTheme.typography.bodyMedium)
            Text("Selesai pada: ${transaction.returnDate?.let { dateFormat.format(Date(it)) } ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
            Text("Total Biaya: ${NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(transaction.totalPrice)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            if (transaction.notesOnReturn.isNotBlank()) {
                Text("Catatan: ${transaction.notesOnReturn}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}