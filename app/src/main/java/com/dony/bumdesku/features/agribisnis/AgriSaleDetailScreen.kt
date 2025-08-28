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
        val receiptWidth = 32 // Lebar struk untuk printer 58mm

        // Fungsi bantuan untuk meratakan teks ke tengah
        fun centerText(text: String): String {
            val padding = (receiptWidth - text.length) / 2
            return " ".repeat(kotlin.math.max(0, padding)) + text
        }

        // Fungsi bantuan untuk membuat baris dengan teks kiri dan kanan
        fun createRow(left: String, right: String): String {
            val spaces = receiptWidth - left.length - right.length
            return left + " ".repeat(kotlin.math.max(0, spaces)) + right
        }

        val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
        val builder = StringBuilder()

        // Perintah inisialisasi printer
        val esc: Char = 27.toChar()
        val initPrinter = byteArrayOf(esc.code.toByte(), 64)
        val alignCenter = byteArrayOf(esc.code.toByte(), 97, 1)
        val alignLeft = byteArrayOf(esc.code.toByte(), 97, 0)
        val boldOn = byteArrayOf(esc.code.toByte(), 69, 1)
        val boldOff = byteArrayOf(esc.code.toByte(), 69, 0)

        builder.append(String(initPrinter))
        builder.append(String(alignCenter))
        builder.append(String(boldOn))
        builder.append("BUMDES Jangkang\n")
        builder.append(String(boldOff))
        builder.append("Unit Usaha Agribisnis\n\n")

        builder.append(String(alignLeft))
        builder.append(createRow("No:", sale.id.take(8).uppercase()))
        builder.append("\n")
        builder.append(createRow("Tgl:", dateFormat.format(Date(sale.transactionDate))))
        builder.append("\n")
        builder.append("-".repeat(receiptWidth)).append("\n")

        cartItems.forEach { item ->
            val pricePerUnit = item.harvest.sellingPrice.toLong()
            val subtotal = (item.quantity * item.harvest.sellingPrice).toLong()
            val formattedSubtotal = formatCurrency(subtotal.toDouble()).replace("Rp", "").trim()

            // Menangani kuantitas desimal atau bulat
            val quantityStr = if (item.quantity % 1.0 == 0.0) {
                "${item.quantity.toInt()} ${item.harvest.unit}"
            } else {
                "${item.quantity} ${item.harvest.unit}"
            }

            // Baris nama hasil panen
            builder.append("${item.harvest.name}\n")

            // Baris detail (Qty x Harga) dan Subtotal
            val leftDetail = " $quantityStr x $pricePerUnit"
            builder.append(createRow(leftDetail, formattedSubtotal))
            builder.append("\n")
        }

        builder.append("-".repeat(receiptWidth)).append("\n")

        val totalText = "TOTAL"
        val formattedTotal = formatCurrency(sale.totalPrice).replace("Rp", "").trim()
        builder.append(String(boldOn))
        builder.append(createRow(totalText, formattedTotal))
        builder.append("\n")
        builder.append(String(boldOff))

        builder.append("\n")
        builder.append(String(alignCenter))
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