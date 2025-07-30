package com.dony.bumdesku.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dony.bumdesku.data.NeracaSaldoItem
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeracaSaldoScreen(
    items: List<NeracaSaldoItem>,
    onNavigateUp: () -> Unit
) {
    val totalDebit = items.sumOf { it.totalDebit }
    val totalKredit = items.sumOf { it.totalKredit }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Neraca Saldo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Akun", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
                Text("Debit", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
                Text("Kredit", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
            }
            Divider()

            // Daftar Akun
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items, key = { it.accountId }) { item -> // Kunci sekarang unik
                    NeracaSaldoItemRow(item = item)
                    Divider()
                }
            }

            // Footer Total
            Divider(thickness = 2.dp)
            NeracaSaldoTotalRow(totalDebit = totalDebit, totalKredit = totalKredit)
        }
    }
}

@Composable
fun NeracaSaldoItemRow(item: NeracaSaldoItem) {
    val localeID = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.weight(2f)) {
            Text(item.accountNumber, style = MaterialTheme.typography.labelSmall)
            Text(item.accountName, style = MaterialTheme.typography.bodyMedium)
        }
        Text(currencyFormat.format(item.totalDebit), modifier = Modifier.weight(1.5f))
        Text(currencyFormat.format(item.totalKredit), modifier = Modifier.weight(1.5f))
    }
}

@Composable
fun NeracaSaldoTotalRow(totalDebit: Double, totalKredit: Double) {
    val localeID = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("TOTAL", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(currencyFormat.format(totalDebit), modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(currencyFormat.format(totalKredit), modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}