package com.dony.bumdesku.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dony.bumdesku.data.NeracaData
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
            // SISI KIRI (ASET)
            Text("ASET", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            NeracaRowItem("Total Aset", neracaData.totalAset, currencyFormat)

            Spacer(modifier = Modifier.height(24.dp))

            // SISI KANAN (KEWAJIBAN & MODAL)
            Text("KEWAJIBAN & MODAL", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            NeracaRowItem("Total Kewajiban", neracaData.totalKewajiban, currencyFormat)
            NeracaRowItem("Total Modal", neracaData.totalModal, currencyFormat)
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

@Composable
fun NeracaRowItem(label: String, value: Double, format: NumberFormat, isTotal: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isTotal) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = format.format(value),
            style = if (isTotal) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
    }
}