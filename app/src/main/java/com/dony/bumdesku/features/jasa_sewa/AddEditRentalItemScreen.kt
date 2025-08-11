package com.dony.bumdesku.features.jasa_sewa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dony.bumdesku.data.RentalItem
import com.dony.bumdesku.util.ThousandSeparatorVisualTransformation
import com.dony.bumdesku.viewmodel.RentalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRentalItemScreen(
    viewModel: RentalViewModel,
    itemToEdit: RentalItem? = null,
    onNavigateUp: () -> Unit
) {
    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var price by remember { mutableStateOf(itemToEdit?.rentalPricePerDay?.toLong()?.toString() ?: "") } // Ubah ke Long agar tidak ada .0
    var totalStock by remember { mutableStateOf(itemToEdit?.totalStock?.toString() ?: "") }
    val isEditMode = itemToEdit != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Barang Sewaan" else "Tambah Barang Sewaan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
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
                value = totalStock,
                onValueChange = { totalStock = it.filter { c -> c.isDigit() } },
                label = { Text("Jumlah Stok Total") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val newItem = (itemToEdit ?: RentalItem()).copy(
                        name = name,
                        rentalPricePerDay = price.toDoubleOrNull() ?: 0.0,
                        totalStock = totalStock.toIntOrNull() ?: 0,
                        availableStock = if (isEditMode) itemToEdit!!.availableStock else totalStock.toIntOrNull() ?: 0
                    )
                    viewModel.saveItem(newItem)
                    onNavigateUp()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan")
            }
        }
    }
}