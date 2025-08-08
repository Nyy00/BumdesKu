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
import com.dony.bumdesku.data.CycleStatus
import com.dony.bumdesku.data.Harvest
import com.dony.bumdesku.data.ProductionCycle
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.util.ThousandSeparatorVisualTransformation
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.flow.map
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// --- HarvestListScreen dan HarvestItem (TIDAK ADA PERUBAHAN) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HarvestListScreen(
    viewModel: AgriViewModel,
    userRole: String,
    onAddHarvestClick: () -> Unit,
    onNavigateUp: () -> Unit
) {
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

// --- Layar untuk Menambah Catatan Panen Baru (DIROMBAK TOTAL) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHarvestScreen(
    viewModel: AgriViewModel,
    cycleViewModel: AgriCycleViewModel,
    userRole: String,
    activeUnitUsaha: UnitUsaha?,
    onSaveComplete: () -> Unit,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val localeID = Locale("in", "ID")
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 2 } }

    var quantity by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    val activeCycles by cycleViewModel.productionCycles
        .map { it.filter { cycle -> cycle.status == CycleStatus.BERJALAN } }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedCycle by remember { mutableStateOf<ProductionCycle?>(null) }
    var isCycleExpanded by remember { mutableStateOf(false) }

    val totalBiaya by cycleViewModel.totalBiayaSiklus.collectAsStateWithLifecycle()
    val totalPanenSebelumnya by cycleViewModel.totalPanenSebelumnya.collectAsStateWithLifecycle()
    val hpp by cycleViewModel.hppSementara.collectAsStateWithLifecycle()

    // ✅✅✅ PERBAIKAN UTAMA ADA DI SINI ✅✅✅
    // LaunchedEffect ini sekarang memiliki dua 'key'.
    // Akan berjalan saat layar pertama kali dibuka (activeUnitUsaha)
    // DAN setiap kali pengguna memilih siklus baru (selectedCycle).
    LaunchedEffect(activeUnitUsaha, selectedCycle) {
        // 1. Pastikan ViewModel tahu unit usaha mana yang aktif
        activeUnitUsaha?.id?.let { cycleViewModel.setActiveUnit(it) }

        // 2. Jika siklus sudah dipilih, panggil fungsi untuk mengambil data HPP
        selectedCycle?.id?.let {
            cycleViewModel.getHppInitialData(it)
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catat Hasil Panen") },
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
            ExposedDropdownMenuBox(
                expanded = isCycleExpanded,
                onExpandedChange = { isCycleExpanded = !isCycleExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCycle?.name ?: "Pilih Siklus Tanam",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Pilih dari Siklus Aktif") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCycleExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isCycleExpanded,
                    onDismissRequest = { isCycleExpanded = false }
                ) {
                    activeCycles.forEach { cycle ->
                        DropdownMenuItem(
                            text = { Text(cycle.name) },
                            onClick = {
                                selectedCycle = cycle
                                isCycleExpanded = false
                            }
                        )
                    }
                }
            }

            if (selectedCycle != null) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        quantity = it
                        cycleViewModel.calculateHpp(it)
                    },
                    label = { Text("Jumlah Panen Hari Ini (Kg)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Kalkulasi Harga Pokok Produksi (HPP)", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow("Total Biaya Terkumpul:", currencyFormat.format(totalBiaya))
                        InfoRow("Total Panen Sebelumnya:", "$totalPanenSebelumnya Kg")
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        InfoRow(
                            label = "Estimasi HPP Saat Ini:",
                            value = "${currencyFormat.format(hpp)} / Kg",
                            isBold = true
                        )
                    }
                }

                OutlinedTextField(
                    value = sellingPrice,
                    onValueChange = { newValue -> sellingPrice = newValue.filter { it.isDigit() } },
                    label = { Text("Harga Jual per Kg") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandSeparatorVisualTransformation(),
                    placeholder = { Text("Disarankan > ${currencyFormat.format(hpp)}") }
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
                        val sellingPriceDouble = sellingPrice.toDoubleOrNull()
                        val harvestDate = dateState.selectedDateMillis
                        val cycleId = selectedCycle?.id
                        val unitUsahaId = activeUnitUsaha?.id

                        if (selectedCycle != null && quantityDouble != null && quantityDouble > 0 && sellingPriceDouble != null && harvestDate != null && cycleId != null && unitUsahaId != null) {
                            val newHarvest = Harvest(
                                name = selectedCycle!!.name,
                                quantity = quantityDouble,
                                unit = "Kg",
                                costPrice = hpp,
                                sellingPrice = sellingPriceDouble,
                                harvestDate = harvestDate,
                                unitUsahaId = unitUsahaId
                            )
                            viewModel.insertPanenToCycle(newHarvest, cycleId)

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
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { Button(onClick = { showDatePicker = false }) { Text("OK") } }
        ) {
            DatePicker(state = dateState)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontWeight = if(isBold) FontWeight.Bold else FontWeight.Normal)
        Text(text = value, fontWeight = if(isBold) FontWeight.Bold else FontWeight.Normal)
    }
}


// --- ProduceSaleScreen dan komponen lainnya (TIDAK ADA PERUBAHAN) ---
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.weight(1.5f),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(harvests.filter { it.quantity > 0 }, key = { it.id }) { harvest ->
                    HarvestSaleItem(
                        harvest = harvest,
                        onClick = {
                            viewModel.addToCart(harvest, 1.0)
                        }
                    )
                }
            }

            Divider(modifier = Modifier.padding(horizontal = 8.dp))

            CartPanel(
                modifier = Modifier.weight(1f),
                cartItems = cartItems,
                onQuantityChange = { item, newQty ->
                    viewModel.updateQuantity(item, newQty)
                },
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
    onQuantityChange: (AgriCartItem, Double) -> Unit,
    onRemoveItem: (AgriCartItem) -> Unit
) {
    Column(modifier = modifier.padding(8.dp)) {
        Text("Keranjang", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Keranjang Kosong",
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Keranjang masih kosong", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cartItems, key = { it.harvest.id }) { item ->
                    AgriCartItemRow(
                        item = item,
                        onQuantityChange = { newQty -> onQuantityChange(item, newQty) },
                        onRemoveItem = { onRemoveItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun AgriCartItemRow(
    item: AgriCartItem,
    onQuantityChange: (Double) -> Unit,
    onRemoveItem: () -> Unit
) {
    val subtotal = item.harvest.sellingPrice * item.quantity
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply { maximumFractionDigits = 0 }

    var textQuantity by remember(item.quantity) {
        mutableStateOf(if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString())
    }
    val focusManager = LocalFocusManager.current
    var showStockAlert by remember { mutableStateOf(false) }

    val confirmChanges = {
        val newQty = textQuantity.toDoubleOrNull() ?: item.quantity
        if (newQty > item.harvest.quantity) {
            showStockAlert = true
            textQuantity = item.harvest.quantity.toString()
        } else if (newQty != item.quantity) {
            onQuantityChange(newQty)
        }
        focusManager.clearFocus()
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(item.harvest.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                IconButton(onClick = onRemoveItem, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, "Hapus Item", tint = MaterialTheme.colorScheme.error)
                }
            }
            Text(
                "Stok tersedia: ${item.harvest.quantity} ${item.harvest.unit}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = { onQuantityChange(item.quantity - 1) },
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Remove, "Kurangi")
                    }

                    OutlinedTextField(
                        value = textQuantity,
                        onValueChange = {
                            if (it.matches(Regex("^\\d*\\.?\\d*\$"))) {
                                textQuantity = it
                            }
                        },
                        modifier = Modifier
                            .width(80.dp)
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    confirmChanges()
                                }
                            },
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { confirmChanges() }
                        ),
                        singleLine = true
                    )

                    OutlinedButton(
                        onClick = {
                            if (item.quantity + 1 <= item.harvest.quantity) {
                                onQuantityChange(item.quantity + 1)
                            } else {
                                showStockAlert = true
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Add, "Tambah")
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    currencyFormat.format(subtotal),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
    if (showStockAlert) {
        AlertDialog(
            onDismissRequest = { showStockAlert = false },
            title = { Text("Stok Tidak Cukup") },
            text = { Text("Jumlah yang Anda masukkan melebihi stok yang tersedia (${item.harvest.quantity} ${item.harvest.unit}).") },
            confirmButton = {
                Button(onClick = { showStockAlert = false }) {
                    Text("Mengerti")
                }
            }
        )
    }
}