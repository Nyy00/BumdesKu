package com.dony.bumdesku.features.jasa_sewa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dony.bumdesku.data.RentalItem
import com.dony.bumdesku.data.RentalTransaction
import com.dony.bumdesku.viewmodel.RentalViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalScreen(
    viewModel: RentalViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToAddItem: () -> Unit,
    onNavigateToCreateTransaction: () -> Unit,
    onNavigateToEditItem: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dasbor Jasa Sewa") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu Lainnya")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tambah Barang Sewaan") },
                            onClick = {
                                onNavigateToAddItem()
                                showMenu = false
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreateTransaction) {
                Icon(Icons.Default.PostAdd, contentDescription = "Buat Transaksi Sewa")
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Barang Tersedia", style = MaterialTheme.typography.titleLarge)
                }
                if (uiState.rentalItems.isEmpty()) {
                    item { Text("Belum ada barang sewaan yang ditambahkan.") }
                } else {
                    items(uiState.rentalItems) { item ->
                        RentalItemView(
                            item = item,
                            onClick = { onNavigateToEditItem(item.id) }
                        )
                    }
                }

                item {
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Transaksi Aktif (Disewa)", style = MaterialTheme.typography.titleLarge)
                }
                if (uiState.activeTransactions.isEmpty()) {
                    item { Text("Tidak ada transaksi yang sedang aktif.") }
                } else {
                    items(uiState.activeTransactions) { transaction ->
                        RentalTransactionView(
                            transaction = transaction,
                            onCompleteClick = { viewModel.completeRental(transaction) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RentalItemView(item: RentalItem, onClick: () -> Unit) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Harga Sewa: ${currencyFormat.format(item.rentalPricePerDay)}/hari")
            Text("Stok Tersedia: ${item.availableStock} dari ${item.totalStock}")
        }
    }
}

@Composable
fun RentalTransactionView(transaction: RentalTransaction, onCompleteClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Penyewa: ${transaction.customerName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Barang: ${transaction.itemName} (x${transaction.quantity})")
            Button(onClick = onCompleteClick, modifier = Modifier.align(Alignment.End)) {
                Text("Selesaikan (Kembali)")
            }
        }
    }
}