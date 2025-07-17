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
import com.dony.bumdesku.repository.TransactionRepository
import com.dony.bumdesku.repository.UnitUsahaRepository
import com.dony.bumdesku.screens.*
import com.dony.bumdesku.screens.NeracaScreen
import com.dony.bumdesku.ui.theme.BumdesKuTheme
import com.dony.bumdesku.viewmodel.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = BumdesDatabase.getDatabase(this)

        // Inisialisasi semua DAO
        val transactionDao = database.transactionDao()
        val unitUsahaDao = database.unitUsahaDao()
        val assetDao = database.assetDao()
        val accountDao = database.accountDao() // Pastikan ini sudah ada

        // Inisialisasi semua Repository
        val transactionRepository = TransactionRepository(transactionDao, unitUsahaDao)
        val unitUsahaRepository = UnitUsahaRepository(unitUsahaDao)
        val assetRepository = AssetRepository(assetDao)
        val accountRepository = AccountRepository(accountDao) // Pastikan ini sudah ada

        // Inisialisasi semua ViewModelFactory
        val transactionViewModelFactory = TransactionViewModelFactory(transactionRepository, unitUsahaRepository, accountRepository)
        val authViewModelFactory = AuthViewModelFactory()
        val assetViewModelFactory = AssetViewModelFactory(assetRepository)
        val accountViewModelFactory = AccountViewModelFactory(accountRepository) // Pastikan ini sudah ada

        setContent {
            BumdesKuTheme {
                // ✅ Berikan factory yang baru ke BumdesApp
                BumdesApp(
                    transactionViewModelFactory = transactionViewModelFactory,
                    authViewModelFactory = authViewModelFactory,
                    assetViewModelFactory = assetViewModelFactory,
                    accountViewModelFactory = accountViewModelFactory
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
    accountViewModelFactory: AccountViewModelFactory // ✅ Terima parameter baru
) {
    val navController = rememberNavController()
    val auth = Firebase.auth
    val context = LocalContext.current

    val startDestination = if (auth.currentUser != null) "home" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
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
            val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
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

        composable("home") {
            // Ambil ViewModel dan profil pengguna
            val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()

            HomeScreen(
                // ✅ Berikan nilai untuk userRole
                userRole = userProfile?.role ?: "pengurus",
                onNavigate = { route -> navController.navigate(route) }
            )
        }


        composable("profile") {
            val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
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
            val transactionViewModel: TransactionViewModel =
                viewModel(factory = transactionViewModelFactory)
            val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)

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
            val transactionViewModel: TransactionViewModel =
                viewModel(factory = transactionViewModelFactory)
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
            val transactionViewModel: TransactionViewModel =
                viewModel(factory = transactionViewModelFactory)
            val transactionId = backStackEntry.arguments?.getInt("transactionId")
            if (transactionId != null) {
                val transactionToEdit by transactionViewModel.getTransactionById(transactionId)
                    .collectAsStateWithLifecycle(initialValue = null)

                // Tampilkan layar hanya jika data sudah siap
                transactionToEdit?.let { transaction ->
                    AddTransactionScreen(
                        viewModel = transactionViewModel,
                        transactionToEdit = transaction, // <-- Kirim data untuk di-edit
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

        composable("unit_usaha_management") {
            val transactionViewModel: TransactionViewModel =
                viewModel(factory = transactionViewModelFactory)
            val unitUsahaList by transactionViewModel.allUnitUsaha.collectAsStateWithLifecycle(
                emptyList()
            )
            UnitUsahaManagementScreen(
                unitUsahaList = unitUsahaList,
                onAddUnitUsaha = { unitName -> transactionViewModel.insert(UnitUsaha(name = unitName)) },
                onDeleteUnitUsaha = { unitUsaha -> transactionViewModel.delete(unitUsaha) },
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable("report_screen") {
            val transactionViewModel: TransactionViewModel = viewModel(factory = transactionViewModelFactory)
            val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)

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
                allAccounts = allAccounts, // ✅ Kirimkan daftar akun
                onGenerateReport = { startDate, endDate, unitUsaha ->
                    transactionViewModel.generateReport(startDate, endDate, unitUsaha)
                },
                onNavigateUp = { navController.popBackStack() }
            )
        }


composable("neraca_screen") {
    // Kita bisa gunakan TransactionViewModel karena datanya sudah ada di sana
    val viewModel: TransactionViewModel = viewModel(factory = transactionViewModelFactory)
    val neracaData by viewModel.neracaData.collectAsStateWithLifecycle()

    NeracaScreen(
        neracaData = neracaData,
        onNavigateUp = { navController.popBackStack() }
    )
}
}
}