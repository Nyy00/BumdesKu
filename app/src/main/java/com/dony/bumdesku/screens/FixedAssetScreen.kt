package com.dony.bumdesku.screens

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
import com.dony.bumdesku.viewmodel.FixedAssetUploadState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dony.bumdesku.viewmodel.*
import com.dony.bumdesku.data.FixedAsset
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.util.ThousandSeparatorVisualTransformation
import com.dony.bumdesku.viewmodel.FixedAssetViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedAssetListScreen(
    viewModel: FixedAssetViewModel,
    userRole: String,
    onAddAssetClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onNavigateUp: () -> Unit,
) {
    val assets by viewModel.filteredAssets.collectAsStateWithLifecycle()
    val allUnitUsaha by viewModel.allUnitUsaha.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedUnit by viewModel.selectedUnitFilter.collectAsStateWithLifecycle()
    var assetToDelete by remember { mutableStateOf<FixedAsset?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Aset Tetap") },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, "Kembali") } }
            )
        },
        floatingActionButton = {
            if (userRole != "auditor") {
                FloatingActionButton(onClick = onAddAssetClick) {
                    Icon(Icons.Default.Add, "Tambah Aset Tetap")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Filter Unit Usaha untuk Manager dan Auditor
            if (userRole == "manager" || userRole == "auditor") {
                var isExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = { isExpanded = !isExpanded },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedUnit?.name ?: "Semua Unit Usaha",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filter Unit Usaha") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Semua Unit Usaha") },
                            onClick = {
                                viewModel.selectUnitFilter(null)
                                isExpanded = false
                            }
                        )
                        allUnitUsaha.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.name) },
                                onClick = {
                                    viewModel.selectUnitFilter(unit)
                                    isExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (assets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada aset tetap yang dicatat.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(assets, key = { it.id }) { asset ->
                        val currentBookValue = viewModel.calculateCurrentBookValue(asset)
                        FixedAssetItem(
                            asset = asset,
                            currentBookValue = currentBookValue, // Kirim hasilnya ke Composable
                            onItemClick = { onItemClick(asset.id) },
                            onDeleteClick = { assetToDelete = asset },
                            userRole = userRole
                        )
                    }
                }
            }
        }
    }

    // AlertDialog untuk konfirmasi hapus
    assetToDelete?.let { asset ->
        AlertDialog(
            onDismissRequest = { assetToDelete = null },
            title = { Text("Konfirmasi Hapus") },
            text = { Text("Anda yakin ingin menghapus aset '${asset.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(asset)
                        assetToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Ya, Hapus") }
            },
            dismissButton = {
                Button(onClick = { assetToDelete = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun FixedAssetItem(
    asset: FixedAsset,
    currentBookValue: Double, // Tambahkan parameter ini
    userRole: String,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val localeID = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }
    val dateFormat = SimpleDateFormat("dd MMM yyyy", localeID)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = userRole != "auditor", onClick = onItemClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(asset.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Tgl Beli: ${dateFormat.format(Date(asset.purchaseDate))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Harga Beli: ${currencyFormat.format(asset.purchasePrice)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Nilai Buku Saat Ini: ${currencyFormat.format(currentBookValue)}", // Gunakan nilai yang sudah dihitung
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary // Beri warna berbeda agar menonjol
                )
            }
            if (userRole != "auditor") {
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.DeleteOutline, "Hapus Aset", tint = Color.Gray)
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFixedAssetScreen(
    assetToEdit: FixedAsset? = null,
    viewModel: FixedAssetViewModel,
    userRole: String,
    activeUnitUsaha: UnitUsaha?,
    onSaveComplete: () -> Unit,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val isEditMode = assetToEdit != null

    var name by remember { mutableStateOf(assetToEdit?.name ?: "") }
    var description by remember { mutableStateOf(assetToEdit?.description ?: "") }
    var purchasePrice by remember { mutableStateOf(assetToEdit?.purchasePrice?.toLong()?.toString() ?: "") }
    var usefulLife by remember { mutableStateOf(assetToEdit?.usefulLife?.toString() ?: "") }
    var residualValue by remember { mutableStateOf(assetToEdit?.residualValue?.toLong()?.toString() ?: "") }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = assetToEdit?.purchaseDate ?: System.currentTimeMillis())
    var showDatePicker by remember { mutableStateOf(false) }

    val allUnitUsaha by viewModel.allUnitUsaha.collectAsStateWithLifecycle(emptyList())
    var selectedUnitUsaha by remember { mutableStateOf<UnitUsaha?>(null) }
    var isUnitUsahaExpanded by remember { mutableStateOf(false) }

    // ✅ --- Logika untuk menampilkan Toast dipindahkan ke sini ---
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
    LaunchedEffect(uploadState) {
        when (uploadState) {
            FixedAssetUploadState.SUCCESS -> {
                Toast.makeText(context, if (isEditMode) "Aset berhasil diperbarui" else "Aset berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                viewModel.resetUploadState()
                onSaveComplete()
            }
            FixedAssetUploadState.ERROR -> {
                Toast.makeText(context, "Gagal menyimpan aset", Toast.LENGTH_SHORT).show()
                viewModel.resetUploadState()
            }
            else -> {}
        }
    }

    LaunchedEffect(activeUnitUsaha, assetToEdit, allUnitUsaha) {
        if (isEditMode && allUnitUsaha.isNotEmpty()) {
            selectedUnitUsaha = allUnitUsaha.find { it.id == assetToEdit?.unitUsahaId }
        } else if (userRole == "pengurus") {
            selectedUnitUsaha = activeUnitUsaha
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Aset Tetap" else "Tambah Aset Tetap") },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, "Kembali") } }
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
            if (userRole == "manager") {
                ExposedDropdownMenuBox(
                    expanded = isUnitUsahaExpanded,
                    onExpandedChange = { if (!isEditMode) isUnitUsahaExpanded = !isUnitUsahaExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedUnitUsaha?.name ?: "Pilih Unit Usaha",
                        onValueChange = {}, readOnly = true, label = { Text("Unit Usaha") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitUsahaExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = isUnitUsahaExpanded, onDismissRequest = { isUnitUsahaExpanded = false }) {
                        allUnitUsaha.forEach { unit ->
                            DropdownMenuItem(text = { Text(unit.name) }, onClick = {
                                selectedUnitUsaha = unit
                                isUnitUsahaExpanded = false
                            })
                        }
                    }
                }
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Aset (cth: Motor Supra)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Deskripsi (Opsional)") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(
                value = purchasePrice, onValueChange = { newValue -> purchasePrice = newValue.filter { it.isDigit() } },
                label = { Text("Harga Beli / Perolehan") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandSeparatorVisualTransformation(), modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = usefulLife, onValueChange = { newValue -> usefulLife = newValue.filter { it.isDigit() } },
                label = { Text("Masa Manfaat (Tahun)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = residualValue, onValueChange = { newValue -> residualValue = newValue.filter { it.isDigit() } },
                label = { Text("Nilai Residu / Sisa (Opsional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandSeparatorVisualTransformation(), modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                val selectedDate = dateState.selectedDateMillis?.let {
                    SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(it))
                } ?: "Pilih Tanggal Pembelian"
                Text(selectedDate)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val priceDouble = purchasePrice.toDoubleOrNull()
                    val lifeInt = usefulLife.toIntOrNull()
                    // --- TAMBAHKAN LOGIKA INI ---
                    val residualDouble = residualValue.toDoubleOrNull() ?: 0.0 // Default 0 jika kosong
                    // -----------------------------
                    val dateLong = dateState.selectedDateMillis
                    val unitId = selectedUnitUsaha?.id

                    if (name.isNotBlank() && priceDouble != null && lifeInt != null && dateLong != null && unitId != null) {
                        val assetData = (assetToEdit ?: FixedAsset()).copy(
                            name = name,
                            description = description,
                            purchasePrice = priceDouble,
                            usefulLife = lifeInt,
                            purchaseDate = dateLong,
                            unitUsahaId = unitId,
                            residualValue = residualDouble, // <-- Simpan nilai residu
                            bookValue = assetToEdit?.bookValue ?: priceDouble
                        )
                        if (isEditMode) {
                            viewModel.update(assetData)
                            onSaveComplete()
                        } else {
                            viewModel.insert(assetData)
                        }
                    } else {
                        Toast.makeText(context, "Harap isi semua kolom dengan benar", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                // ✅ --- Logika untuk menonaktifkan tombol dan menampilkan loading ---
                enabled = uploadState != FixedAssetUploadState.LOADING
            ) {
                if (uploadState == FixedAssetUploadState.LOADING) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Simpan")
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { Button(onClick = { showDatePicker = false }) { Text("OK") } }
        ) { DatePicker(state = dateState) }
    }
}