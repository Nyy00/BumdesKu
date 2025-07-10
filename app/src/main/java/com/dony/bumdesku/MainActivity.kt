package com.dony.bumdesku

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dony.bumdesku.data.BumdesDatabase
import com.dony.bumdesku.data.DashboardData
import com.dony.bumdesku.data.Transaction
import com.dony.bumdesku.repository.TransactionRepository
import com.dony.bumdesku.ui.theme.BumdesKuTheme
import com.dony.bumdesku.viewmodel.TransactionViewModel
import com.dony.bumdesku.viewmodel.TransactionViewModelFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = TransactionViewModelFactory(
            TransactionRepository(BumdesDatabase.getDatabase(this).transactionDao())
        )
        setContent {
            BumdesKuTheme {
                BumdesApp(factory = factory)
            }
        }
    }
}

// Composable utama yang mengatur navigasi
@Composable
fun BumdesApp(factory: TransactionViewModelFactory) {
    val navController = rememberNavController()
    val viewModel: TransactionViewModel = viewModel(factory = factory)

    NavHost(navController = navController, startDestination = "transaction_list") {
        composable("transaction_list") {
            val transactions by viewModel.allTransactions.collectAsStateWithLifecycle(emptyList())
            // AMBIL DATA DASHBOARD DARI VIEWMODEL
            val dashboardData by viewModel.dashboardData.collectAsStateWithLifecycle()

            TransactionListScreen(
                transactions = transactions,
                dashboardData = dashboardData, // KIRIM DATA KE LAYAR
                onAddClick = { navController.navigate("add_transaction") },
                onItemClick = { transactionId ->
                    navController.navigate("edit_transaction/$transactionId")
                },
                onDeleteClick = { transaction ->
                    viewModel.delete(transaction)
                }
            )
        }
        // Rute untuk layar tambah transaksi
        composable("add_transaction") {
            AddTransactionScreen(
                onSave = { transaction ->
                    viewModel.insert(transaction)
                    navController.popBackStack()
                },
                onNavigateUp = { navController.popBackStack() }
            )
        }
        // Rute untuk layar edit transaksi
        composable(
            route = "edit_transaction/{transactionId}",
            arguments = listOf(navArgument("transactionId") { type = NavType.IntType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getInt("transactionId")
            if (transactionId != null) {
                // Ambil transaksi yang akan diedit dari ViewModel
                val transactionToEdit by viewModel.getTransactionById(transactionId)
                    .collectAsStateWithLifecycle(initialValue = null)

                transactionToEdit?.let { transaction ->
                    AddTransactionScreen(
                        transactionToEdit = transaction, // Kirim data untuk di-edit
                        onSave = { updatedTransaction ->
                            viewModel.update(updatedTransaction)
                            navController.popBackStack()
                        },
                        onNavigateUp = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

// --- Layar Daftar Transaksi (Dengan Dashboard) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    transactions: List<Transaction>,
    dashboardData: DashboardData, // TERIMA DATA DASHBOARD
    onAddClick: () -> Unit,
    onItemClick: (Int) -> Unit,
    onDeleteClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Keuangan BUMDes") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Transaksi")
            }
        }
    ) { paddingValues ->
        // Dialog Konfirmasi Hapus (tidak berubah)
        if (transactionToDelete != null) {
            // ...
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // TAMPILKAN KARTU DASHBOARD DI SINI
            DashboardCard(data = dashboardData)

            if (transactions.isEmpty()) {
                // ... (pesan kosong tidak berubah)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(transactions, key = { it.id }) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onItemClick = { onItemClick(transaction.id) },
                            onDeleteClick = { transactionToDelete = transaction }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

// --- COMPOSABLE BARU UNTUK DASHBOARD ---
@Composable
fun DashboardCard(data: DashboardData) {
    val localeID = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Ringkasan Keuangan", style = MaterialTheme.typography.titleLarge)
            Divider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Pemasukan:")
                Text(
                    text = currencyFormat.format(data.totalIncome),
                    color = Color(0xFF008800),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Pengeluaran:")
                Text(
                    text = currencyFormat.format(data.totalExpenses),
                    color = Color.Red,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Divider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Saldo Akhir", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = currencyFormat.format(data.finalBalance),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}


// --- Layar Tambah & Edit Transaksi (Versi Lengkap) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    transactionToEdit: Transaction? = null, // Parameter opsional untuk mode edit
    onSave: (Transaction) -> Unit,
    onNavigateUp: () -> Unit
) {
    // State untuk menampung nilai dari setiap input field
    var description by remember { mutableStateOf(transactionToEdit?.description ?: "") }
    var amount by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(transactionToEdit?.category ?: "") }
    val transactionTypes = listOf("PEMASUKAN", "PENGELUARAN")
    var selectedType by remember { mutableStateOf(transactionToEdit?.type ?: transactionTypes[0]) }
    var isExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val screenTitle = if (transactionToEdit == null) "Tambah Transaksi Baru" else "Edit Transaksi"

    Scaffold(
        topBar = { TopAppBar(title = { Text(screenTitle) }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Menerapkan padding dari Scaffold
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // Menambahkan scroll jika layar kecil
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Kolom input untuk Deskripsi
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi") },
                modifier = Modifier.fillMaxWidth()
            )

            // Kolom input untuk Nominal
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Nominal (Contoh: 50000)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Kolom input untuk Kategori
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Kategori (Contoh: Sewa Kursi)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Dropdown untuk memilih Jenis Transaksi
            ExposedDropdownMenuBox(
                expanded = isExpanded,
                onExpandedChange = { isExpanded = !isExpanded }
            ) {
                OutlinedTextField(
                    value = selectedType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Jenis Transaksi") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false }
                ) {
                    transactionTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                selectedType = type
                                isExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // Pendorong agar tombol ke bawah

            // Tombol Simpan/Perbarui
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    if (description.isNotBlank() && amountDouble != null && category.isNotBlank()) {
                        val transaction = Transaction(
                            id = transactionToEdit?.id ?: 0, // Gunakan id lama jika edit
                            amount = amountDouble,
                            type = selectedType,
                            category = category,
                            description = description,
                            date = transactionToEdit?.date ?: System.currentTimeMillis(), // Gunakan tanggal lama jika edit
                            unitUsahaId = 1
                        )
                        onSave(transaction)
                    } else {
                        Toast.makeText(context, "Harap isi semua kolom dengan benar", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (transactionToEdit == null) "Simpan" else "Perbarui")
            }
        }
    }
}

// --- Item untuk Satu Transaksi (Dengan tombol hapus) ---
@Composable
fun TransactionItem(
    transaction: Transaction,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    // Format angka dan tanggal
    val localeID = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", localeID)
    val formattedAmount = currencyFormat.format(transaction.amount)
    val formattedDate = dateFormat.format(Date(transaction.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick), // Membuat card bisa diklik
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = transaction.description, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(text = transaction.category, fontSize = 14.sp)
                Text(text = formattedDate, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formattedAmount,
                color = if (transaction.type == "PEMASUKAN") Color(0xFF008800) else Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus Transaksi",
                    tint = Color.Gray
                )
            }
        }
    }
}