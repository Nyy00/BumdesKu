package com.dony.bumdesku.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dony.bumdesku.PdfExporter
import com.dony.bumdesku.data.Account
import com.dony.bumdesku.data.DashboardData
import com.dony.bumdesku.data.ReportData
import com.dony.bumdesku.data.Transaction
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.screens.TransactionItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    reportData: ReportData,
    reportTransactions: List<Transaction>,
    unitUsahaList: List<UnitUsaha>, // <-- Tambahkan parameter ini
    userRole: String,
    allAccounts: List<Account>,
    onGenerateReport: (Long, Long, UnitUsaha?) -> Unit, // <-- Ubah parameter ini
    onNavigateUp: () -> Unit
    // onItemClick kita hapus karena tidak digunakan di sini
) {
    val context = LocalContext.current
    val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    var startDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var endDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val startDateState = rememberDatePickerState(initialSelectedDateMillis = startDateMillis)
    val endDateState = rememberDatePickerState(initialSelectedDateMillis = endDateMillis)

    // State untuk dropdown filter
    var selectedUnitUsaha by remember { mutableStateOf<UnitUsaha?>(null) }
    var isUnitUsahaExpanded by remember { mutableStateOf(false) }


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
                },
                actions = {
                    IconButton(onClick = {
                        PdfExporter.createReportPdf(context, reportData, reportTransactions, allAccounts)
                    }) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Ekspor ke PDF"
                        )
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

            // Dropdown Filter Unit Usaha
            if (userRole == "manager" || userRole == "auditor") {
                ExposedDropdownMenuBox(
                    expanded = isUnitUsahaExpanded,
                    onExpandedChange = { isUnitUsahaExpanded = !isUnitUsahaExpanded },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedUnitUsaha?.name ?: "Semua Unit Usaha",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filter Unit Usaha") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitUsahaExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isUnitUsahaExpanded,
                        onDismissRequest = { isUnitUsahaExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Semua Unit Usaha") },
                            onClick = {
                                selectedUnitUsaha = null
                                isUnitUsahaExpanded = false
                            }
                        )
                        unitUsahaList.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.name) },
                                onClick = {
                                    selectedUnitUsaha = unit
                                    isUnitUsahaExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Tombol Generate Laporan
            Button(
                onClick = { onGenerateReport(startDateMillis, endDateMillis + 86400000 - 1, selectedUnitUsaha) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Tampilkan Laporan")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // Tampilan Hasil Laporan
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
                    items(reportTransactions, key = { it.id }) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            userRole = userRole,
                            onItemClick = { /* Tidak ada aksi klik di laporan */ },
                            onDeleteClick = {}
                        )
                    }
                }
            } else {
                Text("Pilih rentang tanggal dan filter, lalu klik 'Tampilkan Laporan'.")
            }
        }

        // Dialog Date Picker
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