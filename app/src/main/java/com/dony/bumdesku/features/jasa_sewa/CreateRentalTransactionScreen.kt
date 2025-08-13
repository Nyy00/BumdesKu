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
import com.dony.bumdesku.data.RentalTransaction
import com.dony.bumdesku.viewmodel.RentalSaveState
import com.dony.bumdesku.viewmodel.RentalViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRentalTransactionScreen(
    viewModel: RentalViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availableItems = uiState.rentalItems

    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val dateState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedItem by remember { mutableStateOf<RentalItem?>(null) }
    var customerName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

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
                            // ✅ PERBAIKAN DI SINI
                            text = { Text("${item.name} (Stok: ${item.getAvailableStock()})") },
                            onClick = {
                                selectedItem = item
                                isDropdownExpanded = false
                            },
                            // ✅ DAN DI SINI
                            enabled = item.getAvailableStock() > 0
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
                    // ✅ DAN DI SINI
                    val maxStock = selectedItem?.getAvailableStock() ?: 1
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

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tanggal Wajib Kembali",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                Button(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    val selectedDate = dateState.selectedDateMillis?.let {
                        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(it))
                    } ?: "Pilih Tanggal"
                    Text(selectedDate)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val item = selectedItem
                    val expectedReturn = dateState.selectedDateMillis
                    if (item != null && customerName.isNotBlank() && expectedReturn != null) {
                        val transaction = RentalTransaction(
                            customerName = customerName,
                            rentalItemId = item.id,
                            itemName = item.name,
                            quantity = quantity.toIntOrNull() ?: 1,
                            expectedReturnDate = expectedReturn,
                            pricePerDay = item.rentalPricePerDay
                        )
                        viewModel.createRental(transaction)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedItem != null && saveState != RentalSaveState.LOADING
            ) {
                if (saveState == RentalSaveState.LOADING) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Simpan Transaksi")
                }
            }
        }
    }
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { Button(onClick = { showDatePicker = false }) { Text("OK") } }
        ) { DatePicker(state = dateState) }
    }
}
