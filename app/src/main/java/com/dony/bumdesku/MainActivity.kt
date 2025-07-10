package com.dony.bumdesku

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dony.bumdesku.data.BumdesDatabase
import com.dony.bumdesku.data.Transaction
import com.dony.bumdesku.repository.TransactionRepository
import com.dony.bumdesku.ui.theme.BumdesKuTheme
import com.dony.bumdesku.viewmodel.TransactionViewModel
import com.dony.bumdesku.viewmodel.TransactionViewModelFactory
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //-- Inisialisasi arsitektur --//
        val database = BumdesDatabase.getDatabase(this)
        val repository = TransactionRepository(database.transactionDao())
        val factory = TransactionViewModelFactory(repository)

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
        // Rute untuk layar utama (daftar transaksi)
        composable("transaction_list") {
            val transactions by viewModel.allTransactions.collectAsStateWithLifecycle(
                initialValue = emptyList()
            )
            TransactionListScreen(
                transactions = transactions,
                onAddClick = {
                    navController.navigate("add_transaction")
                }
            )
        }
        // Rute untuk layar tambah transaksi
        composable("add_transaction") {
            AddTransactionScreen(
                onSave = { transaction ->
                    // Logika untuk menyimpan dan kembali
                    viewModel.insert(transaction)
                    navController.popBackStack()
                }
            )
        }
    }
}

// --- Layar Daftar Transaksi ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    transactions: List<Transaction>,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Keuangan BUMDes") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Transaksi")
            }
        }
    ) { paddingValues ->
        // Konten layar (daftar atau pesan kosong)
        if (transactions.isEmpty()) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Belum ada transaksi.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)) {
                items(transactions) { transaction ->
                    TransactionItem(transaction = transaction)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// --- Layar Tambah Transaksi (Masih Kosong) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(onSave: (Transaction) -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = { TopAppBar(title = { Text("Tambah Transaksi Baru") }) }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)) {

            Text("Form untuk menambah transaksi akan ada di sini.")
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = {
                // Contoh menyimpan data dummy
                val dummyTransaction = Transaction(
                    amount = 50000.0,
                    type = "PEMASUKAN",
                    category = "Contoh",
                    description = "Ini data coba-coba",
                    date = System.currentTimeMillis(),
                    unitUsahaId = 1
                )
                onSave(dummyTransaction)
                Toast.makeText(context, "Data Dummy Tersimpan!", Toast.LENGTH_SHORT).show()
            }) {
                Text("Simpan Data Dummy")
            }
        }
    }
}

// --- Item untuk Satu Transaksi (Tidak Berubah) ---
@Composable
fun TransactionItem(transaction: Transaction) {
    val localeID = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localeID)
    val formattedAmount = currencyFormat.format(transaction.amount)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = transaction.description, fontWeight = FontWeight.Bold)
                Text(text = transaction.category, fontSize = 14.sp)
                Text(
                    text = Date(transaction.date).toString(),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = formattedAmount,
                color = if (transaction.type == "PEMASUKAN") Color(0xFF008800) else Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}