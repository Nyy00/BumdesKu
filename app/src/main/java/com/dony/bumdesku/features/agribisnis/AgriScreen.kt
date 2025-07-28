package com.dony.bumdesku.features.agribisnis

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dony.bumdesku.data.UserProfile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dony.bumdesku.data.Harvest
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.util.ThousandSeparatorVisualTransformation
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// --- Layar Daftar Stok Hasil Panen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HarvestListScreen(
    viewModel: AgriViewModel,
    userRole: String,
    onAddHarvestClick: () -> Unit,
    onNavigateUp: () -> Unit
) {
    // ✅ PERBAIKAN DI SINI: Ubah 'initial' menjadi 'initialValue'
    val harvests by viewModel.allHarvests.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stok Hasil Agribisnis") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddHarvestClick) {
                Icon(Icons.Default.Add, "Catat Panen")
            }
        }
    ) { paddingValues ->
        if (harvests.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada data panen.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(harvests, key = { it.id }) { harvest ->
                    HarvestItem(harvest = harvest)
                }
            }
        }
    }
}
@Composable
fun HarvestItem(harvest: Harvest) {
    val localeID = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }
    val dateFormat = SimpleDateFormat("dd MMM yyyy", localeID)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(harvest.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Stok: ${harvest.quantity} ${harvest.unit}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Tgl Panen: ${dateFormat.format(Date(harvest.harvestDate))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    currencyFormat.format(harvest.sellingPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Harga Jual /${harvest.unit}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// --- Layar untuk Menambah Catatan Panen Baru ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHarvestScreen(
    viewModel: AgriViewModel,
    userRole: String,
    activeUnitUsaha: UnitUsaha?,
    onSaveComplete: () -> Unit,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("Kg") }
    var costPrice by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catat Panen Baru") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Hasil Panen (cth: Padi, Jagung)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Jumlah Panen") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Satuan") },
                    modifier = Modifier.width(120.dp)
                )
            }

            OutlinedTextField(
                value = costPrice,
                onValueChange = { newValue -> costPrice = newValue.filter { it.isDigit() } },
                label = { Text("Modal per Satuan (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandSeparatorVisualTransformation()
            )

            OutlinedTextField(
                value = sellingPrice,
                onValueChange = { newValue -> sellingPrice = newValue.filter { it.isDigit() } },
                label = { Text("Harga Jual per Satuan") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandSeparatorVisualTransformation()
            )

            Button(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                val selectedDate = dateState.selectedDateMillis?.let {
                    SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(it))
                } ?: "Pilih Tanggal Panen"
                Text(selectedDate)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val quantityDouble = quantity.toDoubleOrNull()
                    val costPriceDouble = costPrice.toDoubleOrNull() ?: 0.0
                    val sellingPriceDouble = sellingPrice.toDoubleOrNull()
                    val harvestDate = dateState.selectedDateMillis
                    val unitUsahaId = activeUnitUsaha?.id

                    if (name.isNotBlank() && quantityDouble != null && sellingPriceDouble != null && harvestDate != null && unitUsahaId != null) {
                        val newHarvest = Harvest(
                            name = name,
                            quantity = quantityDouble,
                            unit = unit,
                            costPrice = costPriceDouble,
                            sellingPrice = sellingPriceDouble,
                            harvestDate = harvestDate,
                            unitUsahaId = unitUsahaId
                        )
                        viewModel.insert(newHarvest)
                        Toast.makeText(context, "Data panen berhasil disimpan", Toast.LENGTH_SHORT).show()
                        onSaveComplete()
                    } else {
                        Toast.makeText(context, "Harap isi semua kolom dengan benar.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan Data Panen")
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = { showDatePicker = false }) {
                    Text("OK")
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProduceSaleScreen(
    viewModel: AgriViewModel,
    userProfile: UserProfile?,
    activeUnitUsaha: UnitUsaha?,
    onNavigateUp: () -> Unit,
    onSaleComplete: () -> Unit
) {
    val harvests by viewModel.allHarvests.collectAsStateWithLifecycle(initialValue = emptyList())
    val cartItems by viewModel.cartItems.collectAsState()
    val totalPrice by viewModel.totalPrice.collectAsState()
    val saleState by viewModel.saleState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(saleState) {
        when(saleState) {
            ProduceSaleState.SUCCESS -> {
                Toast.makeText(context, "Transaksi berhasil!", Toast.LENGTH_SHORT).show()
                viewModel.resetSaleState()
                onSaleComplete()
            }
            ProduceSaleState.ERROR -> {
                Toast.makeText(context, "Gagal memproses transaksi.", Toast.LENGTH_SHORT).show()
                viewModel.resetSaleState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jual Hasil Panen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(modifier = Modifier.height(80.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Harga")
                        Text(
                            text = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(totalPrice),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = {
                            if (userProfile != null && activeUnitUsaha != null) {
                                viewModel.completeSale(userProfile, activeUnitUsaha)
                            } else {
                                Toast.makeText(context, "Sesi pengguna tidak valid.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = saleState != ProduceSaleState.LOADING && cartItems.isNotEmpty(),
                        modifier = Modifier.height(50.dp)
                    ) {
                        if (saleState == ProduceSaleState.LOADING) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.ShoppingCartCheckout, "Selesaikan Transaksi")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bayar")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Kolom Kiri: Daftar Hasil Panen
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(harvests.filter { it.quantity > 0 }, key = { it.id }) { harvest ->
                    HarvestSaleItem(
                        harvest = harvest,
                        onClick = {
                            // Untuk agribisnis, kita asumsikan penjualan per satuan (1 Kg, 1 Ikat, dll)
                            viewModel.addToCart(harvest, 1.0)
                        }
                    )
                }
            }

            Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

            // Kolom Kanan: Keranjang Belanja
            CartPanel(
                modifier = Modifier.weight(1f),
                cartItems = cartItems,
                onRemoveItem = { viewModel.removeFromCart(it) }
            )
        }
    }
}

@Composable
fun HarvestSaleItem(harvest: Harvest, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(harvest.name, fontWeight = FontWeight.Bold, maxLines = 2)
            Text("Stok: ${harvest.quantity} ${harvest.unit}")
            Text(NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(harvest.sellingPrice))
        }
    }
}

@Composable
fun CartPanel(
    modifier: Modifier = Modifier,
    cartItems: List<AgriCartItem>,
    onRemoveItem: (AgriCartItem) -> Unit
) {
    Column(modifier = modifier.padding(8.dp)) {
        Text("Keranjang", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        if (cartItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Keranjang kosong")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cartItems, key = { it.harvest.id }) { item ->
                    AgriCartItemRow(
                        item = item,
                        onRemoveItem = { onRemoveItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun AgriCartItemRow(item: AgriCartItem, onRemoveItem: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.harvest.name, fontWeight = FontWeight.Bold)
                Text(
                    "${item.quantity} ${item.harvest.unit} x ${NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(item.harvest.sellingPrice)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onRemoveItem) {
                Icon(Icons.Default.Delete, "Hapus Item", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}