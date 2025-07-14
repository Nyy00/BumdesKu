package com.dony.bumdesku.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitUsahaManagementScreen(
    unitUsahaList: List<UnitUsaha>,
    onAddUnitUsaha: (String) -> Unit,
    onDeleteUnitUsaha: (UnitUsaha) -> Unit,
    onNavigateUp: () -> Unit
) {
    var newUnitName by remember { mutableStateOf("") }
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newUnitName,
                    onValueChange = { newUnitName = it },
                    label = { Text("Nama Unit Usaha Baru") },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    if (newUnitName.isNotBlank()) {
                        onAddUnitUsaha(newUnitName)
                        newUnitName = "" // Kosongkan input field
                    } else {
                        Toast.makeText(context, "Nama unit tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Daftar Unit Usaha", style = MaterialTheme.typography.titleMedium)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Daftar unit usaha yang sudah ada
            if (unitUsahaList.isEmpty()) {
                Text("Belum ada unit usaha.")
            } else {
                LazyColumn {
                    items(unitUsahaList, key = { it.id }) { unit ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = unit.name, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onDeleteUnitUsaha(unit) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}