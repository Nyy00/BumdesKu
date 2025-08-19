package com.dony.bumdesku.features.jasa_sewa

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dony.bumdesku.data.RentalItem
import com.dony.bumdesku.util.ThousandSeparatorVisualTransformation
import com.dony.bumdesku.viewmodel.RentalSaveState
import com.dony.bumdesku.viewmodel.RentalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRentalItemScreen(
    viewModel: RentalViewModel,
    itemToEdit: RentalItem? = null,
    onNavigateUp: () -> Unit
) {
    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var price by remember { mutableStateOf(itemToEdit?.rentalPricePerDay?.toLong()?.toString() ?: "") }
    var lateFee by remember { mutableStateOf(itemToEdit?.lateFeePerDay?.toLong()?.toString() ?: "") }

    // States untuk stok individual
    var stockBaik by remember { mutableStateOf(itemToEdit?.stockBaik?.toString() ?: "") }
    var stockRusakRingan by remember { mutableStateOf(itemToEdit?.stockRusakRingan?.toString() ?: "") }
    var stockPerluPerbaikan by remember { mutableStateOf(itemToEdit?.stockPerluPerbaikan?.toString() ?: "") }

    val isEditMode = itemToEdit != null
    val context = LocalContext.current
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    var showRepairDialog by remember { mutableStateOf(false) }

    LaunchedEffect(saveState) {
        when (saveState) {
            RentalSaveState.SUCCESS -> {
                Toast.makeText(context, "Barang sewaan berhasil disimpan", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
                onNavigateUp()
            }
            is RentalSaveState.ERROR -> {
                Toast.makeText(context, "Gagal menyimpan barang", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Barang Sewaan" else "Tambah Barang Sewaan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Barang (cth: Tenda Dome)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = price,
                onValueChange = { price = it.filter { c -> c.isDigit() } },
                label = { Text("Harga Sewa per Hari") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandSeparatorVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = lateFee,
                onValueChange = { lateFee = it.filter { c -> c.isDigit() } },
                label = { Text("Denda Keterlambatan per Hari (Opsional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandSeparatorVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            // Input untuk stok individual
            if (isEditMode) {
                OutlinedTextField(
                    value = stockBaik,
                    onValueChange = { stockBaik = it.filter { c -> c.isDigit() } },
                    label = { Text("Stok Baik (Bisa Disewakan)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stockRusakRingan,
                    onValueChange = { stockRusakRingan = it.filter { c -> c.isDigit() } },
                    label = { Text("Stok Rusak Ringan") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stockPerluPerbaikan,
                    onValueChange = { stockPerluPerbaikan = it.filter { c -> c.isDigit() } },
                    label = { Text("Stok Perlu Perbaikan") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Total Stok: ${itemToEdit!!.getTotalStock()}", style = MaterialTheme.typography.bodySmall)
            } else {
                OutlinedTextField(
                    value = stockBaik,
                    onValueChange = { stockBaik = it.filter { c -> c.isDigit() } },
                    label = { Text("Jumlah Stok Awal (Baik)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isEditMode) {
                Button(
                    onClick = { showRepairDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Perbaiki Stok")
                }
            }

            Button(
                onClick = {
                    val newItem = (itemToEdit ?: RentalItem()).copy(
                        name = name,
                        rentalPricePerDay = price.toDoubleOrNull() ?: 0.0,
                        lateFeePerDay = lateFee.toDoubleOrNull() ?: 0.0,
                        stockBaik = stockBaik.toIntOrNull() ?: 0,
                        stockRusakRingan = stockRusakRingan.toIntOrNull() ?: 0,
                        stockPerluPerbaikan = stockPerluPerbaikan.toIntOrNull() ?: 0
                    )
                    viewModel.saveItem(newItem)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = saveState != RentalSaveState.LOADING
            ) {
                if(saveState == RentalSaveState.LOADING) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Simpan")
                }
            }
        }
    }

    if (showRepairDialog && isEditMode && itemToEdit != null) {
        RepairItemDialog(
            item = itemToEdit,
            onDismiss = { showRepairDialog = false },
            onConfirmRepair = { quantity, fromCondition, repairCost ->
                viewModel.repairItem(itemToEdit, quantity, fromCondition, repairCost)
                showRepairDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepairItemDialog(
    item: RentalItem,
    onDismiss: () -> Unit,
    onConfirmRepair: (Int, String, Double) -> Unit
) {
    var quantity by remember { mutableStateOf("") }
    var selectedCondition by remember { mutableStateOf("Rusak Ringan") }
    var repairCost by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Perbaiki Stok: ${item.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Input Jumlah
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                    label = { Text("Jumlah yang Diperbaiki") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Dropdown Kondisi Asal
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCondition,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Perbaiki dari Kondisi") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        if (item.stockRusakRingan > 0) {
                            DropdownMenuItem(
                                text = { Text("Rusak Ringan (${item.stockRusakRingan})") },
                                onClick = {
                                    selectedCondition = "Rusak Ringan"
                                    isDropdownExpanded = false
                                }
                            )
                        }
                        if (item.stockPerluPerbaikan > 0) {
                            DropdownMenuItem(
                                text = { Text("Perlu Perbaikan (${item.stockPerluPerbaikan})") },
                                onClick = {
                                    selectedCondition = "Perlu Perbaikan"
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Input Biaya Perbaikan
                OutlinedTextField(
                    value = repairCost,
                    onValueChange = { repairCost = it.filter { c -> c.isDigit() } },
                    label = { Text("Biaya Perbaikan (Opsional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandSeparatorVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toIntOrNull() ?: 0
                    val cost = repairCost.toDoubleOrNull() ?: 0.0
                    if (qty > 0) {
                        onConfirmRepair(qty, selectedCondition, cost)
                    }
                }
            ) {
                Text("Konfirmasi Perbaikan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}