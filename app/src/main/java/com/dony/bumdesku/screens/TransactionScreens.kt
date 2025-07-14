package com.dony.bumdesku.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dony.bumdesku.data.DashboardData
import com.dony.bumdesku.data.Transaction
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    transactions: List<Transaction>,
    dashboardData: DashboardData,
    onAddItemClick: () -> Unit,
    onItemClick: (Transaction) -> Unit, // Diubah menjadi (Transaction)
    onDeleteClick: (Transaction) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToUnitUsaha: () -> Unit,
    onNavigateToReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Transaksi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToUnitUsaha) {
                        Icon(Icons.Default.Store, "Manajemen Unit Usaha")
                    }
                    IconButton(onClick = onNavigateToReport) {
                        Icon(Icons.Default.Assessment, "Halaman Laporan")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItemClick) {
                Icon(Icons.Default.Add, "Tambah Transaksi")
            }
        }
    ) { paddingValues ->
        if (transactionToDelete != null) {
            AlertDialog(
                onDismissRequest = { transactionToDelete = null },
                title = { Text("Konfirmasi Hapus") },
                text = { Text("Apakah Anda yakin ingin menghapus transaksi '${transactionToDelete?.description}'?") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteClick(transactionToDelete!!)
                            transactionToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text("Hapus") }
                },
                dismissButton = {
                    Button(onClick = { transactionToDelete = null }) { Text("Batal") }
                }
            )
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            DashboardCard(data = dashboardData)
            if (transactions.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Belum ada transaksi.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Kunci menggunakan localId yang unik
                    items(transactions, key = { it.localId }) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            // Kirim seluruh objek transaksi
                            onItemClick = { onItemClick(transaction) },
                            onDeleteClick = { transactionToDelete = transaction }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    transactionToEdit: Transaction? = null,
    onSave: (Transaction) -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: TransactionViewModel
) {
    val unitUsahaList by viewModel.allUnitUsaha.collectAsStateWithLifecycle(emptyList())

    var description by remember { mutableStateOf(transactionToEdit?.description ?: "") }
    var amount by remember { mutableStateOf(transactionToEdit?.amount?.takeIf { it > 0 }?.toString() ?: "") }
    var category by remember { mutableStateOf(transactionToEdit?.category ?: "") }
    val transactionTypes = listOf("PEMASUKAN", "PENGELUARAN")
    var selectedType by remember { mutableStateOf(transactionToEdit?.type ?: transactionTypes[0]) }

    var selectedUnitUsaha by remember { mutableStateOf<UnitUsaha?>(null) }
    var isUnitUsahaExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(unitUsahaList, transactionToEdit) {
        if (transactionToEdit != null && unitUsahaList.isNotEmpty()) {
            selectedUnitUsaha = unitUsahaList.find { it.id == transactionToEdit.unitUsahaId }
        }
    }

    val context = LocalContext.current
    val screenTitle = if (transactionToEdit == null) "Tambah Transaksi Baru" else "Edit Transaksi"

    Scaffold(
        topBar = { TopAppBar(title = { Text(screenTitle) }, navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(Icons.Default.ArrowBack, "Kembali")
            }
        }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Nominal (Contoh: 50000)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Kategori (Contoh: Sewa Kursi)") },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = isUnitUsahaExpanded,
                onExpandedChange = { isUnitUsahaExpanded = !isUnitUsahaExpanded }
            ) {
                OutlinedTextField(
                    value = selectedUnitUsaha?.name ?: "Pilih Unit Usaha",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unit Usaha") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitUsahaExpanded)
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isUnitUsahaExpanded,
                    onDismissRequest = { isUnitUsahaExpanded = false }
                ) {
                    unitUsahaList.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text(unit.name) },
                            onClick = {
                                selectedUnitUsaha = unit
                                isUnitUsahaExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    if (description.isNotBlank() && amountDouble != null && category.isNotBlank() && selectedUnitUsaha != null) {
                        val newTransaction = Transaction(
                            localId = transactionToEdit?.localId ?: 0,
                            id = transactionToEdit?.id ?: "", // Diperbaiki, defaultnya string kosong
                            amount = amountDouble,
                            type = selectedType,
                            category = category,
                            description = description,
                            date = transactionToEdit?.date ?: System.currentTimeMillis(),
                            unitUsahaId = selectedUnitUsaha!!.id
                        )
                        onSave(newTransaction)
                    } else {
                        Toast.makeText(context, "Harap isi semua kolom, termasuk Unit Usaha", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (transactionToEdit == null) "Simpan" else "Perbarui")
            }
        }
    }
}