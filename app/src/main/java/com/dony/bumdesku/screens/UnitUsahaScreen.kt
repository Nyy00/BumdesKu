package com.dony.bumdesku.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.data.UnitUsahaType // <-- Import baru

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitUsahaManagementScreen(
    unitUsahaList: List<UnitUsaha>,
    // Lambda diperbarui untuk menerima tipe unit usaha
    onAddUnitUsaha: (String, UnitUsahaType) -> Unit,
    onDeleteUnitUsaha: (UnitUsaha) -> Unit,
    onNavigateUp: () -> Unit
) {
    var newUnitName by remember { mutableStateOf("") }
    // State baru untuk dropdown
    var isTypeExpanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(UnitUsahaType.UMUM) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manajemen Unit Usaha") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali")
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
            // Form untuk menambah unit usaha baru
            OutlinedTextField(
                value = newUnitName,
                onValueChange = { newUnitName = it },
                label = { Text("Nama Unit Usaha Baru") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Dropdown untuk memilih Jenis Unit Usaha
            ExposedDropdownMenuBox(
                expanded = isTypeExpanded,
                onExpandedChange = { isTypeExpanded = !isTypeExpanded }
            ) {
                OutlinedTextField(
                    value = selectedType.name.replace("_", " "),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Jenis Unit Usaha") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isTypeExpanded,
                    onDismissRequest = { isTypeExpanded = false }
                ) {
                    // Ambil semua nilai dari enum UnitUsahaType
                    UnitUsahaType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name.replace("_", " ")) },
                            onClick = {
                                selectedType = type
                                isTypeExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (newUnitName.isNotBlank()) {
                        // Kirim nama dan tipe yang dipilih
                        onAddUnitUsaha(newUnitName, selectedType)
                        newUnitName = "" // Kosongkan input field
                        selectedType = UnitUsahaType.UMUM // Reset pilihan
                    } else {
                        Toast.makeText(context, "Nama unit tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    }
                }) {
                Text("Tambah Unit Usaha")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Daftar Unit Usaha", style = MaterialTheme.typography.titleMedium)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Daftar unit usaha yang sudah ada
            if (unitUsahaList.isEmpty()) {
                Text("Belum ada unit usaha.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(unitUsahaList, key = { it.localId }) { unit ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tampilkan nama dan jenis unit usaha
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = unit.name, style = MaterialTheme.typography.bodyLarge)
                                Text(text = unit.type.name.replace("_", " "), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            IconButton(onClick = { onDeleteUnitUsaha(unit) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Gray)
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }
}