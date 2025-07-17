package com.dony.bumdesku.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dony.bumdesku.data.NeracaData
import com.dony.bumdesku.data.NeracaItem
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeracaScreen(
    neracaData: NeracaData,
    onNavigateUp: () -> Unit
) {
    val localeID = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Posisi Keuangan (Neraca)") },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, "Kembali") } }
            )
        }
    ) { paddingValues ->
        // Gunakan LazyColumn agar bisa di-scroll
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            // SISI ASET
            item {
                Text("ASET", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Divider(modifier = Modifier.padding(vertical = 4.dp))
            }
            items(neracaData.asetItems) { item ->
                NeracaRowItem(label = item.accountName, value = item.balance, format = currencyFormat)
            }
            item {
                Divider(modifier = Modifier.padding(top = 4.dp))
                NeracaRowItem("Total Aset", neracaData.totalAset, currencyFormat, isTotal = true)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // SISI KEWAJIBAN & MODAL
            item {
                Text("KEWAJIBAN & MODAL", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Divider(modifier = Modifier.padding(vertical = 4.dp))
            }
            // Rincian Kewajiban
            item { Text("Kewajiban", style = MaterialTheme.typography.titleMedium) }
            items(neracaData.kewajibanItems) { item ->
                NeracaRowItem(label = item.accountName, value = item.balance, format = currencyFormat)
            }
            item { NeracaRowItem("Total Kewajiban", neracaData.totalKewajiban, currencyFormat, isSubTotal = true) }
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Rincian Modal
            item { Text("Modal", style = MaterialTheme.typography.titleMedium) }
            items(neracaData.modalItems) { item ->
                NeracaRowItem(label = item.accountName, value = item.balance, format = currencyFormat)
            }
            item { NeracaRowItem("Total Modal", neracaData.totalModal, currencyFormat, isSubTotal = true) }
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Total Keseluruhan
            item {
                Divider(modifier = Modifier.padding(top = 4.dp))
                NeracaRowItem(
                    label = "Total Kewajiban + Modal",
                    value = neracaData.totalKewajiban + neracaData.totalModal,
                    format = currencyFormat,
                    isTotal = true
                )
            }
        }
    }
}

@Composable
fun NeracaRowItem(
    label: String,
    value: Double,
    format: NumberFormat,
    isTotal: Boolean = false,
    isSubTotal: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = if (isSubTotal || isTotal) 0.dp else 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isTotal) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal || isSubTotal) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = format.format(value),
            style = if (isTotal) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal || isSubTotal) FontWeight.Bold else FontWeight.Normal
        )
    }
}