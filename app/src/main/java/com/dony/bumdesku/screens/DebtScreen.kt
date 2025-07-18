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
import com.dony.bumdesku.data.Payable
import com.dony.bumdesku.data.Receivable
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// --- Layar Daftar Utang (Payable) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayableListScreen(
    payables: List<Payable>,
    onAddItemClick: () -> Unit,
    onNavigateUp: () -> Unit,
    onMarkAsPaid: (Payable) -> Unit // ✅ Tambahkan parameter ini
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Utang Usaha") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItemClick) {
                Icon(Icons.Default.Add, "Tambah Utang")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(payables, key = { it.localId }) { payable ->
                DebtItemCard(
                    contactName = payable.contactName,
                    description = payable.description,
                    amount = payable.amount,
                    dueDate = payable.dueDate,
                    isPaid = payable.isPaid,
                    onMarkAsPaid = { onMarkAsPaid(payable) } // ✅ Panggil aksi
                )
            }
        }
    }
}

// --- Layar Daftar Piutang (Receivable) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivableListScreen(
    receivables: List<Receivable>,
    onAddItemClick: () -> Unit,
    onNavigateUp: () -> Unit,
    onMarkAsPaid: (Receivable) -> Unit // ✅ Tambahkan parameter ini
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Piutang Usaha") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItemClick) {
                Icon(Icons.Default.Add, "Tambah Piutang")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(receivables, key = { it.localId }) { receivable ->
                DebtItemCard(
                    contactName = receivable.contactName,
                    description = receivable.description,
                    amount = receivable.amount,
                    dueDate = receivable.dueDate,
                    isPaid = receivable.isPaid,
                    onMarkAsPaid = { onMarkAsPaid(receivable) } // ✅ Panggil aksi
                )
            }
        }
    }
}

// --- Composable Umum untuk Menampilkan Item Utang/Piutang ---
@Composable
fun DebtItemCard(
    contactName: String,
    description: String,
    amount: Double,
    dueDate: Long,
    isPaid: Boolean,
    onMarkAsPaid: () -> Unit
) {
    val localeID = Locale("in", "ID")
    val currencyFormat =
        NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }
    val dateFormat = SimpleDateFormat("dd MMM yyyy", localeID)
    val isOverdue = System.currentTimeMillis() > dueDate && !isPaid

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaid) Color.LightGray else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                contactName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        currencyFormat.format(amount),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isPaid) "LUNAS" else "Jatuh Tempo: ${dateFormat.format(Date(dueDate))}",
                        color = if (isOverdue) MaterialTheme.colorScheme.error else Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                // Tampilkan tombol hanya jika belum lunas
                if (!isPaid) {
                    Button(onClick = onMarkAsPaid) {
                        Text("Lunas")
                    }
                }
            }
        }
    }
}


// --- Layar untuk Menambah Utang Baru ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPayableScreen(
    onSave: (Payable) -> Unit,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    var contactName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = dueDateMillis)

    LaunchedEffect(dateState.selectedDateMillis) {
        dateState.selectedDateMillis?.let { dueDateMillis = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Tambah Utang Baru") }, navigationIcon = {
                IconButton(onClick = onNavigateUp) {
                    Icon(
                        Icons.Default.ArrowBack,
                        "Kembali"
                    )
                }
            })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = contactName,
                onValueChange = { contactName = it },
                label = { Text("Nama Kreditor (Pemberi Utang)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi Utang") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Jumlah Utang") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Jatuh Tempo: ${
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(
                            Date(dueDateMillis)
                        )
                    }"
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    if (contactName.isNotBlank() && description.isNotBlank() && amountDouble != null) {
                        onSave(
                            Payable(
                                contactName = contactName,
                                description = description,
                                amount = amountDouble,
                                transactionDate = System.currentTimeMillis(),
                                dueDate = dueDateMillis
                            )
                        )
                    } else {
                        Toast.makeText(context, "Harap isi semua kolom", Toast.LENGTH_SHORT)
                            .show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Simpan Utang") }
        }
    }
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { Button(onClick = { showDatePicker = false }) { Text("OK") } }
        ) { DatePicker(state = dateState) }
    }
}

// --- Layar untuk Menambah Piutang Baru ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReceivableScreen(
    onSave: (Receivable) -> Unit,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    var contactName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = dueDateMillis)

    LaunchedEffect(dateState.selectedDateMillis) {
        dateState.selectedDateMillis?.let { dueDateMillis = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Tambah Piutang Baru") }, navigationIcon = {
                IconButton(onClick = onNavigateUp) {
                    Icon(
                        Icons.Default.ArrowBack,
                        "Kembali"
                    )
                }
            })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = contactName,
                onValueChange = { contactName = it },
                label = { Text("Nama Debitor (Yang Berutang)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi Piutang") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Jumlah Piutang") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Jatuh Tempo: ${
                        SimpleDateFormat(
                            "dd MMM yyyy",
                            Locale.getDefault()
                        ).format(Date(dueDateMillis))
                    }"
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    if (contactName.isNotBlank() && description.isNotBlank() && amountDouble != null) {
                        onSave(
                            Receivable(
                                contactName = contactName,
                                description = description,
                                amount = amountDouble,
                                transactionDate = System.currentTimeMillis(),
                                dueDate = dueDateMillis
                            )
                        )
                    } else {
                        Toast.makeText(context, "Harap isi semua kolom", Toast.LENGTH_SHORT)
                            .show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Simpan Piutang") }
        }
    }
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { Button(onClick = { showDatePicker = false }) { Text("OK") } }
        ) { DatePicker(state = dateState) }
    }
}