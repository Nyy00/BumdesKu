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

    val availableStockResult by viewModel.availabilityState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val dateRangePickerState = rememberDateRangePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedItem by remember { mutableStateOf<RentalItem?>(null) }
    var customerName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Mengambil tanggal dari state picker
    val startDate = dateRangePickerState.selectedStartDateMillis
    val endDate = dateRangePickerState.selectedEndDateMillis

    LaunchedEffect(selectedItem, startDate, endDate) {
        // Ambil unitId dari item yang sedang dipilih
        val unitId = selectedItem?.unitUsahaId

        if (selectedItem != null && unitId != null && startDate != null && endDate != null) {
            // Pastikan unitId dikirim saat memanggil viewModel
            viewModel.checkItemAvailability(selectedItem!!.id, unitId, startDate, endDate)
        } else {
            viewModel.clearAvailabilityCheck()
        }
    }

    LaunchedEffect(saveState) {
        when (saveState) {
            RentalSaveState.SUCCESS -> {
                Toast.makeText(context, "Transaksi sewa berhasil dibuat", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
                onNavigateUp()
            }
            is RentalSaveState.ERROR -> {
                val errorMessage = (saveState as RentalSaveState.ERROR).message
                Toast.makeText(context, "Gagal: $errorMessage", Toast.LENGTH_LONG).show()
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
                            text = { Text("${item.name} (Stok: ${item.getAvailableStock()})") },
                            onClick = {
                                selectedItem = item
                                isDropdownExpanded = false
                            },
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
                    val qty = newQty.filter { c -> c.isDigit() }
                    quantity = qty
                },
                label = { Text("Jumlah") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )


            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Rentang Tanggal Sewa",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                Button(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    val startDateText = startDate?.let { formatter.format(Date(it)) } ?: "Mulai"
                    val endDateText = endDate?.let { formatter.format(Date(it)) } ?: "Selesai"
                    Text("$startDateText  -  $endDateText")
                }
            }

            Box(modifier = Modifier.padding(vertical = 8.dp)) {
                availableStockResult?.let { result ->
                    result.fold(
                        onSuccess = { stock ->
                            Text(
                                text = "Stok tersedia pada tanggal tersebut: $stock",
                                color = if (stock > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onFailure = { error ->
                            Text(
                                text = "Gagal memeriksa stok: ${error.message}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                } ?: run {
                    if (selectedItem != null && startDate != null && endDate != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Memeriksa ketersediaan...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            val stockForSelectedDate = availableStockResult?.getOrNull() ?: -1
            val quantityInt = quantity.toIntOrNull() ?: 0

            val canSave = selectedItem != null &&
                    customerName.isNotBlank() &&
                    startDate != null &&
                    endDate != null &&
                    quantityInt > 0 &&
                    saveState != RentalSaveState.LOADING &&
                    stockForSelectedDate >= quantityInt

            Button(
                onClick = {
                    val transaction = RentalTransaction(
                        customerName = customerName,
                        rentalItemId = selectedItem!!.id,
                        itemName = selectedItem!!.name,
                        quantity = quantityInt,
                        rentalDate = startDate!!,
                        expectedReturnDate = endDate!!,
                        pricePerDay = selectedItem!!.rentalPricePerDay,
                        status = "Dipesan"
                    )
                    viewModel.createRental(transaction)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave
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
            confirmButton = {
                Button(
                    onClick = { showDatePicker = false },
                    enabled = dateRangePickerState.selectedEndDateMillis != null // Tombol OK aktif jika rentang sudah dipilih
                ) { Text("OK") }
            },
            dismissButton = {
                Button(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DateRangePicker(state = dateRangePickerState)
        }
    }
}