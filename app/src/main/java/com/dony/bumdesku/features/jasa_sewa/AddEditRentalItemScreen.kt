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
    // Jika mode edit, tampilkan total stok. Jika mode tambah, biarkan kosong.
    var totalStock by remember { mutableStateOf(if (itemToEdit != null) itemToEdit.getTotalStock().toString() else "") }
    val isEditMode = itemToEdit != null
    val context = LocalContext.current

    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

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

            OutlinedTextField(
                value = totalStock,
                onValueChange = { totalStock = it.filter { c -> c.isDigit() } },
                label = { Text(if (isEditMode) "Total Stok (Tidak dapat diubah)" else "Jumlah Stok Awal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                readOnly = isEditMode // Stok tidak bisa diubah saat edit
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val totalStockInt = totalStock.toIntOrNull() ?: 0
                    val newItem = (itemToEdit ?: RentalItem()).copy(
                        name = name,
                        rentalPricePerDay = price.toDoubleOrNull() ?: 0.0,
                        lateFeePerDay = lateFee.toDoubleOrNull() ?: 0.0,
                        // Saat item baru dibuat, semua stok dianggap "Baik"
                        stockBaik = if (isEditMode) itemToEdit!!.stockBaik else totalStockInt,
                        stockRusakRingan = if (isEditMode) itemToEdit!!.stockRusakRingan else 0,
                        stockPerluPerbaikan = if (isEditMode) itemToEdit!!.stockPerluPerbaikan else 0
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
}
