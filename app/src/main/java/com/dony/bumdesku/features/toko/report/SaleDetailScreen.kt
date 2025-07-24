package com.dony.bumdesku.features.toko.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dony.bumdesku.data.CartItem
import com.dony.bumdesku.data.Sale
import com.dony.bumdesku.features.toko.formatCurrency
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailScreen(
    viewModel: SaleDetailViewModel,
    sale: Sale,
    onNavigateUp: () -> Unit
) {
    // Muat data sale ke ViewModel saat layar pertama kali dibuat
    LaunchedEffect(sale) {
        viewModel.loadSale(sale)
    }

    val cartItems by viewModel.cartItems.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Transaksi #${sale.id}") },
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
        ) {
            // Informasi Header
            Text("Tanggal: ${dateFormat.format(Date(sale.transactionDate))}")
            Spacer(modifier = Modifier.height(16.dp))
            Text("Item yang Dibeli:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Daftar Item
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(cartItems) { item ->
                    DetailItemRow(item = item)
                }
            }

            // Informasi Total
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Belanja", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    formatCurrency(sale.totalPrice),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun DetailItemRow(item: CartItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${item.quantity}x",
            modifier = Modifier.width(40.dp),
            fontWeight = FontWeight.Bold
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(item.asset.name)
            Text(formatCurrency(item.asset.sellingPrice), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Text(formatCurrency(item.asset.sellingPrice * item.quantity))
    }
}