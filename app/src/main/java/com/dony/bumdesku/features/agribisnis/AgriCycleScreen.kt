package com.dony.bumdesku.features.agribisnis

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import com.dony.bumdesku.data.*
import com.dony.bumdesku.util.ThousandSeparatorVisualTransformation
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// 1. Layar Utama: Daftar Siklus Produksi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionCycleListScreen(
    viewModel: AgriCycleViewModel,
    onNavigateToCycleDetail: (String) -> Unit,
    onNavigateUp: () -> Unit
) {
    val cycles by viewModel.productionCycles.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Siklus Produksi Agribisnis") },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, "Kembali") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Mulai Siklus Baru")
            }
        }
    ) { paddingValues ->
        if (cycles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Belum ada siklus produksi. Mulai yang baru!")
            }
        } else {
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cycles, key = { it.id }) { cycle ->
                    CycleItemCard(cycle = cycle, onClick = { onNavigateToCycleDetail(cycle.id) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddNewCycleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, date ->
                viewModel.createProductionCycle(name, date)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CycleItemCard(cycle: ProductionCycle, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    val statusColor = if (cycle.status == CycleStatus.BERJALAN) Color(0xFF008800) else Color.Gray

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(cycle.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Dimulai: ${dateFormat.format(Date(cycle.startDate))}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Biaya:", style = MaterialTheme.typography.bodyMedium)
                Text(currencyFormat.format(cycle.totalCost), fontWeight = FontWeight.SemiBold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Status:", style = MaterialTheme.typography.bodyMedium)
                Text(cycle.status.name, fontWeight = FontWeight.SemiBold, color = statusColor)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewCycleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mulai Siklus Produksi Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Siklus (cth: Tanam Padi Gogo)") }
                )
                Button(onClick = { showDatePicker = true }) {
                    Text("Tanggal Mulai: ${SimpleDateFormat("dd MMM yyyy").format(Date(dateState.selectedDateMillis!!))}")
                }
                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = { Button(onClick = { showDatePicker = false }) { Text("OK") } }
                    ) { DatePicker(state = dateState) }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onConfirm(name, dateState.selectedDateMillis!!)
                }
            }) { Text("Mulai") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleDetailScreen(
    viewModel: AgriCycleViewModel,
    onNavigateUp: () -> Unit
) {
    val cycle by viewModel.selectedCycle.collectAsStateWithLifecycle()
    val costs by viewModel.cycleCosts.collectAsStateWithLifecycle()
    var showAddCostDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cycle?.name ?: "Detail Siklus") },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, "Kembali") } }
            )
        },
        floatingActionButton = {
            if (cycle?.status == CycleStatus.BERJALAN) {
                FloatingActionButton(onClick = { showAddCostDialog = true }) {
                    Icon(Icons.Default.Add, "Tambah Biaya")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bagian 1: Detail Ringkasan Siklus
            item {
                cycle?.let {
                    CycleDetailHeader(
                        cycle = it,
                        onFinishCycle = { totalHarvest ->
                            viewModel.finishProductionCycle(it, totalHarvest)
                        }
                    )
                }
            }

            // Bagian 2: Daftar Biaya
            item {
                Text(
                    "Rincian Biaya Produksi",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            if (costs.isEmpty()) {
                item {
                    Text(
                        "Belum ada biaya yang dicatat untuk siklus ini.",
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(costs, key = { it.id }) { cost ->
                    CostItemCard(cost = cost)
                }
            }
        }
    }

    if (showAddCostDialog) {
        cycle?.let {
            AddCostDialog(
                cycle = it,
                viewModel = viewModel,
                onDismiss = { showAddCostDialog = false }
            )
        }
    }
}

@Composable
fun CycleDetailHeader(
    cycle: ProductionCycle,
    onFinishCycle: (totalHarvest: Double) -> Unit
) {
    var showFinishDialog by remember { mutableStateOf(false) }

    CycleItemCard(cycle = cycle, onClick = {})

    if (cycle.status == CycleStatus.BERJALAN) {
        Button(
            onClick = { showFinishDialog = true },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Selesaikan Siklus & Hitung HPP")
        }
    }

    if (showFinishDialog) {
        FinishCycleDialog(
            onDismiss = { showFinishDialog = false },
            onConfirm = { totalHarvest ->
                onFinishCycle(totalHarvest)
                showFinishDialog = false
            }
        )
    }
}


@Composable
fun CostItemCard(cost: CycleCost) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(cost.description, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(dateFormat.format(Date(cost.date)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Text(currencyFormat.format(cost.amount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCostDialog(
    cycle: ProductionCycle,
    viewModel: AgriCycleViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    val costAccounts by viewModel.costAccounts.collectAsStateWithLifecycle()
    var selectedCostAccount by remember { mutableStateOf<Account?>(null) }
    var isCostExpanded by remember { mutableStateOf(false) }

    // [PERBAIKAN 1] Ambil daftar akun pembayaran dari ViewModel
    val paymentAccounts by viewModel.paymentAccounts.collectAsStateWithLifecycle()
    var selectedPaymentAccount by remember { mutableStateOf<Account?>(null) }
    var isPaymentExpanded by remember { mutableStateOf(false) }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Biaya Produksi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Keterangan Biaya (cth: Beli Pupuk)") }
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue -> amount = newValue.filter { it.isDigit() } },
                    label = { Text("Jumlah Biaya") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandSeparatorVisualTransformation()
                )

                ExposedDropdownMenuBox(expanded = isCostExpanded, onExpandedChange = { isCostExpanded = !isCostExpanded }) {
                    OutlinedTextField(
                        value = selectedCostAccount?.accountName ?: "Pilih Kategori Beban",
                        onValueChange = {}, readOnly = true, modifier = Modifier.menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCostExpanded) }
                    )
                    ExposedDropdownMenu(expanded = isCostExpanded, onDismissRequest = { isCostExpanded = false }) {
                        costAccounts.forEach { account ->
                            DropdownMenuItem(text = { Text(account.accountName) }, onClick = { selectedCostAccount = account; isCostExpanded = false })
                        }
                    }
                }

                // [PERBAIKAN 2] Gunakan `paymentAccounts` yang sudah berisi objek lengkap
                ExposedDropdownMenuBox(expanded = isPaymentExpanded, onExpandedChange = { isPaymentExpanded = !isPaymentExpanded }) {
                    OutlinedTextField(
                        value = selectedPaymentAccount?.accountName ?: "Bayar Dari Akun Mana?",
                        onValueChange = {}, readOnly = true, modifier = Modifier.menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPaymentExpanded) }
                    )
                    ExposedDropdownMenu(expanded = isPaymentExpanded, onDismissRequest = { isPaymentExpanded = false }) {
                        paymentAccounts.forEach { account ->
                            DropdownMenuItem(text = { Text(account.accountName) }, onClick = { selectedPaymentAccount = account; isPaymentExpanded = false })
                        }
                    }
                }

            }
        },
        confirmButton = {
            Button(onClick = {
                val amountDouble = amount.toDoubleOrNull()
                // [PERBAIKAN 3] Pastikan `selectedPaymentAccount` tidak null
                if (description.isNotBlank() && amountDouble != null && selectedCostAccount != null && selectedPaymentAccount != null) {
                    viewModel.addCostToCycle(cycle.id, cycle.unitUsahaId, description, amountDouble, selectedCostAccount!!, selectedPaymentAccount!!)
                    onDismiss()
                } else {
                    Toast.makeText(context, "Semua kolom harus diisi", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
fun FinishCycleDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var totalHarvest by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selesaikan Siklus") },
        text = {
            Column {
                Text("Masukkan total hasil panen (dalam satuan yang sama, cth: Kg) untuk menghitung HPP per unit.")
                OutlinedTextField(
                    value = totalHarvest,
                    onValueChange = { totalHarvest = it },
                    label = { Text("Total Hasil Panen (cth: 1500)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val harvestDouble = totalHarvest.toDoubleOrNull()
                if (harvestDouble != null && harvestDouble > 0) {
                    onConfirm(harvestDouble)
                } else {
                    Toast.makeText(context, "Harap isi total panen dengan benar.", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Selesaikan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
