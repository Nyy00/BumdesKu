package com.dony.bumdesku.screens

import android.widget.Toast
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
import com.dony.bumdesku.data.Asset
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetListScreen(
    assets: List<Asset>,
    onAddAssetClick: () -> Unit,
    onNavigateUp: () -> Unit,
    onDeleteClick: (Asset) -> Unit
) {
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
        if (assets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Belum ada aset yang ditambahkan.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(assets, key = { it.localId }) { asset ->
                    AssetItem(asset = asset, onDeleteClick = { onDeleteClick(asset) })
                }
            }
        }
    }
}

@Composable
fun AssetItem(asset: Asset, onDeleteClick: () -> Unit) {
    val localeID = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(asset.name, style = MaterialTheme.typography.titleMedium)
                Text("Jumlah: ${asset.quantity}", style = MaterialTheme.typography.bodyMedium)
                Text("Harga: ${currencyFormat.format(asset.purchasePrice)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
    onSave: (Asset) -> Unit,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Aset Baru") },
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
                label = { Text("Nama Aset / Barang") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Jumlah / Stok") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = purchasePrice,
                onValueChange = { purchasePrice = it },
                label = { Text("Harga Beli Satuan") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi (Opsional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // TODO: Tambahkan tombol upload gambar di sini

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val quantityInt = quantity.toIntOrNull()
                    val priceDouble = purchasePrice.toDoubleOrNull()
                    if (name.isNotBlank() && quantityInt != null && priceDouble != null) {
                        val newAsset = Asset(
                            name = name,
                            quantity = quantityInt,
                            purchasePrice = priceDouble,
                            description = description
                        )
                        onSave(newAsset)
                    } else {
                        Toast.makeText(context, "Harap isi Nama, Jumlah, dan Harga dengan benar", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan")
            }
        }
    }
}