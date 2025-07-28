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
import com.dony.bumdesku.features.toko.report.SalesReportScreen
import com.dony.bumdesku.features.toko.report.SalesReportViewModel
import com.dony.bumdesku.features.toko.report.SalesReportViewModelFactory
import com.google.gson.Gson
import com.dony.bumdesku.features.toko.report.SaleDetailScreen
import com.dony.bumdesku.features.toko.report.SaleDetailViewModel
import com.dony.bumdesku.features.toko.report.SaleDetailViewModelFactory
import com.dony.bumdesku.repository.*
import com.dony.bumdesku.screens.*
import com.dony.bumdesku.features.toko.PosScreen
import com.dony.bumdesku.features.toko.PosViewModel
import com.dony.bumdesku.features.toko.PosViewModelFactory
import com.dony.bumdesku.ui.theme.BumdesKuTheme
import com.dony.bumdesku.viewmodel.*
import com.dony.bumdesku.features.agribisnis.*
import com.dony.bumdesku.data.AgriDao
import com.dony.bumdesku.features.agribisnis.AgriViewModel
import com.dony.bumdesku.features.agribisnis.AgriViewModelFactory
import com.dony.bumdesku.features.agribisnis.HarvestListScreen
import com.dony.bumdesku.features.agribisnis.AddHarvestScreen
import com.dony.bumdesku.features.agribisnis.ProduceSaleScreen
import com.dony.bumdesku.repository.AgriRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi Database dan Repositori (tidak ada perubahan di sini)
        val database = BumdesDatabase.getDatabase(this)
        val transactionDao = database.transactionDao()
        val unitUsahaDao = database.unitUsahaDao()
        val assetDao = database.assetDao()
        val accountDao = database.accountDao()
        val debtDao = database.debtDao()
        val saleDao = database.saleDao()
        val agriDao = database.agriDao()

        val accountRepository = AccountRepository(accountDao)
        val unitUsahaRepository = UnitUsahaRepository(unitUsahaDao)
        val assetRepository = AssetRepository(assetDao)
        val transactionRepository = TransactionRepository(transactionDao)
        val debtRepository = DebtRepository(debtDao)
        val posRepository = PosRepository(saleDao, assetRepository, transactionRepository, accountRepository)
        val agriRepository = AgriRepository(agriDao, transactionRepository, accountRepository)

        // Inisialisasi ViewModel Factories (tidak ada perubahan di sini)
        val transactionViewModelFactory = TransactionViewModelFactory(transactionRepository, unitUsahaRepository, accountRepository)
        val assetViewModelFactory = AssetViewModelFactory(assetRepository, unitUsahaRepository)
        val accountViewModelFactory = AccountViewModelFactory(accountRepository)
        val debtViewModelFactory = DebtViewModelFactory(debtRepository, transactionRepository, accountRepository, unitUsahaRepository)
        val posViewModelFactory = PosViewModelFactory(assetRepository, posRepository)
        val salesReportViewModelFactory = SalesReportViewModelFactory(posRepository)
        val saleDetailViewModelFactory = SaleDetailViewModelFactory()
        val agriViewModelFactory = AgriViewModelFactory(agriRepository)

        // AuthViewModelFactory sekarang membutuhkan semua repositori
        val authViewModelFactory = AuthViewModelFactory(
            unitUsahaRepository,
            transactionRepository,
            assetRepository,
            posRepository,
            accountRepository,
            debtRepository,
            agriRepository
        )

        setContent {
            BumdesKuTheme {
                BumdesApp(
                    transactionViewModelFactory = transactionViewModelFactory,
                    authViewModelFactory = authViewModelFactory,
                    assetViewModelFactory = assetViewModelFactory,
                    accountViewModelFactory = accountViewModelFactory,
                    debtViewModelFactory = debtViewModelFactory,
                    salesReportViewModelFactory = salesReportViewModelFactory,
                    saleDetailViewModelFactory = saleDetailViewModelFactory,
                    posViewModelFactory = posViewModelFactory,
                    agriViewModelFactory = agriViewModelFactory
                )
            }
        }
    }
}

@Composable
fun BumdesApp(
    transactionViewModelFactory: TransactionViewModelFactory,
    authViewModelFactory: AuthViewModelFactory,
    assetViewModelFactory: AssetViewModelFactory,
    accountViewModelFactory: AccountViewModelFactory,
    debtViewModelFactory: DebtViewModelFactory,
    salesReportViewModelFactory: SalesReportViewModelFactory,
    saleDetailViewModelFactory: SaleDetailViewModelFactory,
    posViewModelFactory: PosViewModelFactory,
    agriViewModelFactory: AgriViewModelFactory,
) {
    val navController = rememberNavController()
    val auth = Firebase.auth
    val context = LocalContext.current

    val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
    val transactionViewModel: TransactionViewModel = viewModel(factory = transactionViewModelFactory)

    // ✅ BLOK KODE YANG ANDA TUNJUKKAN TELAH DIHAPUS SELURUHNYA
    // Semua logika sinkronisasi sekarang ada di dalam AuthViewModel.

    val startDestination = if (auth.currentUser != null) "home" else "login"

    NavHost(navController = navController, startDestination = startDestination) {

        composable("home") {
            val debtViewModel: DebtViewModel = viewModel(factory = debtViewModelFactory)
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            val healthData by transactionViewModel.financialHealthData.collectAsStateWithLifecycle()
            val debtSummary by debtViewModel.debtSummary.collectAsStateWithLifecycle()
            // Ambil unit usaha yang aktif dari AuthViewModel
            val activeUnitUsaha by authViewModel.activeUnitUsaha.collectAsStateWithLifecycle()


            HomeScreen(
                userRole = userProfile?.role ?: "pengurus",
                financialHealthData = healthData,
                debtSummary = debtSummary,
                // Kirim tipe unit usaha yang aktif ke HomeScreen
                activeUnitUsahaType = activeUnitUsaha?.type ?: UnitUsahaType.UMUM,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable("unit_usaha_management") {
            val unitUsahaList by transactionViewModel.allUnitUsaha.collectAsStateWithLifecycle(
                emptyList()
            )
            UnitUsahaManagementScreen(
                unitUsahaList = unitUsahaList,
                // Pastikan lambda mengirimkan objek UnitUsaha dengan tipe yang benar
                onAddUnitUsaha = { unitName, unitType ->
                    transactionViewModel.insert(UnitUsaha(name = unitName, type = unitType))
                },
                onDeleteUnitUsaha = { unitUsaha -> transactionViewModel.delete(unitUsaha) },
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable("login") {
            val authState by authViewModel.authState.collectAsStateWithLifecycle()
            val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()
            val navigationState by authViewModel.loginNavigationState.collectAsStateWithLifecycle()

            LoginScreen(
                authState = authState,
                onLoginClick = { email, password ->
                    authViewModel.loginUser(email, password)
                },
                onNavigateToRegister = { navController.navigate("register") }
            )

            // --- NAVIGATION LOGIC SIMPLIFIED ---
            LaunchedEffect(navigationState) {
                when (navigationState) {
                    LoginNavigationState.NAVIGATE_TO_HOME_SPESIFIK,
                    LoginNavigationState.NAVIGATE_TO_HOME_UMUM -> {
                        navController.navigate("home") { popUpTo("login") { inclusive = true } }
                        authViewModel.resetNavigationState()
                        authViewModel.resetAuthState()
                    }

                    LoginNavigationState.NAVIGATE_TO_SELECTION -> {
                        navController.navigate("unit_usaha_selection") {
                            popUpTo("login") {
                                inclusive = true
                            }
                        }
                        authViewModel.resetNavigationState()
                        authViewModel.resetAuthState()
                    }

                    LoginNavigationState.IDLE -> { /* Do nothing */
                    }
                }
            }

            // This handles error messages
            LaunchedEffect(authState) {
                if (authState == AuthState.ERROR) {
                    Toast.makeText(context, errorMessage ?: "Login Gagal", Toast.LENGTH_SHORT)
                        .show()
                    authViewModel.resetAuthState()
                }
            }
        }

        // --- Rute Baru untuk Pemilihan Unit Usaha ---
        composable("unit_usaha_selection") {
            val unitUsahaList by authViewModel.userUnitUsahaList.collectAsStateWithLifecycle()

            UnitUsahaSelectionScreen(
                unitUsahaList = unitUsahaList,
                onUnitSelected = { selectedUnit ->
                    authViewModel.setActiveUnitUsaha(selectedUnit)
                    navController.navigate("home") {
                        // Hapus layar pemilihan dari back stack
                        popUpTo("unit_usaha_selection") { inclusive = true }
                    }
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                }
            )
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
            // Kita butuh authViewModel di sini untuk mengambil daftar unit usaha
            val authState by authViewModel.authState.collectAsStateWithLifecycle()
            val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()

            RegisterScreen(
                authViewModel = authViewModel, // Berikan ViewModel ke layar
                onRegisterClick = { email, password, unitUsahaId ->
                    authViewModel.registerUser(email, password, unitUsahaId)
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
                searchQuery = searchQuery,
                onSearchQueryChange = transactionViewModel::onSearchQueryChange,
                onAddItemClick = { navController.navigate("add_transaction") },

                // ✅ PERBAIKAN DI SINI:
                // Ubah transaction.localId menjadi transaction.id
                onItemClick = { transaction ->
                    navController.navigate("edit_transaction/${transaction.id}")
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
            // ✅ UBAH 1: Tipe argumen menjadi StringType
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            // ✅ UBAH 2: Ambil argumen sebagai String
            val transactionId = backStackEntry.arguments?.getString("transactionId")
            val localContext = LocalContext.current

            if (transactionId != null) {
                // ✅ UBAH 3: Panggil fungsi ViewModel dengan ID yang sudah String
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
            // Kita butuh AuthViewModel untuk mendapatkan peran pengguna
            val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()

            val assetViewModel: AssetViewModel = viewModel(factory = assetViewModelFactory)

            AssetListScreen(
                viewModel = assetViewModel, // Berikan ViewModel langsung
                userRole = userProfile?.role ?: "pengurus", // Berikan peran pengguna
                onAddAssetClick = { navController.navigate("add_asset") },
                onItemClick = { asset ->
                    navController.navigate("edit_asset/${asset.id}")
                },
                onNavigateUp = { navController.popBackStack() },
                onDeleteClick = { asset -> assetViewModel.delete(asset) }
            )
        }

        composable("add_asset") {
            val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            val activeUnitUsaha by authViewModel.activeUnitUsaha.collectAsStateWithLifecycle()

            val assetViewModel: AssetViewModel = viewModel(factory = assetViewModelFactory)

            AddAssetScreen(
                viewModel = assetViewModel,
                userRole = userProfile?.role ?: "pengurus", // Berikan peran pengguna
                activeUnitUsaha = activeUnitUsaha, // Berikan unit usaha yang aktif
                onSaveComplete = { navController.popBackStack() },
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable(
            "edit_asset/{assetId}",
            // ✅ UBAH 1: Tipe argumen di sini harus StringType
            arguments = listOf(navArgument("assetId") { type = NavType.StringType })
        ) { backStackEntry ->
            // ✅ UBAH 2: Ambil argumen sebagai String, bukan Int
            val assetId = backStackEntry.arguments?.getString("assetId")
            if (assetId != null) {
                val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
                val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
                val activeUnitUsaha by authViewModel.activeUnitUsaha.collectAsStateWithLifecycle()

                val assetViewModel: AssetViewModel = viewModel(factory = assetViewModelFactory)
                // ✅ UBAH 3: Panggil fungsi ViewModel dengan assetId yang sekarang sudah String
                val assetToEdit by assetViewModel.getAssetById(assetId)
                    .collectAsStateWithLifecycle(initialValue = null)

                assetToEdit?.let {
                    AddAssetScreen(
                        assetToEdit = it,
                        viewModel = assetViewModel,
                        userRole = userProfile?.role ?: "pengurus",
                        activeUnitUsaha = activeUnitUsaha,
                        onSaveComplete = { navController.popBackStack() },
                        onNavigateUp = { navController.popBackStack() }
                    )
                }
            }
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

        composable("payable_list") {
            val debtViewModel: DebtViewModel = viewModel(factory = debtViewModelFactory)
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            PayableListScreen(
                viewModel = debtViewModel,
                userRole = userProfile?.role ?: "pengurus",
                onAddItemClick = { navController.navigate("add_payable") },
                onEditItemClick = { payableId -> navController.navigate("edit_payable/$payableId") },
                onNavigateUp = { navController.popBackStack() },
                onMarkAsPaid = { payable -> debtViewModel.markPayableAsPaid(payable) },
                onDeleteItem = { payable -> debtViewModel.delete(payable) }
            )
        }

        composable("add_payable") {
            val debtViewModel: DebtViewModel = viewModel(factory = debtViewModelFactory)
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            val activeUnitUsaha by authViewModel.activeUnitUsaha.collectAsStateWithLifecycle()
            PayableEntryScreen(
                payableToEdit = null,
                viewModel = debtViewModel,
                userRole = userProfile?.role ?: "pengurus",
                activeUnitUsaha = activeUnitUsaha,
                onSave = { updatedPayable ->
                    debtViewModel.update(updatedPayable)
                    navController.popBackStack()
                },
                onSaveWithJournal = { payable, account ->
                    debtViewModel.insertPayableWithJournal(payable, account)
                    navController.popBackStack()
                },
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable(
            "edit_payable/{payableId}",
            arguments = listOf(navArgument("payableId") { type = NavType.StringType })
        ) { backStackEntry ->
            val debtViewModel: DebtViewModel = viewModel(factory = debtViewModelFactory)
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            val activeUnitUsaha by authViewModel.activeUnitUsaha.collectAsStateWithLifecycle()
            val payableId = backStackEntry.arguments?.getString("payableId")
            val payableToEdit by debtViewModel.getPayableById(payableId ?: "").collectAsState(initial = null)

            if (payableToEdit != null) {
                PayableEntryScreen(
                    payableToEdit = payableToEdit,
                    viewModel = debtViewModel,
                    userRole = userProfile?.role ?: "pengurus",
                    activeUnitUsaha = activeUnitUsaha,
                    onSave = { updatedPayable ->
                        debtViewModel.update(updatedPayable)
                        navController.popBackStack()
                    },
                    onSaveWithJournal = { _, _ -> /* Tidak digunakan saat edit */ },
                    onNavigateUp = { navController.popBackStack() }
                )
            }
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

        composable("receivable_list") {
            val debtViewModel: DebtViewModel = viewModel(factory = debtViewModelFactory)
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            // Menggunakan viewModel langsung untuk mengakses semua state yang diperlukan
            ReceivableListScreen(
                viewModel = debtViewModel,
                userRole = userProfile?.role ?: "pengurus",
                onAddItemClick = { navController.navigate("add_receivable") },
                onEditItemClick = { receivableId -> navController.navigate("edit_receivable/$receivableId") },
                onNavigateUp = { navController.popBackStack() },
                onMarkAsPaid = { receivable -> debtViewModel.markReceivableAsPaid(receivable) },
                onDeleteItem = { receivable -> debtViewModel.delete(receivable) }
            )
        }

        composable("add_receivable") {
            val debtViewModel: DebtViewModel = viewModel(factory = debtViewModelFactory)
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            val activeUnitUsaha by authViewModel.activeUnitUsaha.collectAsStateWithLifecycle()
            ReceivableEntryScreen(
                receivableToEdit = null, // Mode Tambah
                viewModel = debtViewModel,
                userRole = userProfile?.role ?: "pengurus",
                activeUnitUsaha = activeUnitUsaha,
                onSave = { updatedReceivable ->
                    debtViewModel.update(updatedReceivable)
                    navController.popBackStack()
                },
                onSaveWithJournal = { receivable, account ->
                    debtViewModel.insertReceivableWithJournal(receivable, account)
                    navController.popBackStack()
                },
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable(
            "edit_receivable/{receivableId}",
            // Pastikan argumen adalah String
            arguments = listOf(navArgument("receivableId") { type = NavType.StringType })
        ) { backStackEntry ->
            val debtViewModel: DebtViewModel = viewModel(factory = debtViewModelFactory)
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            val activeUnitUsaha by authViewModel.activeUnitUsaha.collectAsStateWithLifecycle()
            // Ambil argumen sebagai String
            val receivableId = backStackEntry.arguments?.getString("receivableId")
            val receivableToEdit by debtViewModel.getReceivableById(receivableId ?: "").collectAsState(initial = null)

            if (receivableToEdit != null) {
                ReceivableEntryScreen(
                    receivableToEdit = receivableToEdit, // Mode Edit
                    viewModel = debtViewModel,
                    userRole = userProfile?.role ?: "pengurus",
                    activeUnitUsaha = activeUnitUsaha,
                    onSave = { updatedReceivable ->
                        debtViewModel.update(updatedReceivable)
                        navController.popBackStack()
                    },
                    onSaveWithJournal = { _, _ -> /* Tidak digunakan saat edit */ },
                    onNavigateUp = { navController.popBackStack() }
                )
            }
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

        composable("pos_screen") {
            // Ambil ViewModel utama yang memegang state user
            val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            val activeUnitUsaha by authViewModel.activeUnitUsaha.collectAsStateWithLifecycle()

            // Buat instance PosViewModel
            val posViewModel: PosViewModel = viewModel(factory = posViewModelFactory)

            // Berikan data user dan unit usaha ke PosViewModel saat composable ini aktif
            LaunchedEffect(userProfile, activeUnitUsaha) {
                posViewModel.userProfile.value = userProfile
                posViewModel.activeUnitUsaha.value = activeUnitUsaha
            }

            PosScreen(
                posViewModel = posViewModel,
                onNavigateUp = { navController.popBackStack() },
                onSaleComplete = { navController.popBackStack() }
            )
        }

        composable("sales_report") {
            val viewModel: SalesReportViewModel = viewModel(factory = salesReportViewModelFactory)
            SalesReportScreen(
                viewModel = viewModel,
                onNavigateUp = { navController.popBackStack() },
                onSaleClick = { sale ->
                    // Kirim objek sale sebagai JSON string saat navigasi
                    val saleJson = Gson().toJson(sale)
                    navController.navigate("sale_detail/${saleJson}")
                }
            )
        }

        composable(
            "sale_detail/{saleJson}",
            arguments = listOf(navArgument("saleJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val saleJson = backStackEntry.arguments?.getString("saleJson")
            if (saleJson != null) {
                val viewModel: SaleDetailViewModel = viewModel(factory = saleDetailViewModelFactory)
                val sale = Gson().fromJson(saleJson, Sale::class.java)
                SaleDetailScreen(
                    viewModel = viewModel,
                    sale = sale,
                    onNavigateUp = { navController.popBackStack() }
                )
            }
        }

        // Rute untuk Fitur Agribisnis
        composable("harvest_list") {
            val agriViewModel: AgriViewModel = viewModel(factory = agriViewModelFactory)
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            HarvestListScreen(
                viewModel = agriViewModel,
                userRole = userProfile?.role ?: "pengurus",
                onAddHarvestClick = { navController.navigate("add_harvest") },
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable("add_harvest") {
            val agriViewModel: AgriViewModel = viewModel(factory = agriViewModelFactory)
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            val activeUnitUsaha by authViewModel.activeUnitUsaha.collectAsStateWithLifecycle()
            AddHarvestScreen(
                viewModel = agriViewModel,
                userRole = userProfile?.role ?: "pengurus",
                activeUnitUsaha = activeUnitUsaha,
                onSaveComplete = { navController.popBackStack() },
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable("produce_sale") {
            // Ambil semua ViewModel yang diperlukan
            val agriViewModel: AgriViewModel = viewModel(factory = agriViewModelFactory)
            val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)

            // Ambil data user yang sedang login dari AuthViewModel
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            val activeUnitUsaha by authViewModel.activeUnitUsaha.collectAsStateWithLifecycle()

            ProduceSaleScreen(
                viewModel = agriViewModel,
                userProfile = userProfile,
                activeUnitUsaha = activeUnitUsaha,
                onNavigateUp = { navController.popBackStack() },
                onSaleComplete = { navController.popBackStack() } // Kembali setelah penjualan selesai
            )
        }

        composable("agri_sale_report") {
            val agriViewModel: AgriViewModel = viewModel(factory = agriViewModelFactory)
            AgriSaleReportScreen(
                viewModel = agriViewModel,
                onNavigateUp = { navController.popBackStack() }
            )
        }

    }
}
