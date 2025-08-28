package com.dony.bumdesku.features.jasa_sewa

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dony.bumdesku.data.RentalTransaction
import com.dony.bumdesku.screens.EmptyState // Pastikan Anda mengimpor EmptyState
import com.dony.bumdesku.viewmodel.RentalViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalHistoryScreen(
    viewModel: RentalViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ✅ 1. State untuk menyimpan query pencarian dan status aktif/tidak aktif
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // ✅ 2. Logika untuk memfilter daftar transaksi
    val filteredTransactions = remember(searchQuery, uiState.completedTransactions) {
        if (searchQuery.isBlank()) {
            uiState.completedTransactions
        } else {
            uiState.completedTransactions.filter { transaction ->
                transaction.customerName.contains(searchQuery, ignoreCase = true) ||
                        transaction.itemName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Otomatis fokus ke kolom pencarian saat diaktifkan
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                // ✅ 3. Tampilan TopAppBar yang dinamis
                title = {
                    AnimatedVisibility(visible = !isSearchActive, enter = fadeIn(), exit = fadeOut()) {
                        Text("Riwayat Transaksi Sewa")
                    }
                    AnimatedVisibility(visible = isSearchActive, enter = fadeIn(), exit = fadeOut()) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Cari pelanggan atau barang...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) {
                            searchQuery = "" // Reset query saat pencarian ditutup
                        }
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Pencarian"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            // ✅ 4. Menampilkan hasil filter atau pesan "kosong" yang lebih cerdas
            filteredTransactions.isEmpty() -> {
                EmptyState(
                    message = if (searchQuery.isBlank()) "Belum ada transaksi yang sudah selesai." else "Tidak ada hasil untuk \"$searchQuery\""
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { transaction ->
                        CompletedRentalTransactionView(
                            transaction = transaction,
                            onClick = { onNavigateToDetail(transaction.id) }
                        )
                    }
                }
            }
        }
    }
}

// Composable untuk menampilkan detail transaksi selesai
@Composable
fun CompletedRentalTransactionView(
    transaction: RentalTransaction,
    onClick: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(transaction.id) },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Penyewa: ${transaction.customerName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Barang: ${transaction.itemName} (x${transaction.quantity})",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Selesai pada: ${transaction.returnDate?.let { dateFormat.format(Date(it)) } ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Total Biaya: ${NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(transaction.totalPrice)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            if (transaction.notesOnReturn.isNotBlank()) {
                Text(
                    "Catatan: ${transaction.notesOnReturn}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}