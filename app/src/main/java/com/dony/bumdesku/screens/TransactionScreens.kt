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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dony.bumdesku.data.DashboardData
import com.dony.bumdesku.data.Transaction
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.viewmodel.TransactionViewModel
import com.dony.bumdesku.viewmodel.ChartData
import com.dony.bumdesku.screens.MonthlyBarChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    transactions: List<Transaction>,
    dashboardData: DashboardData,
    chartData: ChartData,
    userRole: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddItemClick: () -> Unit,
    onItemClick: (Transaction) -> Unit,
    onDeleteClick: (Transaction) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToUnitUsaha: () -> Unit,
    onNavigateToReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Cari deskripsi atau kategori...") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (searchQuery.isNotEmpty()) {
                                        onSearchQueryChange("")
                                    } else {
                                        isSearchActive = false
                                    }
                                }) {
                                    Icon(Icons.Default.Close, "Hapus atau Tutup")
                                }
                            }
                        )
                    } else {
                        Text("Daftar Transaksi")
                    }
                },
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = { isSearchActive = false }) {
                            Icon(Icons.Default.ArrowBack, "Tutup Pencarian")
                        }
                    } else {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.Default.ArrowBack, "Kembali")
                        }
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, "Cari")
                        }
                        IconButton(onClick = onNavigateToUnitUsaha) {
                            Icon(Icons.Default.Store, "Manajemen Unit Usaha")
                        }
                        IconButton(onClick = onNavigateToReport) {
                            Icon(Icons.Default.Assessment, "Halaman Laporan")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (userRole == "pengurus" && !isSearchActive) {
                FloatingActionButton(onClick = onAddItemClick) {
                    Icon(Icons.Default.Add, "Tambah Transaksi")
                }
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

        // Konten utama
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tampilkan Dashboard Card dan Grafik hanya jika tidak sedang mencari
            if (!isSearchActive) {
                DashboardCard(data = dashboardData)
                MonthlyBarChart(chartData = chartData)
            }

            if (transactions.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(if (searchQuery.isNotEmpty()) "Transaksi tidak ditemukan." else "Belum ada transaksi.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions, key = { it.localId }) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            userRole = userRole,
                            onItemClick = { if (userRole == "pengurus") onItemClick(transaction) },
                            onDeleteClick = { if (userRole == "pengurus") transactionToDelete = transaction }
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
    // State untuk tipe transaksi yang dipilih
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

            // --- ✅ KODE BARU UNTUK PILIHAN PEMASUKAN/PENGELUARAN ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                transactionTypes.forEach { text ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selectedType = text }
                    ) {
                        RadioButton(
                            selected = (text == selectedType),
                            onClick = { selectedType = text }
                        )
                        Text(text = text, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
            // --------------------------------------------------------

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
                            id = transactionToEdit?.id ?: "",
                            amount = amountDouble,
                            type = selectedType, // Gunakan state yang dipilih
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