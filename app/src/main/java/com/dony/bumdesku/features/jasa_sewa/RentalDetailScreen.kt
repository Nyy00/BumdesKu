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
import com.dony.bumdesku.util.BluetoothPrinterService
import com.dony.bumdesku.util.formatCurrency
import com.dony.bumdesku.viewmodel.RentalViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
                title = { Text("Detail Transaksi Sewa") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    if (transactionState != null) {
                        IconButton(onClick = { showDeviceListDialog = true }) {
                            Icon(Icons.Default.Print, contentDescription = "Cetak Bukti Sewa")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        val transaction = transactionState

        if (transaction == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // --- KARTU INFORMASI UTAMA ---
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = transaction.itemName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Penyewa: ${transaction.customerName}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Jumlah: ${transaction.quantity} unit",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // --- KARTU DETAIL WAKTU ---
                item {
                    DetailCard(title = "Periode Sewa") {
                        val simpleDateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID"))
                        DetailRow("Tanggal Sewa", simpleDateFormat.format(Date(transaction.rentalDate)))
                        DetailRow("Tanggal Kembali", transaction.returnDate?.let { simpleDateFormat.format(Date(it)) } ?: "Belum Kembali")
                    }
                }

                // --- KARTU RINCIAN BIAYA ---
                item {
                    DetailCard(title = "Rincian Biaya") {
                        // Menghitung sisa pembayaran berdasarkan data yang tersimpan
                        val sisaPembayaran = transaction.totalPrice - transaction.downPayment

                        DetailRow("Harga per Hari", formatCurrency(transaction.pricePerDay))
                        DetailRow("Total Dibayar", formatCurrency(transaction.downPayment))

                        // Hanya tampilkan sisa pembayaran jika nilainya lebih dari nol
                        if (sisaPembayaran > 0) {
                            DetailRow("Sisa Pembayaran", formatCurrency(sisaPembayaran))
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        DetailRow("Total Biaya Sewa", formatCurrency(transaction.totalPrice), isTotal = true)
                    }
                }

                // --- KARTU CATATAN ---
                if (transaction.notesOnReturn.isNotBlank()) {
                    item {
                        DetailCard(title = "Catatan Pengembalian") {
                            Text(
                                text = transaction.notesOnReturn,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog untuk memilih printer Bluetooth (tidak ada perubahan di sini)
    if (showDeviceListDialog && transactionState != null) {
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
                                            try {
                                                val receiptText = viewModel.buildRentalReceiptText(transactionState!!)
                                                printerService.printText(device, receiptText)
                                                Toast
                                                    .makeText(
                                                        context,
                                                        "Mencetak...",
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                            } catch (e: Exception) {
                                                Toast
                                                    .makeText(
                                                        context,
                                                        "Gagal mencetak: ${e.message}",
                                                        Toast.LENGTH_LONG
                                                    )
                                                    .show()
                                            } finally {
                                                showDeviceListDialog = false
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

// Composable terpisah untuk Kartu Detail
@Composable
private fun DetailCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

// Composable terpisah untuk Baris Detail
@Composable
private fun DetailRow(label: String, value: String, isTotal: Boolean = false) {
    val valueStyle = if (isTotal) {
        MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    } else {
        MaterialTheme.typography.bodyMedium
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = valueStyle
        )
    }
}