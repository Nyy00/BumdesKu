package com.dony.bumdesku.features.jasa_sewa

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dony.bumdesku.data.RentalItem
import com.dony.bumdesku.data.RentalTransaction
import com.dony.bumdesku.util.ThousandSeparatorVisualTransformation
import com.dony.bumdesku.viewmodel.RentalSaveState
import com.dony.bumdesku.viewmodel.RentalViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalScreen(
    viewModel: RentalViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToAddItem: () -> Unit,
    onNavigateToCreateTransaction: () -> Unit,
    onNavigateToEditItem: (String) -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    var transactionToComplete by remember { mutableStateOf<RentalTransaction?>(null) }
    var itemToDelete by remember { mutableStateOf<RentalItem?>(null) }
    var itemToRepair by remember { mutableStateOf<RentalItem?>(null) }

    LaunchedEffect(saveState) {
        when (saveState) {
            RentalSaveState.SUCCESS -> {
                Toast.makeText(context, "Operasi berhasil", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
            }
            is RentalSaveState.ERROR -> {
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "Riwayat Sewa")
                    }
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
                        val rentedQty = uiState.rentedStockMap[item.id] ?: 0

                        RentalItemView(
                            item = item,
                            rentedQuantity = rentedQty,
                            onClick = { onNavigateToEditItem(item.id) },
                            onDeleteClick = { itemToDelete = item },
                            onRepairClick = { itemToRepair = item }
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
                            onCompleteClick = { transactionToComplete = transaction }
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
            text = { Text("Anda yakin ingin menghapus barang '${item.name}'?") },
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

    transactionToComplete?.let { trx ->
        CompleteRentalDialog(
            transaction = trx,
            onDismiss = { transactionToComplete = null },
            onConfirm = { returnedConditions, damageCost, notes ->
                viewModel.completeRental(trx, returnedConditions, damageCost, notes)
                transactionToComplete = null
            }
        )
    }

    itemToRepair?.let { item ->
        RepairItemDialog(
            item = item,
            onDismiss = { itemToRepair = null },
            onConfirmRepair = { quantity, fromCondition, repairCost ->
                viewModel.repairItem(item, quantity, fromCondition, repairCost)
                itemToRepair = null
            }
        )
    }
}

@Composable
fun RentalItemView(
    item: RentalItem,
    rentedQuantity: Int,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRepairClick: () -> Unit
) {
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
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Bisa Disewa: ${item.getAvailableStock()}", style = MaterialTheme.typography.bodyMedium)
                if (rentedQuantity > 0) {
                    Text(
                        "Sedang Disewakan: $rentedQuantity",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Red // Opsional, agar lebih menonjol
                    )
                }
                if (item.stockRusakRingan > 0) {
                    Text("Rusak Ringan: ${item.stockRusakRingan}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFFFA500))
                }
                if (item.stockPerluPerbaikan > 0) {
                    Text("Perlu Perbaikan: ${item.stockPerluPerbaikan}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
            Row {
                if (item.stockRusakRingan > 0 || item.stockPerluPerbaikan > 0) {
                    IconButton(onClick = onRepairClick) {
                        Icon(Icons.Default.Build, "Perbaiki Barang", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.DeleteOutline, "Hapus Barang", tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
fun RentalTransactionView(transaction: RentalTransaction, onCompleteClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Penyewa: ${transaction.customerName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Barang: ${transaction.itemName} (x${transaction.quantity})", style = MaterialTheme.typography.bodyMedium)
            Text("Wajib Kembali: ${dateFormat.format(Date(transaction.expectedReturnDate))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            Button(onClick = onCompleteClick, modifier = Modifier.align(Alignment.End)) {
                Text("Selesaikan (Kembali)")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteRentalDialog(
    transaction: RentalTransaction,
    onDismiss: () -> Unit,
    onConfirm: (returnedConditions: Map<String, Int>, damageCost: Double, notes: String) -> Unit
) {
    var baikCount by remember { mutableStateOf(transaction.quantity.toString()) }
    var rusakRinganCount by remember { mutableStateOf("0") }
    var perluPerbaikanCount by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }
    var damageCost by remember { mutableStateOf("") }

    val totalReturned = (baikCount.toIntOrNull() ?: 0) +
            (rusakRinganCount.toIntOrNull() ?: 0) +
            (perluPerbaikanCount.toIntOrNull() ?: 0)

    val isCountValid = totalReturned == transaction.quantity

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pengembalian: ${transaction.itemName} (x${transaction.quantity})") },
        text = {
            LazyColumn {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Masukkan jumlah barang yang dikembalikan sesuai kondisinya.")
                        ConditionInputRow(label = "Kondisi Baik", value = baikCount, onValueChange = { baikCount = it })
                        ConditionInputRow(label = "Rusak Ringan", value = rusakRinganCount, onValueChange = { rusakRinganCount = it })
                        ConditionInputRow(label = "Perlu Perbaikan", value = perluPerbaikanCount, onValueChange = { perluPerbaikanCount = it })

                        if (!isCountValid) {
                            Text(
                                "Total jumlah harus sama dengan ${transaction.quantity}! (Saat ini: $totalReturned)",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        OutlinedTextField(
                            value = damageCost,
                            onValueChange = { damageCost = it.filter { c -> c.isDigit() } },
                            label = { Text("Biaya Kerusakan (Opsional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = ThousandSeparatorVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Catatan Pengembalian (Opsional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val returnedConditions = mapOf(
                        "Baik" to (baikCount.toIntOrNull() ?: 0),
                        "Rusak Ringan" to (rusakRinganCount.toIntOrNull() ?: 0),
                        "Perlu Perbaikan" to (perluPerbaikanCount.toIntOrNull() ?: 0)
                    )
                    onConfirm(
                        returnedConditions,
                        damageCost.toDoubleOrNull() ?: 0.0,
                        notes
                    )
                },
                enabled = isCountValid
            ) {
                Text("Konfirmasi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
@Composable
fun ConditionInputRow(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}
