package com.dony.bumdesku

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dony.bumdesku.data.*
import com.dony.bumdesku.repository.AccountRepository
import com.dony.bumdesku.repository.AssetRepository
import com.dony.bumdesku.repository.DebtRepository
import com.dony.bumdesku.repository.TransactionRepository
import com.dony.bumdesku.repository.UnitUsahaRepository
import com.dony.bumdesku.screens.*
import com.dony.bumdesku.ui.theme.BumdesKuTheme
import com.dony.bumdesku.viewmodel.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi semua dependensi di sini
        val database = BumdesDatabase.getDatabase(this)
        val transactionDao = database.transactionDao()
        val unitUsahaDao = database.unitUsahaDao()
        val assetDao = database.assetDao()
        val accountDao = database.accountDao()
        val debtDao = database.debtDao()

        // Buat instance dari semua repository
        val accountRepository = AccountRepository(accountDao)
        val transactionRepository = TransactionRepository(transactionDao, unitUsahaDao, accountRepository)
        val unitUsahaRepository = UnitUsahaRepository(unitUsahaDao)
        val assetRepository = AssetRepository(assetDao)
        val debtRepository = DebtRepository(debtDao)

        // Buat semua ViewModelFactory
        val transactionViewModelFactory = TransactionViewModelFactory(transactionRepository, unitUsahaRepository, accountRepository)
        val authViewModelFactory = AuthViewModelFactory()
        val assetViewModelFactory = AssetViewModelFactory(assetRepository)
        val accountViewModelFactory = AccountViewModelFactory(accountRepository)
        val debtViewModelFactory = DebtViewModelFactory(debtRepository, transactionRepository, accountRepository)

        setContent {
            BumdesKuTheme {
                // Kirim semua instance yang dibutuhkan ke BumdesApp
                BumdesApp(
                    transactionViewModelFactory = transactionViewModelFactory,
                    authViewModelFactory = authViewModelFactory,
                    assetViewModelFactory = assetViewModelFactory,
                    accountViewModelFactory = accountViewModelFactory,
                    debtViewModelFactory = debtViewModelFactory,
                    // Kirim repository untuk pemicu sinkronisasi
                    repositories = AppRepositories(
                        unitUsaha = unitUsahaRepository,
                        asset = assetRepository,
                        account = accountRepository,
                        transaction = transactionRepository,
                        debt = debtRepository
                    )
                )
            }
        }
    }
}

// Data class untuk membungkus semua repository agar lebih rapi
data class AppRepositories(
    val unitUsaha: UnitUsahaRepository,
    val asset: AssetRepository,
    val account: AccountRepository,
    val transaction: TransactionRepository,
    val debt: DebtRepository
)

@Composable
fun BumdesApp(
    transactionViewModelFactory: TransactionViewModelFactory,
    authViewModelFactory: AuthViewModelFactory,
    assetViewModelFactory: AssetViewModelFactory,
    accountViewModelFactory: AccountViewModelFactory,
    debtViewModelFactory: DebtViewModelFactory,
    repositories: AppRepositories // Terima semua repository
) {
    val navController = rememberNavController()
    val auth = Firebase.auth
    val context = LocalContext.current

    // Inisialisasi ViewModel yang akan dibagikan ke beberapa layar
    val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
    val transactionViewModel: TransactionViewModel = viewModel(factory = transactionViewModelFactory)

    // ✅ PEMICU SINKRONISASI UTAMA
    // Blok ini akan berjalan setiap kali status login berubah.
    val targetUserId by authViewModel.targetUserId.collectAsStateWithLifecycle()
    LaunchedEffect(targetUserId) {
        if (targetUserId != null) {
            // Panggil fungsi sinkronisasi dari SEMUA repository dengan targetId yang benar
            repositories.unitUsaha.syncUnitUsaha(targetUserId!!)
            repositories.asset.syncAssets(targetUserId!!)
            repositories.account.syncAccounts(targetUserId!!)
            repositories.transaction.syncTransactions(targetUserId!!)
            repositories.debt.syncPayables(targetUserId!!)
            repositories.debt.syncReceivables(targetUserId!!)
        }
    }

    val startDestination = if (auth.currentUser != null) "home" else "login"

    NavHost(navController = navController, startDestination = startDestination) {

        composable("home") {
            // ✅ PERBAIKAN DI SINI: Tambahkan baris ini
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            val healthData by transactionViewModel.financialHealthData.collectAsStateWithLifecycle()

            HomeScreen(
                userRole = userProfile?.role ?: "pengurus",
                financialHealthData = healthData,
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable("unit_usaha_management") {
            val unitUsahaList by transactionViewModel.allUnitUsaha.collectAsStateWithLifecycle(emptyList())
            UnitUsahaManagementScreen(
                unitUsahaList = unitUsahaList,
                onAddUnitUsaha = { unitName -> transactionViewModel.insert(UnitUsaha(name = unitName)) },
                onDeleteUnitUsaha = { unitUsaha -> transactionViewModel.delete(unitUsaha) },
                onNavigateUp = { navController.popBackStack() }
            )
        }

        // ... (Sisa NavHost Anda)
        composable("login") {
            val authState by authViewModel.authState.collectAsStateWithLifecycle()
            val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()

            LoginScreen(
                authState = authState,
                onLoginClick = { email, password ->
                    authViewModel.loginUser(email, password)
                },
                onNavigateToRegister = { navController.navigate("register") }
            )

            LaunchedEffect(authState) {
                when (authState) {
                    AuthState.SUCCESS -> {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                        authViewModel.resetAuthState()
                    }

                    AuthState.ERROR -> {
                        Toast.makeText(context, errorMessage ?: "Login Gagal", Toast.LENGTH_SHORT)
                            .show()
                        authViewModel.resetAuthState()
                    }

                    else -> {}
                }
            }
        }

        composable("account_list") {
            val viewModel: AccountViewModel = viewModel(factory = accountViewModelFactory)
            val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
            AccountListScreen(
                accounts = accounts,
                onAddAccountClick = { navController.navigate("add_account") },
                // ✅ Arahkan ke rute baru saat akun diklik
                onAccountClick = { account ->
                    navController.navigate("buku_pembantu/${account.id}")
                },
                onDeleteAccount = { account -> viewModel.delete(account) },
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable("add_account") {
            val viewModel: AccountViewModel = viewModel(factory = accountViewModelFactory)
            AddAccountScreen(
                onSave = { account ->
                    viewModel.insert(account)
                    navController.popBackStack()
                },
                onNavigateUp = { navController.popBackStack() }
            )
        }


        composable("register") {
            val authState by authViewModel.authState.collectAsStateWithLifecycle()
            val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()

            RegisterScreen(
                authState = authState,
                onRegisterClick = { email, password ->
                    authViewModel.registerUser(email, password)
                },
                onNavigateToLogin = { navController.popBackStack() }
            )

            LaunchedEffect(authState) {
                when (authState) {
                    AuthState.SUCCESS -> {
                        Toast.makeText(
                            context,
                            "Registrasi berhasil, silakan login",
                            Toast.LENGTH_SHORT
                        ).show()
                        navController.popBackStack()
                        authViewModel.resetAuthState()
                    }

                    AuthState.ERROR -> {
                        Toast.makeText(
                            context,
                            errorMessage ?: "Registrasi Gagal",
                            Toast.LENGTH_SHORT
                        ).show()
                        authViewModel.resetAuthState()
                    }

                    else -> {}
                }
            }
        }

        composable("profile") {
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            val localContext = LocalContext.current

            ProfileScreen(
                userProfile = userProfile,
                onNavigateUp = { navController.popBackStack() },
                onChangePasswordClick = {
                    authViewModel.changePassword { success, message ->
                        Toast.makeText(localContext, message, Toast.LENGTH_LONG).show()
                    }
                },
                // ✅ Tambahkan aksi untuk tombol logout baru
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        // Hapus semua halaman sebelumnya dari back stack
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // ✅ INI VERSI YANG BENAR DAN LENGKAP (yang duplikat sudah dihapus)
        composable("transaction_list") {

            // Ambil data yang dibutuhkan, termasuk searchQuery
            val transactions by transactionViewModel.allTransactions.collectAsStateWithLifecycle()
            val dashboardData by transactionViewModel.dashboardData.collectAsStateWithLifecycle()
            val chartData by transactionViewModel.chartData.collectAsStateWithLifecycle()
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            val searchQuery by transactionViewModel.searchQuery.collectAsStateWithLifecycle()

            TransactionListScreen(
                transactions = transactions,
                dashboardData = dashboardData,
                chartData = chartData,
                userRole = userProfile?.role ?: "pengurus",
                searchQuery = searchQuery, // <-- Kirim query
                onSearchQueryChange = transactionViewModel::onSearchQueryChange, // <-- Kirim aksi
                onAddItemClick = { navController.navigate("add_transaction") },
                onItemClick = { transaction ->
                    navController.navigate("edit_transaction/${transaction.localId}")
                },
                onDeleteClick = { transaction -> transactionViewModel.delete(transaction) },
                onNavigateUp = { navController.popBackStack() },
                onNavigateToUnitUsaha = { navController.navigate("unit_usaha_management") },
                onNavigateToReport = {
                    transactionViewModel.clearReport()
                    navController.navigate("report_screen")
                }
            )
        }

        composable("add_transaction") {
            AddTransactionScreen(
                viewModel = transactionViewModel,
                onSave = { transaction ->
                    transactionViewModel.insert(transaction)
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
            val localContext = LocalContext.current // Ambil context untuk Toast

            if (transactionId != null) {
                val transactionToEdit by transactionViewModel.getTransactionById(transactionId)
                    .collectAsStateWithLifecycle(initialValue = null)

                // LaunchedEffect digunakan agar Toast & navigasi hanya berjalan sekali
                LaunchedEffect(transactionToEdit) {
                    transactionToEdit?.let { transaction ->
                        if (transaction.isLocked) {
                            // Tampilkan pesan dan kembali jika terkunci
                            Toast.makeText(
                                localContext,
                                "Transaksi ini sudah dikunci dan tidak bisa diubah.",
                                Toast.LENGTH_LONG
                            ).show()
                            navController.popBackStack()
                        }
                    }
                }

                // Tampilkan layar edit hanya jika data ada DAN tidak terkunci
                transactionToEdit?.takeIf { !it.isLocked }?.let { transaction ->
                    AddTransactionScreen(
                        viewModel = transactionViewModel,
                        transactionToEdit = transaction,
                        onSave = { updatedTransaction ->
                            transactionViewModel.update(updatedTransaction)
                            navController.popBackStack()
                        },
                        onNavigateUp = { navController.popBackStack() }
                    )
                }
            }
        }

        composable("asset_list") {
            val assetViewModel: AssetViewModel = viewModel(factory = assetViewModelFactory)
            val assets by assetViewModel.allAssets.collectAsStateWithLifecycle(emptyList())
            AssetListScreen(
                assets = assets,
                onAddAssetClick = { navController.navigate("add_asset") },
                onNavigateUp = { navController.popBackStack() },
                onDeleteClick = { asset -> assetViewModel.delete(asset) }
            )
        }

        composable("add_asset") {
            val assetViewModel: AssetViewModel = viewModel(factory = assetViewModelFactory)
            AddAssetScreen(
                viewModel = assetViewModel,
                onSaveComplete = { navController.popBackStack() },
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable("report_screen") {

            // Ambil semua state yang relevan dari ViewModel
            val reportData by transactionViewModel.reportData.collectAsState()
            val reportTransactions by transactionViewModel.filteredReportTransactions.collectAsState()
            val userProfile by authViewModel.userProfile.collectAsState()
            val unitUsahaList by transactionViewModel.allUnitUsaha.collectAsState(initial = emptyList())
            val allAccounts by transactionViewModel.allAccounts.collectAsState(initial = emptyList()) // ✅ Ambil daftar akun

            ReportScreen(
                reportData = reportData,
                unitUsahaList = unitUsahaList,
                userRole = userProfile?.role ?: "pengurus",
                reportTransactions = reportTransactions,
                allAccounts = allAccounts, // ✅ Berikan daftar akun ke ReportScreen
                onGenerateReport = { startDate, endDate, unitUsaha ->
                    transactionViewModel.generateReport(startDate, endDate, unitUsaha)
                },
                onNavigateUp = { navController.popBackStack() }
            )
        }


        composable("neraca_screen") {
            val neracaData by transactionViewModel.neracaData.collectAsStateWithLifecycle()
            val allAccounts by transactionViewModel.allAccounts.collectAsState(initial = emptyList()) // Ambil semua akun

            NeracaScreen(
                neracaData = neracaData,
                onNavigateUp = { navController.popBackStack() },
                onAccountClick = { neracaItem ->
                    // Cari akun yang sesuai berdasarkan nama dari item neraca yang diklik
                    val targetAccount =
                        allAccounts.find { it.accountName == neracaItem.accountName }
                    if (targetAccount != null) {
                        // Navigasi ke Buku Pembantu dengan ID akun yang ditemukan
                        navController.navigate("buku_pembantu/${targetAccount.id}")
                    }
                }
            )
        }

        composable("neraca_saldo_screen") {
            val neracaSaldoItems by transactionViewModel.neracaSaldoItems.collectAsStateWithLifecycle()

            NeracaSaldoScreen(
                items = neracaSaldoItems,
                onNavigateUp = { navController.popBackStack() }
            )
        }


        composable(
            "buku_pembantu/{accountId}",
            arguments = listOf(navArgument("accountId") { type = NavType.StringType })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId")
            if (accountId != null) {
                val allAccounts by transactionViewModel.allAccounts.collectAsState(initial = emptyList())

                val selectedAccount = allAccounts.find { it.id == accountId }

                if (selectedAccount != null) {
                    val bukuPembantuData by transactionViewModel.getBukuPembantuData(
                        accountId,
                        selectedAccount.category
                    )
                        .collectAsState(initial = BukuPembantuData())

                    BukuPembantuScreen(
                        account = selectedAccount,
                        transactions = bukuPembantuData.transactions,
                        runningBalances = bukuPembantuData.runningBalances,
                        onNavigateUp = { navController.popBackStack() }
                    )
                }
            }
        }

        // TAMBAHKAN RUTE NAVIGASI BARU UNTUK UTANG & PIUTANG
        composable("payable_list") {
            val viewModel: DebtViewModel = viewModel(factory = debtViewModelFactory)
            val payables by viewModel.allPayables.collectAsState(initial = emptyList())
            PayableListScreen(
                payables = payables,
                onAddItemClick = { navController.navigate("add_payable") },
                onNavigateUp = { navController.popBackStack() },
                onMarkAsPaid = { payable -> viewModel.markPayableAsPaid(payable) } // Hubungkan aksi
            )
        }

        composable("add_payable") {
            val viewModel: DebtViewModel = viewModel(factory = debtViewModelFactory)
            AddPayableScreen(
                onSave = { payable ->
                    viewModel.insert(payable)
                    navController.popBackStack()
                },
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable("lpe_screen") {
            // Ambil lpeData dari transactionViewModel yang sudah ada
            val lpeData by transactionViewModel.lpeData.collectAsStateWithLifecycle()

            LpeScreen(
                lpeData = lpeData,
                onGenerateLpe = { startDate, endDate ->
                    // ✅ PERBAIKAN: Gunakan 'transactionViewModel' bukan 'viewModel'
                    transactionViewModel.generateLpe(startDate, endDate)
                },
                onNavigateUp = { navController.popBackStack() }
            )
        }

// ✅ RUTE UNTUK MENAMPILKAN DAFTAR PIUTANG (SEKARANG AKTIF)
        composable("receivable_list") {
            val viewModel: DebtViewModel = viewModel(factory = debtViewModelFactory)
            val receivables by viewModel.allReceivables.collectAsState(initial = emptyList())
            ReceivableListScreen(
                receivables = receivables,
                onAddItemClick = { navController.navigate("add_receivable") },
                onNavigateUp = { navController.popBackStack() },
                onMarkAsPaid = { receivable -> viewModel.markReceivableAsPaid(receivable) } // Hubungkan aksi
            )
        }

// ✅ RUTE BARU UNTUK MENAMBAH PIUTANG
        composable("add_receivable") {
            val viewModel: DebtViewModel = viewModel(factory = debtViewModelFactory)
            AddReceivableScreen(
                onSave = { receivable ->
                    viewModel.insert(receivable)
                    navController.popBackStack()
                },
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable("lock_journal") {
            val localContext = LocalContext.current
            LockJournalScreen(
                onLockClick = { date ->
                    transactionViewModel.lockTransactionsUpTo(date) {
                        // Beri tahu pengguna setelah selesai
                        Toast.makeText(localContext, "Jurnal berhasil dikunci!", Toast.LENGTH_SHORT)
                            .show()
                    }
                },
                onNavigateUp = { navController.popBackStack() }
            )
        }
    }
}
