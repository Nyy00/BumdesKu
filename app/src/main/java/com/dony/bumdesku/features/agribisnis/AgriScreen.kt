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

// --- Layar Daftar Stok Hasil Panen ---
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

// --- Layar untuk Menambah Catatan Panen Baru (UPDATED) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHarvestScreen(
    viewModel: AgriViewModel,
    cycleViewModel: AgriCycleViewModel, // <-- Parameter baru
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

    // State untuk dropdown siklus
    val completedCycles by cycleViewModel.productionCycles
        .map { it.filter { cycle -> cycle.status == CycleStatus.SELESAI && !cycle.isArchived } }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedCycle by remember { mutableStateOf<ProductionCycle?>(null) }
    var isCycleExpanded by remember { mutableStateOf(false) }

    // Saat pengguna memilih siklus, otomatis isi nama dan modalnya
    LaunchedEffect(selectedCycle) {
        selectedCycle?.let { cycle ->
            name = cycle.name
            costPrice = cycle.hppPerUnit.toLong().toString()
            // Mengisi otomatis jumlah panen dari data siklus
            quantity = if (cycle.totalHarvest % 1 == 0.0) {
                // Jika angka bulat, hilangkan .0
                cycle.totalHarvest.toLong().toString()
            } else {
                cycle.totalHarvest.toString()
            }
        }
    }

    // Pastikan ViewModel mengambil data untuk unit usaha yang aktif
    LaunchedEffect(activeUnitUsaha) {
        activeUnitUsaha?.id?.let { cycleViewModel.setActiveUnit(it) }
    }

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
            // Dropdown untuk memilih siklus yang sudah selesai
            ExposedDropdownMenuBox(
                expanded = isCycleExpanded,
                onExpandedChange = { isCycleExpanded = !isCycleExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCycle?.name ?: "Pilih dari Siklus Selesai (Opsional)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ambil Data dari Siklus") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCycleExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isCycleExpanded,
                    onDismissRequest = { isCycleExpanded = false }
                ) {
                    completedCycles.forEach { cycle ->
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

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Hasil Panen (cth: Padi, Jagung)") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = selectedCycle != null // Kunci input jika siklus dipilih
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
                label = { Text("Modal per Satuan (HPP)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandSeparatorVisualTransformation(),
                readOnly = selectedCycle != null // Kunci input jika siklus dipilih
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

                        // Arsipkan siklus jika dipilih
                        selectedCycle?.let { cycleToArchive ->
                            // cycleViewModel.archiveCycle(cycleToArchive) // This function needs to be added to AgriCycleViewModel
                        }

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
        // ✅ --- PERUBAHAN UTAMA: Row diubah menjadi Column ---
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Bagian Atas: Daftar Hasil Panen
            LazyColumn(
                modifier = Modifier.weight(1.5f), // Beri porsi lebih besar
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
    // Tambahkan parameter baru untuk onQuantityChange
    onQuantityChange: (AgriCartItem, Double) -> Unit,
    onRemoveItem: (AgriCartItem) -> Unit
) {
    Column(modifier = modifier.padding(8.dp)) {
        Text("Keranjang", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        if (cartItems.isEmpty()) {
            // Tampilan keranjang kosong yang lebih menarik
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
                        // Kirim aksi ke ViewModel
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

    // ✅ --- State untuk menampilkan dialog peringatan ---
    var showStockAlert by remember { mutableStateOf(false) }

    val confirmChanges = {
        val newQty = textQuantity.toDoubleOrNull() ?: item.quantity

        // ✅ --- LOGIKA PENGECEKAN STOK ---
        if (newQty > item.harvest.quantity) {
            // Jika kuantitas melebihi stok, tampilkan alert
            showStockAlert = true
            // Kembalikan teks ke jumlah stok maksimal
            textQuantity = item.harvest.quantity.toString()
        } else if (newQty != item.quantity) {
            // Jika stok cukup dan nilai berubah, update kuantitas
            onQuantityChange(newQty)
        }
        focusManager.clearFocus() // Sembunyikan keyboard
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Baris Atas: Nama produk dan tombol hapus (tidak berubah)
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

            // ✅ Tampilkan informasi stok di bawah nama produk
            Text(
                "Stok tersedia: ${item.harvest.quantity} ${item.harvest.unit}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Baris Bawah: Kontrol kuantitas dan subtotal
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Kontrol Kuantitas
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
                            // Cek stok sebelum menambah dengan tombol '+'
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

                // Subtotal per item (tidak berubah)
                Text(
                    currencyFormat.format(subtotal),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    // ✅ --- AlertDialog untuk Peringatan Stok ---
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