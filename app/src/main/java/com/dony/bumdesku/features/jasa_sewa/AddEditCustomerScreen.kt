package com.dony.bumdesku.features.jasa_sewa

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dony.bumdesku.data.Customer
import com.dony.bumdesku.viewmodel.RentalSaveState
import com.dony.bumdesku.viewmodel.RentalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCustomerScreen(
    viewModel: RentalViewModel,
    customerId: String?,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEditMode = !customerId.isNullOrBlank()
    val customerToEdit = remember(customerId) {
        if (isEditMode) uiState.customers.find { it.id == customerId } else null
    }

    var name by remember { mutableStateOf(customerToEdit?.name ?: "") }
    var phone by remember { mutableStateOf(customerToEdit?.phone ?: "") }
    var address by remember { mutableStateOf(customerToEdit?.address ?: "") }
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(saveState) {
        when (saveState) {
            RentalSaveState.SUCCESS -> {
                Toast.makeText(context, "Pelanggan berhasil disimpan", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
                onNavigateUp()
            }
            is RentalSaveState.ERROR -> {
                Toast.makeText(context, "Gagal menyimpan pelanggan", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Pelanggan" else "Tambah Pelanggan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Pelanggan") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Nomor Telepon") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Alamat") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val customer = (customerToEdit ?: Customer()).copy(
                        name = name,
                        phone = phone,
                        address = address,
                    )
                    viewModel.saveCustomer(customer)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = saveState != RentalSaveState.LOADING
            ) {
                if(saveState == RentalSaveState.LOADING) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Simpan")
                }
            }
        }
    }
}