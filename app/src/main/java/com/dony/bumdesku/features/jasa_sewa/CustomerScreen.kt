package com.dony.bumdesku.features.jasa_sewa

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dony.bumdesku.data.Customer
import com.dony.bumdesku.viewmodel.RentalSaveState
import com.dony.bumdesku.viewmodel.RentalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    viewModel: RentalViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToAddEditCustomer: (String?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }

    LaunchedEffect(saveState) {
        when (saveState) {
            RentalSaveState.SUCCESS -> {
                Toast.makeText(context, "Operasi berhasil", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
            }
            is RentalSaveState.ERROR -> {
                Toast.makeText(context, "Operasi gagal", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manajemen Pelanggan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToAddEditCustomer(null) }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Pelanggan")
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            if (uiState.customers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("Belum ada data pelanggan.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.customers) { customer ->
                        CustomerCard(
                            customer = customer,
                            onEdit = { onNavigateToAddEditCustomer(customer.id) },
                            onDelete = { customerToDelete = customer }
                        )
                    }
                }
            }
        }
    }

    customerToDelete?.let { customer ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("Konfirmasi Hapus") },
            text = { Text("Anda yakin ingin menghapus pelanggan '${customer.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(customer)
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ya, Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun CustomerCard(customer: Customer, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(customer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (customer.phone.isNotBlank()) {
                    Text("Telp: ${customer.phone}", style = MaterialTheme.typography.bodySmall)
                }
                if (customer.address.isNotBlank()) {
                    Text("Alamat: ${customer.address}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus")
                }
            }
        }
    }
}