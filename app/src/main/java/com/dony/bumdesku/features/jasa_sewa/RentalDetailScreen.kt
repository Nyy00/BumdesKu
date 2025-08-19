package com.dony.bumdesku.features.jasa_sewa

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dony.bumdesku.data.RentalTransaction
import com.dony.bumdesku.util.formatCurrency
import com.dony.bumdesku.util.BluetoothPrinterService
import com.dony.bumdesku.viewmodel.RentalViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalDetailScreen(
    viewModel: RentalViewModel,
    transactionId: String,
    onNavigateUp: () -> Unit,
    printerService: BluetoothPrinterService
) {
    val transactionState by viewModel.getRentalTransactionById(transactionId).collectAsStateWithLifecycle(initialValue = null)

    var showDeviceListDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Transaksi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    // Tampilkan tombol cetak jika transaksi ditemukan
                    if (transactionState != null) {
                        IconButton(onClick = { showDeviceListDialog = true }) {
                            Icon(Icons.Default.Print, contentDescription = "Cetak Struk")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (transactionState == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val transaction = transactionState!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Text("Penyewa: ${transaction.customerName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Barang: ${transaction.itemName}", style = MaterialTheme.typography.titleMedium)
                Text("Jumlah: ${transaction.quantity}", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))

                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                Text("Detail Waktu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Sewa dari: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.rentalDate))}")
                Text("Selesai pada: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.returnDate ?: 0L))}")

                Spacer(modifier = Modifier.height(16.dp))

                Text("Informasi Biaya", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Harga per hari: ${formatCurrency(transaction.pricePerDay)}")
                Text("Total Biaya: ${formatCurrency(transaction.totalPrice)}", fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(16.dp))

                if (transaction.notesOnReturn.isNotBlank()) {
                    Text("Catatan Pengembalian", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(transaction.notesOnReturn)
                }
            }
        }
    }

    // Dialog untuk memilih printer Bluetooth
    if (showDeviceListDialog) {
        val pairedDevices = printerService.getPairedDevices()
        AlertDialog(
            onDismissRequest = { showDeviceListDialog = false },
            title = { Text("Pilih Printer Bluetooth") },
            text = {
                if (pairedDevices.isEmpty()) {
                    Text("Tidak ada printer ter-pairing. Harap pairing printer dari pengaturan Bluetooth ponsel Anda.")
                } else {
                    LazyColumn {
                        items(pairedDevices) { device ->
                            Text(
                                text = device.name ?: "Unknown Device",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch {
                                            transactionState?.let { trx ->
                                                try {
                                                    val receiptText = viewModel.buildRentalReceiptText(trx)
                                                    printerService.printText(device, receiptText)
                                                    Toast.makeText(context, "Mencetak...", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Gagal mencetak: ${e.message}", Toast.LENGTH_LONG).show()
                                                } finally {
                                                    showDeviceListDialog = false
                                                }
                                            }
                                        }
                                    }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeviceListDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}
