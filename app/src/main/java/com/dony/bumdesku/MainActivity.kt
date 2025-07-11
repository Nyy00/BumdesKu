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
import androidx.compose.material.icons.filled.*
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
import com.dony.bumdesku.data.*
import com.dony.bumdesku.repository.TransactionRepository
import com.dony.bumdesku.repository.UnitUsahaRepository
import com.dony.bumdesku.ui.theme.BumdesKuTheme
import com.dony.bumdesku.viewmodel.TransactionViewModel
import com.dony.bumdesku.viewmodel.TransactionViewModelFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = BumdesDatabase.getDatabase(this)
        val transactionRepository = TransactionRepository(database.transactionDao())
        val unitUsahaRepository = UnitUsahaRepository(database.unitUsahaDao())
        val factory = TransactionViewModelFactory(transactionRepository, unitUsahaRepository)

        setContent {
            BumdesKuTheme {
                BumdesApp(factory = factory)
            }
        }
    }
}

@Composable
fun BumdesApp(factory: TransactionViewModelFactory) {
    val navController = rememberNavController()
    val viewModel: TransactionViewModel = viewModel(factory = factory)

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable("transaction_list") {
            val transactions by viewModel.allTransactions.collectAsStateWithLifecycle(emptyList())
            val dashboardData by viewModel.dashboardData.collectAsStateWithLifecycle()
            TransactionListScreen(
                transactions = transactions,
                dashboardData = dashboardData,
                onAddItemClick = { navController.navigate("add_transaction") },
                onItemClick = { transactionId -> navController.navigate("edit_transaction/$transactionId") },
                onDeleteClick = { transaction -> viewModel.delete(transaction) },
                onNavigateUp = { navController.popBackStack() },
                onNavigateToUnitUsaha = { navController.navigate("unit_usaha_management") },
                onNavigateToReport = {
                    viewModel.clearReport() // Bersihkan laporan lama sebelum navigasi
                    navController.navigate("report_screen")
                }
            )
        }
        composable("add_transaction") {
            AddTransactionScreen(
                viewModel = viewModel,
                onSave = { transaction ->
                    viewModel.insert(transaction)
                    navController.popBackStack()
                },
                onNavigateUp = { navController.popBackStack() }
            )
        }
        composable(
            "edit_transaction/{transactionId}",
            arguments = listOf(navArgument("transactionId") { type = NavType.IntType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getInt("transactionId")
            if (transactionId != null) {
                val transactionToEdit by viewModel.getTransactionById(transactionId)
                    .collectAsStateWithLifecycle(initialValue = null)
                transactionToEdit?.let { transaction ->
                    AddTransactionScreen(
                        viewModel = viewModel,
                        transactionToEdit = transaction,
                        onSave = { updatedTransaction ->
                            viewModel.update(updatedTransaction)
                            navController.popBackStack()
                        },
                        onNavigateUp = { navController.popBackStack() }
                    )
                }
            }
        }
        composable("unit_usaha_management") {
            val unitUsahaList by viewModel.allUnitUsaha.collectAsStateWithLifecycle(emptyList())
            UnitUsahaManagementScreen(
                unitUsahaList = unitUsahaList,
                onAddUnitUsaha = { unitName -> viewModel.insert(UnitUsaha(name = unitName)) },
                onDeleteUnitUsaha = { unitUsaha -> viewModel.delete(unitUsaha) },
                onNavigateUp = { navController.popBackStack() }
            )
        }
        composable("report_screen") {
            val reportData by viewModel.reportData.collectAsStateWithLifecycle()
            ReportScreen(
                reportData = reportData,
                onGenerateReport = { startDate, endDate -> viewModel.generateReport(startDate, endDate) },
                onNavigateUp = { navController.popBackStack() }
            )
        }
    }
}

// --- Layar Daftar Transaksi ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    transactions: List<Transaction>,
    dashboardData: DashboardData,
    onAddItemClick: () -> Unit,
    onItemClick: (Int) -> Unit,
    onDeleteClick: (Transaction) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToUnitUsaha: () -> Unit,
    onNavigateToReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Transaksi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToUnitUsaha) {
                        Icon(Icons.Default.Store, "Manajemen Unit Usaha")
                    }
                    IconButton(onClick = onNavigateToReport) {
                        Icon(Icons.Default.Assessment, "Halaman Laporan")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItemClick) {
                Icon(Icons.Default.Add, "Tambah Transaksi")
            }
        }
    ) { paddingValues ->
        // Dialog Konfirmasi Hapus... (tidak berubah)
        if (transactionToDelete != null) {
            AlertDialog(
                onDismissRequest = { transactionToDelete = null },
                title = { Text("Konfirmasi Hapus") },
                text = { Text("Apakah Anda yakin ingin menghapus transaksi '${transactionToDelete?.description}'?") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteClick(transactionToDelete!!)
                            transactionToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    Button(onClick = { transactionToDelete = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // TAMPILKAN KARTU DASHBOARD DI SINI
            DashboardCard(data = dashboardData)

            if (transactions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Belum ada transaksi.", style = MaterialTheme.typography.bodyLarge)
                }
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
        val currencyFormat =
            NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }

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
                    Text(
                        "Saldo Akhir",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = currencyFormat.format(data.finalBalance),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }


// --- Layar Tambah & Edit Transaksi (Dengan Dropdown Unit Usaha) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    transactionToEdit: Transaction? = null,
    onSave: (Transaction) -> Unit,
    onNavigateUp: () -> Unit,
    // Tambahkan parameter baru
    viewModel: TransactionViewModel
) {
    // Ambil daftar unit usaha dari ViewModel
    val unitUsahaList by viewModel.allUnitUsaha.collectAsStateWithLifecycle(emptyList())

    // State untuk input fields
    var description by remember { mutableStateOf(transactionToEdit?.description ?: "") }
    var amount by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(transactionToEdit?.category ?: "") }
    val transactionTypes = listOf("PEMASUKAN", "PENGELUARAN")
    var selectedType by remember { mutableStateOf(transactionToEdit?.type ?: transactionTypes[0]) }

    // State untuk dropdown unit usaha
    var selectedUnitUsaha by remember { mutableStateOf<UnitUsaha?>(null) }
    var isUnitUsahaExpanded by remember { mutableStateOf(false) }

    // Efek untuk memilih unit usaha yang benar saat mode edit
    LaunchedEffect(unitUsahaList, transactionToEdit) {
        if (transactionToEdit != null && unitUsahaList.isNotEmpty()) {
            selectedUnitUsaha = unitUsahaList.find { it.id == transactionToEdit.unitUsahaId }
        }
    }

    val context = LocalContext.current
    val screenTitle = if (transactionToEdit == null) "Tambah Transaksi Baru" else "Edit Transaksi"

    Scaffold(
        topBar = { TopAppBar(title = { Text(screenTitle) }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
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

            // --- DROPDOWN BARU UNTUK UNIT USAHA ---
            ExposedDropdownMenuBox(
                expanded = isUnitUsahaExpanded,
                onExpandedChange = { isUnitUsahaExpanded = !isUnitUsahaExpanded }
            ) {
                OutlinedTextField(
                    value = selectedUnitUsaha?.name ?: "Pilih Unit Usaha",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unit Usaha") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitUsahaExpanded)
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isUnitUsahaExpanded,
                    onDismissRequest = { isUnitUsahaExpanded = false }
                ) {
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

            Spacer(modifier = Modifier.weight(1f))

            // Tombol Simpan
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    // Pastikan unit usaha sudah dipilih
                    if (description.isNotBlank() && amountDouble != null && category.isNotBlank() && selectedUnitUsaha != null) {
                        val transaction = Transaction(
                            id = transactionToEdit?.id ?: 0,
                            amount = amountDouble,
                            type = selectedType,
                            category = category,
                            description = description,
                            date = transactionToEdit?.date ?: System.currentTimeMillis(),
                            // Gunakan ID dari unit usaha yang dipilih
                            unitUsahaId = selectedUnitUsaha!!.id
                        )
                        onSave(transaction)
                    } else {
                        Toast.makeText(context, "Harap isi semua kolom, termasuk Unit Usaha", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (transactionToEdit == null) "Simpan" else "Perbarui")
            }
        }
    }
}

// --- COMPOSABLE BARU UNTUK MANAJEMEN UNIT USAHA ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitUsahaManagementScreen(
    unitUsahaList: List<UnitUsaha>,
    onAddUnitUsaha: (String) -> Unit,
    onDeleteUnitUsaha: (UnitUsaha) -> Unit,
    onNavigateUp: () -> Unit
) {
    var newUnitName by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manajemen Unit Usaha") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali")
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
            // Form untuk menambah unit usaha baru
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newUnitName,
                    onValueChange = { newUnitName = it },
                    label = { Text("Nama Unit Usaha Baru") },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    if (newUnitName.isNotBlank()) {
                        onAddUnitUsaha(newUnitName)
                        newUnitName = "" // Kosongkan input field
                    } else {
                        Toast.makeText(context, "Nama unit tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Daftar Unit Usaha", style = MaterialTheme.typography.titleMedium)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Daftar unit usaha yang sudah ada
            if (unitUsahaList.isEmpty()) {
                Text("Belum ada unit usaha.")
            } else {
                LazyColumn {
                    items(unitUsahaList, key = { it.id }) { unit ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = unit.name, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onDeleteUnitUsaha(unit) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}
// --- HALAMAN LAPORAN (VERSI FUNGSIONAL) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    reportData: ReportData,
    onGenerateReport: (Long, Long) -> Unit,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    var startDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var endDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val startDateState = rememberDatePickerState(initialSelectedDateMillis = startDateMillis)
    val endDateState = rememberDatePickerState(initialSelectedDateMillis = endDateMillis)

    // Update tanggal saat dipilih di DatePicker
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            // Tombol Generate
            Button(
                onClick = { onGenerateReport(startDateMillis, endDateMillis + 86400000 - 1) }, // Tambah 1 hari agar inklusif
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tampilkan Laporan")
            }

            Divider()

            // Tampilkan hasil jika laporan sudah digenerate
            if (reportData.isGenerated) {
                DashboardCard(
                    data = DashboardData(
                        totalIncome = reportData.totalIncome,
                        totalExpenses = reportData.totalExpenses,
                        finalBalance = reportData.netProfit
                    )
                )
            } else {
                Text("Pilih rentang tanggal lalu klik 'Tampilkan Laporan'.")
            }
        }

        // Dialog Date Picker
        if (showStartDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showStartDatePicker = false },
                confirmButton = {
                    Button(onClick = { showStartDatePicker = false }) { Text("OK") }
                }
            ) {
                DatePicker(state = startDateState)
            }
        }
        if (showEndDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showEndDatePicker = false },
                confirmButton = {
                    Button(onClick = { showEndDatePicker = false }) { Text("OK") }
                }
            ) {
                DatePicker(state = endDateState)
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
        val currencyFormat =
            NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }
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
                    Text(
                        text = transaction.description,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
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