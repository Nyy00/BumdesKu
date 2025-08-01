package com.dony.bumdesku.features.agribisnis

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dony.bumdesku.data.ProduceSale
import com.dony.bumdesku.features.toko.formatCurrency
import com.dony.bumdesku.util.BluetoothPrinterService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgriSaleDetailScreen(
    viewModel: AgriSaleDetailViewModel,
    sale: ProduceSale,
    onNavigateUp: () -> Unit,
    printerService: BluetoothPrinterService // Ditambahkan untuk fungsionalitas cetak
) {
    // Muat data sale ke ViewModel saat layar pertama kali dibuat
    LaunchedEffect(sale) {
        viewModel.loadSale(sale)
    }

    val cartItems by viewModel.cartItems.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))

    // Variabel untuk logika cetak
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showDeviceListDialog by remember { mutableStateOf(false) }

    // Fungsi untuk membangun teks struk agribisnis
    fun buildReceiptText(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
        val builder = StringBuilder()
        // Menggunakan format ESC/POS sederhana
        val esc: Char = 27.toChar()
        val initPrinter = byteArrayOf(esc.code.toByte(), 64)
        val alignCenter = byteArrayOf(esc.code.toByte(), 97, 1)
        val alignLeft = byteArrayOf(esc.code.toByte(), 97, 0)
        val alignRight = byteArrayOf(esc.code.toByte(), 97, 2)
        val boldOn = byteArrayOf(esc.code.toByte(), 69, 1)
        val boldOff = byteArrayOf(esc.code.toByte(), 69, 0)

        builder.append(String(initPrinter))
        builder.append(String(alignCenter))
        builder.append(String(boldOn))
        builder.append("BUMDES Jangkang\n")
        builder.append(String(boldOff))
        builder.append("Unit Usaha Agribisnis\n\n")
        builder.append(String(alignLeft))
        builder.append("No: ${sale.id.take(8)}\n")
        builder.append("Tgl: ${dateFormat.format(Date(sale.transactionDate))}\n")
        builder.append("--------------------------------\n")
        cartItems.forEach { item ->
            val itemName = item.harvest.name
            val qtyStr = "${if(item.quantity % 1.0 == 0.0) item.quantity.toInt() else item.quantity} ${item.harvest.unit}"
            val subtotal = formatCurrency(item.quantity * item.harvest.sellingPrice)
            builder.append("$itemName\n")

            val spaces = 32 - qtyStr.length - subtotal.length
            builder.append(qtyStr + " ".repeat(if(spaces > 0) spaces else 0) + subtotal + "\n")
        }
        builder.append("--------------------------------\n")
        builder.append(String(alignRight))
        builder.append(String(boldOn))
        builder.append("TOTAL : ${formatCurrency(sale.totalPrice)}\n\n")
        builder.append(String(alignCenter))
        builder.append(String(boldOff))
        builder.append("Terima kasih!\n\n\n\n")
        return builder.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Transaksi #${sale.id.take(6)}...") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeviceListDialog = true }) {
                        Icon(Icons.Default.Print, contentDescription = "Cetak Struk")
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
        ) {
            // Informasi Header
            Text("Tanggal: ${dateFormat.format(Date(sale.transactionDate))}")
            Spacer(modifier = Modifier.height(16.dp))
            Text("Item yang Dibeli:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Daftar Item
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(cartItems) { item ->
                    AgriDetailItemRow(item = item)
                }
            }

            // Informasi Total
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Belanja", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    formatCurrency(sale.totalPrice),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // Dialog untuk memilih printer
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
                                            try {
                                                val receiptText = buildReceiptText()
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

@Composable
fun AgriDetailItemRow(item: AgriCartItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            // Format angka untuk menghilangkan .0 jika tidak perlu
            text = "${if (item.quantity % 1 == 0.0) item.quantity.toInt() else item.quantity} ${item.harvest.unit}",
            modifier = Modifier.width(80.dp),
            fontWeight = FontWeight.Bold
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(item.harvest.name)
            Text(formatCurrency(item.harvest.sellingPrice), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Text(formatCurrency(item.harvest.sellingPrice * item.quantity))
    }
}