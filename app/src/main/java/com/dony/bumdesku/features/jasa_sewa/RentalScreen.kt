package com.dony.bumdesku.features.jasa_sewa

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dony.bumdesku.data.RentalItem
import com.dony.bumdesku.data.RentalTransaction
import com.dony.bumdesku.viewmodel.RentalSaveState
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
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    var itemToDelete by remember { mutableStateOf<RentalItem?>(null) }

    // Menampilkan notifikasi Toast setelah proses simpan/update/selesai
    LaunchedEffect(saveState) {
        when (saveState) {
            RentalSaveState.SUCCESS -> {
                Toast.makeText(context, "Operasi berhasil", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
            }
            RentalSaveState.ERROR -> {
                Toast.makeText(context, "Operasi gagal", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
            }
            else -> {}
        }
    }

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
                            onClick = { onNavigateToEditItem(item.id) },
                            onDeleteClick = { itemToDelete = item } // ✅ Tampilkan dialog saat ikon hapus diklik
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

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Konfirmasi Hapus") },
            text = { Text("Anda yakin ingin menghapus barang '${item.name}'? Aksi ini tidak dapat dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteItem(item)
                        itemToDelete = null
                        Toast.makeText(context, "'${item.name}' dihapus", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Ya, Hapus") }
            },
            dismissButton = {
                Button(onClick = { itemToDelete = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun RentalItemView(
    item: RentalItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Harga Sewa: ${currencyFormat.format(item.rentalPricePerDay)}/hari")
                Text("Stok Tersedia: ${item.availableStock} dari ${item.totalStock}")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.DeleteOutline, "Hapus Barang", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun RentalTransactionView(transaction: RentalTransaction, onCompleteClick: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Penyewa: ${transaction.customerName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Barang: ${transaction.itemName} (x${transaction.quantity})")
            Button(onClick = onCompleteClick, modifier = Modifier.align(Alignment.End)) {
                Text("Selesaikan (Kembali)")
            }
        }
    }
}