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
import com.dony.bumdesku.data.Customer
import com.dony.bumdesku.data.PaymentStatus
import com.dony.bumdesku.data.RentalItem
import com.dony.bumdesku.data.RentalTransaction
import com.dony.bumdesku.util.ThousandSeparatorVisualTransformation
import com.dony.bumdesku.viewmodel.RentalSaveState
import com.dony.bumdesku.viewmodel.RentalViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRentalTransactionScreen(
    viewModel: RentalViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availableItems = uiState.rentalItems
    val customers = uiState.customers

    val availableStockResult by viewModel.availabilityState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val dateRangePickerState = rememberDateRangePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedItem by remember { mutableStateOf<RentalItem?>(null) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var quantity by remember { mutableStateOf("1") }

    // State untuk status pembayaran dan uang muka
    var paymentStatus by remember { mutableStateOf(PaymentStatus.BELUM_LUNAS) }
    var downPayment by remember { mutableStateOf("") }
    var isPaymentDropdownExpanded by remember { mutableStateOf(false) }

    var isItemDropdownExpanded by remember { mutableStateOf(false) }
    var isCustomerDropdownExpanded by remember { mutableStateOf(false) }

    val startDate = dateRangePickerState.selectedStartDateMillis
    val endDate = dateRangePickerState.selectedEndDateMillis

    // Hitung total harga sewa
    val durationInDays = if (startDate != null && endDate != null) {
        ceil((endDate - startDate) / (1000.0 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
    } else {
        0
    }
    val totalPrice = (selectedItem?.rentalPricePerDay ?: 0.0) * (quantity.toIntOrNull() ?: 0) * durationInDays

    val formattedTotalPrice = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }.format(totalPrice)

    LaunchedEffect(selectedItem, startDate, endDate) {
        val unitId = selectedItem?.unitUsahaId
        if (selectedItem != null && unitId != null && startDate != null && endDate != null) {
            viewModel.checkItemAvailability(selectedItem!!.id, unitId, startDate, endDate)
        } else {
            viewModel.clearAvailabilityCheck()
        }
    }

    LaunchedEffect(paymentStatus, totalPrice) {
        if (paymentStatus == PaymentStatus.LUNAS) {
            downPayment = totalPrice.toString()
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
                expanded = isItemDropdownExpanded,
                onExpandedChange = { isItemDropdownExpanded = !isItemDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedItem?.name ?: "Pilih Barang",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Barang yang Disewa") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isItemDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isItemDropdownExpanded,
                    onDismissRequest = { isItemDropdownExpanded = false }
                ) {
                    availableItems.forEach { item ->
                        DropdownMenuItem(
                            text = { Text("${item.name} (Stok: ${item.getAvailableStock()})") },
                            onClick = {
                                selectedItem = item
                                isItemDropdownExpanded = false
                            },
                            enabled = item.getAvailableStock() > 0
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = isCustomerDropdownExpanded,
                onExpandedChange = { isCustomerDropdownExpanded = !isCustomerDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCustomer?.name ?: "Pilih Pelanggan",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Penyewa") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCustomerDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isCustomerDropdownExpanded,
                    onDismissRequest = { isCustomerDropdownExpanded = false }
                ) {
                    customers.forEach { customer ->
                        DropdownMenuItem(
                            text = { Text(customer.name) },
                            onClick = {
                                selectedCustomer = customer
                                isCustomerDropdownExpanded = false
                            }
                        )
                    }
                }
            }

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

            Text(
                text = "Total Harga: $formattedTotalPrice",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.End)
            )

            ExposedDropdownMenuBox(
                expanded = isPaymentDropdownExpanded,
                onExpandedChange = { isPaymentDropdownExpanded = !isPaymentDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = paymentStatus.name.replace("_", " "),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status Pembayaran") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPaymentDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isPaymentDropdownExpanded,
                    onDismissRequest = { isPaymentDropdownExpanded = false }
                ) {
                    PaymentStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.name.replace("_", " ")) },
                            onClick = {
                                paymentStatus = status
                                isPaymentDropdownExpanded = false
                                if (status == PaymentStatus.LUNAS) {
                                    downPayment = totalPrice.toInt().toString()
                                } else {
                                    downPayment = ""
                                }
                            }
                        )
                    }
                }
            }

            // Kolom Uang Muka hanya ditampilkan untuk status DP dan Lunas
            if (paymentStatus == PaymentStatus.DP || paymentStatus == PaymentStatus.LUNAS) {
                OutlinedTextField(
                    value = downPayment,
                    onValueChange = { newDp ->
                        val dp = newDp.filter { c -> c.isDigit() }
                        downPayment = dp
                    },
                    label = { Text(if (paymentStatus == PaymentStatus.LUNAS) "Total Pembayaran (Lunas)" else "Uang Muka (DP)") },
                    enabled = paymentStatus == PaymentStatus.DP,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandSeparatorVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            val stockForSelectedDate = availableStockResult?.getOrNull() ?: -1
            val quantityInt = quantity.toIntOrNull() ?: 0

            val canSave = selectedItem != null &&
                    selectedCustomer != null &&
                    startDate != null &&
                    endDate != null &&
                    quantityInt > 0 &&
                    saveState != RentalSaveState.LOADING &&
                    stockForSelectedDate >= quantityInt &&
                    (paymentStatus != PaymentStatus.DP || (downPayment.toDoubleOrNull() ?: 0.0) <= totalPrice)


            Button(
                onClick = {
                    val transaction = RentalTransaction(
                        id = UUID.randomUUID().toString(),
                        customerId = selectedCustomer!!.id,
                        customerName = selectedCustomer!!.name,
                        rentalItemId = selectedItem!!.id,
                        itemName = selectedItem!!.name,
                        quantity = quantityInt,
                        rentalDate = startDate!!,
                        expectedReturnDate = endDate!!,
                        pricePerDay = selectedItem!!.rentalPricePerDay,
                        totalPrice = totalPrice,
                        paymentStatus = paymentStatus,
                        downPayment = downPayment.toDoubleOrNull() ?: 0.0,
                        unitUsahaId = selectedItem!!.unitUsahaId,
                        userId = viewModel.getCurrentUserId(),
                        status = "Disewa"
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
                    enabled = dateRangePickerState.selectedEndDateMillis != null
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