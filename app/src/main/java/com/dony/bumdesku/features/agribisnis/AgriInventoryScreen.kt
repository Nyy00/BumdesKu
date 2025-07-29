package com.dony.bumdesku.features.agribisnis

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.dony.bumdesku.data.AgriInventory
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.util.ThousandSeparatorVisualTransformation
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgriInventoryListScreen(
    viewModel: AgriInventoryViewModel,
    onAddInventoryClick: () -> Unit,
    onNavigateUp: () -> Unit
) {
    val inventoryItems by viewModel.allAgriInventory.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventaris Agribisnis") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddInventoryClick) {
                Icon(Icons.Default.Add, "Tambah Inventaris")
            }
        }
    ) { paddingValues ->
        if (inventoryItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada data inventaris.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(inventoryItems, key = { it.id }) { item ->
                    AgriInventoryItem(item = item)
                }
            }
        }
    }
}

@Composable
fun AgriInventoryItem(item: AgriInventory) {
    val localeID = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }
    val dateFormat = SimpleDateFormat("dd MMM yyyy", localeID)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Stok: ${item.quantity} ${item.unit}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Tgl Beli: ${dateFormat.format(Date(item.purchaseDate))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    currencyFormat.format(item.cost),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Harga Beli", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAgriInventoryScreen(
    inventoryToEdit: AgriInventory? = null,
    viewModel: AgriInventoryViewModel,
    activeUnitUsaha: UnitUsaha?,
    onSaveComplete: () -> Unit,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val isEditMode = inventoryToEdit != null

    // State diinisialisasi dengan data yang ada jika dalam mode edit
    var name by remember { mutableStateOf(inventoryToEdit?.name ?: "") }
    var quantity by remember { mutableStateOf(inventoryToEdit?.quantity?.toString() ?: "") }
    var cost by remember { mutableStateOf(inventoryToEdit?.cost?.toLong()?.toString() ?: "") }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = inventoryToEdit?.purchaseDate ?: System.currentTimeMillis())
    var showDatePicker by remember { mutableStateOf(false) }

    val unitOptions = listOf("Kg", "Gram", "Ons", "Buah", "Bungkus", "Botol")
    var selectedUnit by remember { mutableStateOf(inventoryToEdit?.unit ?: unitOptions[0]) }
    var isUnitExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Inventaris" else "Tambah Inventaris Baru") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Barang (cth: Pupuk Urea, Bibit Padi)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Jumlah") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.width(8.dp))

                // <-- [PERUBAHAN 2] Ganti OutlinedTextField Satuan dengan Dropdown
                ExposedDropdownMenuBox(
                    expanded = isUnitExpanded,
                    onExpandedChange = { isUnitExpanded = !isUnitExpanded },
                    modifier = Modifier.width(130.dp) // Sesuaikan lebar jika perlu
                ) {
                    OutlinedTextField(
                        value = selectedUnit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Satuan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isUnitExpanded,
                        onDismissRequest = { isUnitExpanded = false }
                    ) {
                        unitOptions.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit) },
                                onClick = {
                                    selectedUnit = unit
                                    isUnitExpanded = false
                                }
                            )
                        }
                    }
                }
                // -----------------------------------------------------------
            }

            OutlinedTextField(
                value = cost,
                onValueChange = { newValue -> cost = newValue.filter { it.isDigit() } },
                label = { Text("Total Harga Beli") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandSeparatorVisualTransformation()
            )

            Button(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                val selectedDate = dateState.selectedDateMillis?.let {
                    SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(it))
                } ?: "Pilih Tanggal Pembelian"
                Text(selectedDate)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val quantityDouble = quantity.toDoubleOrNull()
                    val costDouble = cost.toDoubleOrNull()
                    val purchaseDate = dateState.selectedDateMillis
                    // Untuk edit, unitUsahaId tidak bisa diubah, ambil dari data lama
                    val unitUsahaId = inventoryToEdit?.unitUsahaId ?: activeUnitUsaha?.id

                    if (name.isNotBlank() && quantityDouble != null && costDouble != null && purchaseDate != null && unitUsahaId != null) {

                        // <-- 3. Logika Simpan/Update
                        if (isEditMode) {
                            val updatedInventory = inventoryToEdit!!.copy(
                                name = name,
                                quantity = quantityDouble,
                                unit = selectedUnit,
                                cost = costDouble,
                                purchaseDate = purchaseDate
                            )
                            viewModel.update(updatedInventory)
                            Toast.makeText(context, "Inventaris berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        } else {
                            val newInventory = AgriInventory(
                                name = name,
                                quantity = quantityDouble,
                                unit = selectedUnit,
                                cost = costDouble,
                                purchaseDate = purchaseDate,
                                unitUsahaId = unitUsahaId
                            )
                            viewModel.insert(newInventory)
                            Toast.makeText(context, "Inventaris berhasil disimpan", Toast.LENGTH_SHORT).show()
                        }
                        onSaveComplete()

                    } else {
                        Toast.makeText(context, "Harap isi semua kolom dengan benar.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditMode) "Update Inventaris" else "Simpan Inventaris")
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { Button(onClick = { showDatePicker = false }) { Text("OK") } }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = { showDatePicker = false }) {
                    Text("OK")
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}