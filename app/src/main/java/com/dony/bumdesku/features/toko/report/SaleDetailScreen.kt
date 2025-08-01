package com.dony.bumdesku.features.toko.report

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
import com.dony.bumdesku.data.CartItem
import com.dony.bumdesku.data.Sale
import com.dony.bumdesku.features.toko.formatCurrency
import com.dony.bumdesku.util.BluetoothPrinterService
import android.annotation.SuppressLint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailScreen(
    viewModel: SaleDetailViewModel,
    sale: Sale,
    onNavigateUp: () -> Unit,
    printerService: BluetoothPrinterService
) {
    // Muat data sale ke ViewModel saat layar pertama kali dibuat
    LaunchedEffect(sale) {
        viewModel.loadSale(sale)
    }

    val cartItems by viewModel.cartItems.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))

    // ✅ --- Variabel yang hilang ditambahkan di sini ---
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showDeviceListDialog by remember { mutableStateOf(false) }


    fun buildReceiptText(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
        val builder = StringBuilder()
        // Menggunakan format ESC/POS sederhana untuk tebal dan rata tengah
        val esc: Char = 27.toChar()
        val gs: Char = 29.toChar()
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
        builder.append("Jl. Raya Desa Jangkang\n\n")
        builder.append(String(alignLeft))
        builder.append("No: ${sale.id.take(8)}\n")
        builder.append("Tgl: ${dateFormat.format(Date(sale.transactionDate))}\n")
        builder.append("--------------------------------\n")
        cartItems.forEach { item ->
            val itemName = item.asset.name
            val qtyPrice = "${item.quantity} x ${item.asset.sellingPrice.toLong()}"
            val subtotal = formatCurrency((item.quantity * item.asset.sellingPrice))
            builder.append("$itemName\n")

            // Membuat baris harga dan subtotal rata kanan
            val spaces = 32 - qtyPrice.length - subtotal.length
            builder.append(qtyPrice + " ".repeat(if(spaces > 0) spaces else 0) + subtotal + "\n")
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
                title = { Text("Detail Transaksi #${sale.id.take(8)}...") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                },
                // ✅ --- Tombol cetak ditambahkan di sini ---
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
                    DetailItemRow(item = item)
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

    // ✅ --- Dialog dipindahkan ke luar Scaffold content ---
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
                                // Menampilkan nama perangkat, pastikan permission tidak null
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
fun DetailItemRow(item: CartItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${item.quantity}x",
            modifier = Modifier.width(40.dp),
            fontWeight = FontWeight.Bold
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(item.asset.name)
            Text(formatCurrency(item.asset.sellingPrice), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Text(formatCurrency(item.asset.sellingPrice * item.quantity))
    }
}