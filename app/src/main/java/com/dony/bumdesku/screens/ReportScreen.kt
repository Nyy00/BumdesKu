package com.dony.bumdesku.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dony.bumdesku.data.DashboardData
import com.dony.bumdesku.data.ReportData
import com.dony.bumdesku.data.Transaction
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    reportData: ReportData,
    reportTransactions: List<Transaction>,
    userRole: String, // ✅ Tambahkan parameter ini
    onGenerateReport: (Long, Long) -> Unit,
    onNavigateUp: () -> Unit,
    onItemClick: (Transaction) -> Unit
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
                title = { Text("Laporan Keuangan") },
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showStartDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Text("Dari: ${simpleDateFormat.format(Date(startDateMillis))}")
                }
                Button(onClick = { showEndDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Text("Hingga: ${simpleDateFormat.format(Date(endDateMillis))}")
                }
            }

            Button(
                onClick = { onGenerateReport(startDateMillis, endDateMillis + 86400000 - 1) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tampilkan Laporan")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            if (reportData.isGenerated) {
                DashboardCard(
                    data = DashboardData(
                        totalIncome = reportData.totalIncome,
                        totalExpenses = reportData.totalExpenses,
                        finalBalance = reportData.netProfit
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(reportTransactions, key = { it.localId }) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            userRole = userRole, // ✅ Kirimkan peran ke TransactionItem
                            onItemClick = { if (userRole == "pengurus") onItemClick(transaction) },
                            onDeleteClick = {} // Di halaman laporan tidak ada fungsi hapus
                        )
                    }
                }
            } else {
                Text("Pilih rentang tanggal lalu klik 'Tampilkan Laporan'.")
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
}