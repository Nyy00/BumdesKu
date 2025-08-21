package com.dony.bumdesku.features.jasa_sewa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dony.bumdesku.data.RentalTransaction
import com.dony.bumdesku.util.formatDate
import com.dony.bumdesku.util.formatRupiah
import com.dony.bumdesku.viewmodel.RentalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerTransactionHistoryScreen(
    customerId: String,
    customerName: String,
    viewModel: RentalViewModel,
    onNavigateBack: () -> Unit
) {
    val transactions by viewModel.getTransactionsForCustomer(customerId).collectAsStateWithLifecycle(initialValue = emptyList()) // Menggunakan collectAsStateWithLifecycle

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Transaksi: $customerName") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Tidak ada riwayat transaksi.", fontStyle = FontStyle.Italic)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions) { transaction ->
                    TransactionCard(transaction = transaction)
                }
            }
        }
    }
}

@Composable
fun TransactionCard(transaction: RentalTransaction) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Item: ${transaction.itemName} (x${transaction.quantity})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text("Tgl Sewa: ${formatDate(transaction.rentalDate)}")
            Text("Tgl Kembali: ${transaction.returnDate?.let { formatDate(it) } ?: "Belum Kembali"}")
            Text("Total Biaya: Rp ${formatRupiah(transaction.totalPrice)}")
            Text("Status: ${transaction.status}")
        }
    }
}