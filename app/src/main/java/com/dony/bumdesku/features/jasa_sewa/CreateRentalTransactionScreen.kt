package com.dony.bumdesku.features.jasa_sewa

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dony.bumdesku.data.RentalItem
import com.dony.bumdesku.data.RentalTransaction
import com.dony.bumdesku.viewmodel.RentalSaveState
import com.dony.bumdesku.viewmodel.RentalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRentalTransactionScreen(
    viewModel: RentalViewModel,
    onNavigateUp: () -> Unit
) {
    // ✅ CARA MENGAMBIL DATA YANG BENAR
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availableItems = uiState.rentalItems

    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedItem by remember { mutableStateOf<RentalItem?>(null) }
    var customerName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Menampilkan Toast dan navigasi setelah proses simpan selesai
    LaunchedEffect(saveState) {
        when (saveState) {
            RentalSaveState.SUCCESS -> {
                Toast.makeText(context, "Transaksi sewa berhasil dibuat", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
                onNavigateUp()
            }
            RentalSaveState.ERROR -> {
                Toast.makeText(context, "Gagal membuat transaksi (mungkin stok tidak cukup)", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buat Transaksi Sewa") },
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dropdown untuk memilih barang
            ExposedDropdownMenuBox(
                expanded = isDropdownExpanded,
                onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedItem?.name ?: "Pilih Barang",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Barang yang Disewa") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    availableItems.forEach { item ->
                        DropdownMenuItem(
                            text = { Text("${item.name} (Stok: ${item.availableStock})") },
                            onClick = {
                                selectedItem = item
                                isDropdownExpanded = false
                            },
                            enabled = item.availableStock > 0
                        )
                    }
                }
            }

            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text("Nama Pelanggan") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = quantity,
                onValueChange = { newQty ->
                    val qty = newQty.filter { c -> c.isDigit() }.toIntOrNull() ?: 1
                    val maxStock = selectedItem?.availableStock ?: 1
                    // Batasi jumlah tidak melebihi stok yang tersedia
                    if (qty <= maxStock) {
                        quantity = newQty.filter { c -> c.isDigit() }
                    } else {
                        Toast.makeText(context, "Jumlah melebihi stok tersedia ($maxStock)", Toast.LENGTH_SHORT).show()
                    }
                },
                label = { Text("Jumlah") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val item = selectedItem
                    if (item != null && customerName.isNotBlank()) {
                        val transaction = RentalTransaction(
                            customerName = customerName,
                            rentalItemId = item.id,
                            itemName = item.name,
                            quantity = quantity.toIntOrNull() ?: 1,
                            pricePerDay = item.rentalPricePerDay,
                            // Harga total dihitung berdasarkan jumlah dan harga per hari
                            totalPrice = item.rentalPricePerDay * (quantity.toIntOrNull() ?: 1)
                        )
                        viewModel.createRental(transaction)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedItem != null && saveState != RentalSaveState.LOADING // Nonaktifkan saat loading
            ) {
                if (saveState == RentalSaveState.LOADING) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Simpan Transaksi")
                }
            }
        }
    }
}