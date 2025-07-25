package com.dony.bumdesku.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.dony.bumdesku.util.ThousandSeparatorVisualTransformation
import androidx.compose.ui.unit.dp
import com.dony.bumdesku.data.*
import com.dony.bumdesku.viewmodel.ChartData
import com.dony.bumdesku.viewmodel.TransactionViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    transactions: List<Transaction>,
    dashboardData: DashboardData,
    chartData: ChartData,
    userRole: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddItemClick: () -> Unit,
    onItemClick: (Transaction) -> Unit,
    onDeleteClick: (Transaction) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToUnitUsaha: () -> Unit,
    onNavigateToReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Cari deskripsi...") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (searchQuery.isNotEmpty()) {
                                        onSearchQueryChange("")
                                    } else {
                                        isSearchActive = false
                                    }
                                }) {
                                    Icon(Icons.Default.Close, "Hapus atau Tutup")
                                }
                            }
                        )
                    } else {
                        Text("Buku Besar & Jurnal")
                    }
                },
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = {
                            isSearchActive = false
                            onSearchQueryChange("")
                        }) {
                            Icon(Icons.Default.ArrowBack, "Tutup Pencarian")
                        }
                    } else {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.Default.ArrowBack, "Kembali")
                        }
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, "Cari")
                        }
                        IconButton(onClick = onNavigateToUnitUsaha) {
                            Icon(Icons.Default.Store, "Manajemen Unit Usaha")
                        }
                        IconButton(onClick = onNavigateToReport) {
                            Icon(Icons.Default.Assessment, "Halaman Laporan")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (userRole == "pengurus" && !isSearchActive) {
                FloatingActionButton(onClick = onAddItemClick) {
                    Icon(Icons.Default.Add, "Input Jurnal")
                }
            }
        }
    ) { paddingValues ->
        if (transactionToDelete != null) {
            AlertDialog(
                onDismissRequest = { transactionToDelete = null },
                title = { Text("Konfirmasi Hapus") },
                text = { Text("Apakah Anda yakin ingin menghapus jurnal '${transactionToDelete?.description}'?") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteClick(transactionToDelete!!)
                            transactionToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Hapus") }
                },
                dismissButton = {
                    Button(onClick = { transactionToDelete = null }) { Text("Batal") }
                }
            )
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!isSearchActive) {
                DashboardCard(data = dashboardData)
                MonthlyBarChart(chartData = chartData)
            }

            if (transactions.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(if (searchQuery.isNotEmpty()) "Jurnal tidak ditemukan." else "Belum ada jurnal.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions, key = { it.id }) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            userRole = userRole,
                            onItemClick = { onItemClick(transaction) },
                            onDeleteClick = { transactionToDelete = transaction }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    transactionToEdit: Transaction? = null,
    onSave: (Transaction) -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: TransactionViewModel
) {
    val allAccounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val unitUsahaList by viewModel.allUnitUsaha.collectAsState(initial = emptyList())

    val context = LocalContext.current

    var description by remember { mutableStateOf(transactionToEdit?.description ?: "") }
    var amount by remember { mutableStateOf(transactionToEdit?.amount?.toLong()?.toString() ?: "") }
    var selectedUnitUsaha by remember { mutableStateOf<UnitUsaha?>(null) }
    var isUnitUsahaExpanded by remember { mutableStateOf(false) }
    var debitAccount by remember { mutableStateOf<Account?>(null) }
    var isDebitExpanded by remember { mutableStateOf(false) }
    var creditAccount by remember { mutableStateOf<Account?>(null) }
    var isCreditExpanded by remember { mutableStateOf(false) }
    val screenTitle = if (transactionToEdit == null) "Input Jurnal Umum" else "Edit Jurnal"

    LaunchedEffect(key1 = transactionToEdit, key2 = allAccounts, key3 = unitUsahaList) {
        if (transactionToEdit != null && allAccounts.isNotEmpty()) {
            debitAccount = allAccounts.find { it.id == transactionToEdit.debitAccountId }
            creditAccount = allAccounts.find { it.id == transactionToEdit.creditAccountId }
            if (transactionToEdit.unitUsahaId.isNotBlank() && unitUsahaList.isNotEmpty()) {
                selectedUnitUsaha = unitUsahaList.find { it.id == transactionToEdit.unitUsahaId }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(screenTitle) }, navigationIcon = {
            IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, "Kembali") }
        }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Deskripsi Transaksi") }, modifier = Modifier.fillMaxWidth())

            // ✅ PERBAIKAN DITERAPKAN DI SINI
            OutlinedTextField(
                value = amount,
                onValueChange = { newValue -> amount = newValue.filter { it.isDigit() } },
                label = { Text("Nominal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandSeparatorVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(expanded = isDebitExpanded, onExpandedChange = { isDebitExpanded = !isDebitExpanded }) {
                OutlinedTextField(value = debitAccount?.let { "${it.accountNumber} - ${it.accountName}" } ?: "Pilih Akun Debit", onValueChange = {}, readOnly = true, label = { Text("Akun Debit") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDebitExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded = isDebitExpanded, onDismissRequest = { isDebitExpanded = false }) {
                    allAccounts.forEach { account ->
                        DropdownMenuItem(text = { Text("${account.accountNumber} - ${account.accountName}") }, onClick = {
                            debitAccount = account
                            isDebitExpanded = false
                        })
                    }
                }
            }

            ExposedDropdownMenuBox(expanded = isCreditExpanded, onExpandedChange = { isCreditExpanded = !isCreditExpanded }) {
                OutlinedTextField(value = creditAccount?.let { "${it.accountNumber} - ${it.accountName}" } ?: "Pilih Akun Kredit", onValueChange = {}, readOnly = true, label = { Text("Akun Kredit") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCreditExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded = isCreditExpanded, onDismissRequest = { isCreditExpanded = false }) {
                    allAccounts.forEach { account ->
                        DropdownMenuItem(text = { Text("${account.accountNumber} - ${account.accountName}") }, onClick = {
                            creditAccount = account
                            isCreditExpanded = false
                        })
                    }
                }
            }

            ExposedDropdownMenuBox(expanded = isUnitUsahaExpanded, onExpandedChange = { isUnitUsahaExpanded = !isUnitUsahaExpanded }) {
                OutlinedTextField(value = selectedUnitUsaha?.name ?: "Pilih Unit Usaha (Opsional)", onValueChange = {}, readOnly = true, label = { Text("Unit Usaha") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitUsahaExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded = isUnitUsahaExpanded, onDismissRequest = { isUnitUsahaExpanded = false }) {
                    unitUsahaList.forEach { unit ->
                        DropdownMenuItem(text = { Text(unit.name) }, onClick = {
                            selectedUnitUsaha = unit
                            isUnitUsahaExpanded = false
                        })
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    if (description.isNotBlank() && amountDouble != null && debitAccount != null && creditAccount != null) {
                        // ✅ PERBAIKAN DI SINI: Hapus 'localId' dari konstruktor
                        onSave(
                            Transaction(
                                id = transactionToEdit?.id ?: "", // 'id' sudah ada, tidak perlu 'localId'
                                description = description,
                                amount = amountDouble,
                                date = transactionToEdit?.date ?: System.currentTimeMillis(),
                                debitAccountId = debitAccount!!.id,
                                creditAccountId = creditAccount!!.id,
                                debitAccountName = debitAccount!!.accountName,
                                creditAccountName = creditAccount!!.accountName,
                                unitUsahaId = selectedUnitUsaha?.id ?: ""
                            )
                        )
                    } else {
                        Toast.makeText(context, "Harap isi semua kolom dengan benar.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Simpan Jurnal") }
        }
    }
}