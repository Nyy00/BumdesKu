package com.dony.bumdesku.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dony.bumdesku.data.LpeData
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LpeScreen(
    lpeData: LpeData,
    onGenerateLpe: (Long, Long) -> Unit,
    onNavigateUp: () -> Unit
) {
    val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var startDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var endDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val startDateState = rememberDatePickerState(initialSelectedDateMillis = startDateMillis)
    val endDateState = rememberDatePickerState(initialSelectedDateMillis = endDateMillis)

    LaunchedEffect(startDateState.selectedDateMillis) {
        startDateState.selectedDateMillis?.let { startDateMillis = it }
    }
    LaunchedEffect(endDateState.selectedDateMillis) {
        endDateState.selectedDateMillis?.let { endDateMillis = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Perubahan Ekuitas") },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pemilih Tanggal
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showStartDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Text("Dari: ${simpleDateFormat.format(Date(startDateMillis))}")
                }
                Button(onClick = { showEndDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Text("Hingga: ${simpleDateFormat.format(Date(endDateMillis))}")
                }
            }
            Button(
                onClick = { onGenerateLpe(startDateMillis, endDateMillis + 86400000 - 1) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Tampilkan Laporan")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Divider()

            // Tampilan Laporan
            if (lpeData.isGenerated) {
                LpeReportContent(data = lpeData)
            } else {
                Text("Pilih rentang tanggal lalu klik 'Tampilkan Laporan'.", modifier = Modifier.padding(top=16.dp))
            }
        }
    }

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = { Button(onClick = { showStartDatePicker = false }) { Text("OK") } }
        ) { DatePicker(state = startDateState) }
    }
    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = { Button(onClick = { showEndDatePicker = false }) { Text("OK") } }
        ) { DatePicker(state = endDateState) }
    }
}

@Composable
fun LpeReportContent(data: LpeData) {
    val localeID = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }
    Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LpeRowItem("Modal Awal Periode", data.modalAwal, currencyFormat)
        LpeRowItem("Laba Bersih Periode Ini", data.labaBersih, currencyFormat)
        LpeRowItem("Prive (Penarikan Modal)", data.prive, currencyFormat, isNegative = true)
        Divider(modifier = Modifier.padding(vertical = 4.dp))
        LpeRowItem("Modal Akhir Periode", data.modalAkhir, currencyFormat, isTotal = true)
    }
}

@Composable
fun LpeRowItem(label: String, value: Double, format: NumberFormat, isTotal: Boolean = false, isNegative: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            modifier = if (isNegative) Modifier.padding(start = 16.dp) else Modifier,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = format.format(value),
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
    }
}