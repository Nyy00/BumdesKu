package com.dony.bumdesku.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockJournalScreen(
    onLockClick: (Long) -> Unit,
    onNavigateUp: () -> Unit
) {
    var endDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = endDateMillis)
    var showConfirmationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(dateState.selectedDateMillis) {
        dateState.selectedDateMillis?.let { endDateMillis = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kunci Jurnal Transaksi") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Pilih tanggal akhir periode yang akan dikunci. Semua transaksi hingga tanggal ini tidak akan bisa diubah atau dihapus lagi.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Kunci Hingga Tanggal: ${SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date(endDateMillis))}")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showConfirmationDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text("Kunci Jurnal Sekarang")
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { Button(onClick = { showDatePicker = false }) { Text("OK") } }
        ) { DatePicker(state = dateState) }
    }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text("Konfirmasi Penguncian") },
            text = { Text("Apakah Anda yakin? Proses ini tidak dapat dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        onLockClick(endDateMillis + 86400000 - 1) // Kunci hingga akhir hari
                        showConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Ya, Kunci") }
            },
            dismissButton = {
                Button(onClick = { showConfirmationDialog = false }) { Text("Batal") }
            }
        )
    }
}