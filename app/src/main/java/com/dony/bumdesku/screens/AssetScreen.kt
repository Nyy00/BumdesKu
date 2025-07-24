package com.dony.bumdesku.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.dony.bumdesku.data.Asset
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.util.ThousandSeparatorVisualTransformation
import com.dony.bumdesku.viewmodel.AssetViewModel
import com.dony.bumdesku.viewmodel.UploadState
import java.text.NumberFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetListScreen(
    viewModel: AssetViewModel,
    userRole: String,
    onAddAssetClick: () -> Unit,
    onItemClick: (Asset) -> Unit,
    onNavigateUp: () -> Unit,
    onDeleteClick: (Asset) -> Unit
) {
    val assets by viewModel.filteredAssets.collectAsStateWithLifecycle()
    val allUnitUsaha by viewModel.allUnitUsaha.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedUnit by viewModel.selectedUnitFilter.collectAsStateWithLifecycle()
    var assetToDelete by remember { mutableStateOf<Asset?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Aset & Inventaris") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAssetClick) {
                Icon(Icons.Default.Add, "Tambah Aset")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {

            if (userRole == "manager") {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false }
                    ) {
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (selectedUnit != null) "Tidak ada aset untuk unit ini." else "Belum ada aset yang ditambahkan.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ✅ UBAH KEY DARI localId MENJADI id
                    items(assets, key = { it.id }) { asset ->
                        AssetItem(
                            asset = asset,
                            // ✅ UBAH PARAMETER DARI asset.localId MENJADI asset.id
                            onItemClick = { onItemClick(asset) },
                            onDeleteClick = { assetToDelete = asset }
                        )
                    }
                }
            }
        }
    }

    assetToDelete?.let { asset ->
        AlertDialog(
            onDismissRequest = { assetToDelete = null },
            title = { Text("Konfirmasi Hapus") },
            text = { Text("Apakah Anda yakin ingin menghapus aset '${asset.name}'? Stok akan hilang permanen.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteClick(asset)
                        assetToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ya, Hapus")
                }
            },
            dismissButton = {
                Button(onClick = { assetToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun AssetItem(
    asset: Asset,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val localeID = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (asset.imageUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(asset.imageUrl),
                    contentDescription = asset.name,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(asset.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Stok: ${asset.quantity}", style = MaterialTheme.typography.bodyMedium)

                Text(
                    "Harga Jual: ${currencyFormat.format(asset.sellingPrice)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Modal: ${currencyFormat.format(asset.purchasePrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.DeleteOutline, "Hapus Aset", tint = Color.Gray)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAssetScreen(
    assetToEdit: Asset? = null,
    viewModel: AssetViewModel,
    userRole: String,
    activeUnitUsaha: UnitUsaha?,
    onSaveComplete: () -> Unit,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val isEditMode = assetToEdit != null

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var category by remember { mutableStateOf("") }
    var isCategoryExpanded by remember { mutableStateOf(false) }

    val defaultCategories = listOf(
        "Minuman", "Makanan Ringan", "Bumbu Dapur", "Mie Instan",
        "Kebutuhan Mandi", "Kebutuhan Cuci", "ATK", "Rokok", "Lain-lain"
    )
    val customCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val allCategoryOptions = (defaultCategories + customCategories).distinct().sorted()

    val allUnitUsaha by viewModel.allUnitUsaha.collectAsStateWithLifecycle(emptyList())
    var selectedUnitUsaha by remember { mutableStateOf<UnitUsaha?>(null) }
    var isUnitUsahaExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(activeUnitUsaha, assetToEdit, allUnitUsaha) {
        if (isEditMode) {
            selectedUnitUsaha = allUnitUsaha.find { it.id == assetToEdit?.unitUsahaId }
        } else if (userRole == "pengurus") {
            selectedUnitUsaha = activeUnitUsaha
        }
    }

    LaunchedEffect(assetToEdit) {
        if (isEditMode) {
            name = assetToEdit?.name ?: ""
            description = assetToEdit?.description ?: ""
            quantity = assetToEdit?.quantity?.toString() ?: ""
            purchasePrice = assetToEdit?.purchasePrice?.toLong()?.toString() ?: ""
            sellingPrice = assetToEdit?.sellingPrice?.toLong()?.toString() ?: ""
            category = assetToEdit?.category ?: "Lain-lain"
        }
    }

    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    LaunchedEffect(uploadState) {
        if (uploadState == UploadState.SUCCESS) {
            Toast.makeText(context, "Aset berhasil disimpan", Toast.LENGTH_SHORT).show()
            viewModel.resetUploadState()
            onSaveComplete()
        } else if (uploadState == UploadState.ERROR) {
            Toast.makeText(context, "Gagal menyimpan aset", Toast.LENGTH_SHORT).show()
            viewModel.resetUploadState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Aset" else "Tambah Aset Baru") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp, enabled = uploadState != UploadState.UPLOADING) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                        onExpandedChange = { isUnitUsahaExpanded = !isUnitUsahaExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedUnitUsaha?.name ?: "Pilih Unit Usaha",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit Usaha") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitUsahaExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = isUnitUsahaExpanded,
                            onDismissRequest = { isUnitUsahaExpanded = false }
                        ) {
                            allUnitUsaha.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.name) },
                                    onClick = {
                                        selectedUnitUsaha = unit
                                        isUnitUsahaExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk / Barang") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = isCategoryExpanded,
                    onExpandedChange = { isCategoryExpanded = !isCategoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategori Produk") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryExpanded,
                        onDismissRequest = { isCategoryExpanded = false }
                    ) {
                        allCategoryOptions.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    category = selectionOption
                                    isCategoryExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Jumlah / Stok") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = purchasePrice,
                    onValueChange = { newValue -> purchasePrice = newValue.filter { it.isDigit() } },
                    label = { Text("Harga Beli Satuan (Modal)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandSeparatorVisualTransformation()
                )

                OutlinedTextField(
                    value = sellingPrice,
                    onValueChange = { newValue -> sellingPrice = newValue.filter { it.isDigit() } },
                    label = { Text("Harga Jual Satuan") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandSeparatorVisualTransformation()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi (Opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Text("Pilih Gambar")
                    }
                    imageUri?.let {
                        Image(
                            painter = rememberAsyncImagePainter(it),
                            contentDescription = "Preview Gambar Aset",
                            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val quantityInt = quantity.toIntOrNull()
                        val priceDouble = purchasePrice.toDoubleOrNull()
                        val sellingPriceDouble = sellingPrice.toDoubleOrNull()
                        val finalUnitUsahaId = selectedUnitUsaha?.id

                        if (name.isNotBlank() && finalUnitUsahaId != null && quantityInt != null && priceDouble != null && sellingPriceDouble != null) {
                            val finalCategory = category.trim().ifBlank { "Lain-lain" }
                            if (isEditMode) {
                                val updatedAsset = assetToEdit!!.copy(
                                    name = name,
                                    unitUsahaId = finalUnitUsahaId,
                                    quantity = quantityInt,
                                    purchasePrice = priceDouble,
                                    sellingPrice = sellingPriceDouble,
                                    description = description,
                                    category = finalCategory
                                )
                                viewModel.update(updatedAsset)
                                Toast.makeText(context, "Aset berhasil diperbarui", Toast.LENGTH_SHORT).show()
                                onSaveComplete()
                            } else {
                                val newAsset = Asset(
                                    name = name,
                                    unitUsahaId = finalUnitUsahaId,
                                    quantity = quantityInt,
                                    purchasePrice = priceDouble,
                                    sellingPrice = sellingPriceDouble,
                                    description = description,
                                    category = finalCategory
                                )
                                viewModel.insert(newAsset, imageUri)
                            }
                        } else {
                            Toast.makeText(context, "Harap isi semua kolom dengan benar, termasuk Unit Usaha", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uploadState != UploadState.UPLOADING
                ) {
                    Text("Simpan")
                }
            }
            if (uploadState == UploadState.UPLOADING) {
                CircularProgressIndicator()
            }
        }
    }
}