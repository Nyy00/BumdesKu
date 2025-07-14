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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dony.bumdesku.data.BumdesDatabase
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.repository.TransactionRepository
import com.dony.bumdesku.repository.UnitUsahaRepository
import com.dony.bumdesku.ui.theme.BumdesKuTheme
import com.dony.bumdesku.viewmodel.AuthViewModel
import com.dony.bumdesku.viewmodel.AuthViewModelFactory
import com.dony.bumdesku.viewmodel.TransactionViewModel
import com.dony.bumdesku.viewmodel.TransactionViewModelFactory
import com.dony.bumdesku.data.AssetDao // import baru
import com.dony.bumdesku.repository.AssetRepository // import baru
import com.dony.bumdesku.viewmodel.AssetViewModel // import baru
import com.dony.bumdesku.viewmodel.AssetViewModelFactory // import baru
import com.dony.bumdesku.screens.AssetListScreen // import baru
import com.dony.bumdesku.screens.AddAssetScreen // import baru
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.dony.bumdesku.viewmodel.AuthState

// Import semua screen Anda dari package 'screens'
import com.dony.bumdesku.screens.*


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = BumdesDatabase.getDatabase(this)

        val transactionDao = database.transactionDao()
        val unitUsahaDao = database.unitUsahaDao()

        val transactionRepository = TransactionRepository(transactionDao, unitUsahaDao)
        val unitUsahaRepository = UnitUsahaRepository(unitUsahaDao)

        val transactionViewModelFactory = TransactionViewModelFactory(transactionRepository, unitUsahaRepository)
        val authViewModelFactory = AuthViewModelFactory()

        val assetDao = database.assetDao()
        val assetRepository = AssetRepository(assetDao)
        val assetViewModelFactory = AssetViewModelFactory(assetRepository)

        setContent {
            BumdesKuTheme {
                BumdesApp(
                    transactionViewModelFactory = transactionViewModelFactory,
                    authViewModelFactory = authViewModelFactory,
                    assetViewModelFactory = assetViewModelFactory
                )
            }
        }
    }
}

@Composable
fun BumdesApp(
    transactionViewModelFactory: TransactionViewModelFactory,
    authViewModelFactory: AuthViewModelFactory,
    assetViewModelFactory: AssetViewModelFactory
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
                        Toast.makeText(context, errorMessage ?: "Login Gagal", Toast.LENGTH_SHORT).show()
                        authViewModel.resetAuthState()
                    }
                    else -> {}
                }
            }
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
                        Toast.makeText(context, "Registrasi berhasil, silakan login", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                        authViewModel.resetAuthState()
                    }
                    AuthState.ERROR -> {
                        Toast.makeText(context, errorMessage ?: "Registrasi Gagal", Toast.LENGTH_SHORT).show()
                        authViewModel.resetAuthState()
                    }
                    else -> {}
                }
            }
        }

        composable("home") {
            HomeScreen(
                onNavigate = { route -> navController.navigate(route) },
                onLogout = {
                    auth.signOut()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("transaction_list") {
            val transactionViewModel: TransactionViewModel = viewModel(factory = transactionViewModelFactory)
            val transactions by transactionViewModel.allTransactions.collectAsStateWithLifecycle(emptyList())
            val dashboardData by transactionViewModel.dashboardData.collectAsStateWithLifecycle()

            TransactionListScreen(
                transactions = transactions,
                dashboardData = dashboardData,
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
            val transactionViewModel: TransactionViewModel = viewModel(factory = transactionViewModelFactory)
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
            val transactionViewModel: TransactionViewModel = viewModel(factory = transactionViewModelFactory)
            val transactionId = backStackEntry.arguments?.getInt("transactionId")
            if (transactionId != null) {
                val transactionToEdit by transactionViewModel.getTransactionById(transactionId)
                    .collectAsStateWithLifecycle(initialValue = null)
                transactionToEdit?.let { transaction ->
                    AddTransactionScreen(
                        viewModel = transactionViewModel,
                        transactionToEdit = transaction,
                        onSave = { updatedTransaction ->
                            transactionViewModel.update(updatedTransaction)
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
                onSave = { asset ->
                    assetViewModel.insert(asset)
                    navController.popBackStack()
                },
                onNavigateUp = { navController.popBackStack() }
            )
        }


        composable("unit_usaha_management") {
            val transactionViewModel: TransactionViewModel = viewModel(factory = transactionViewModelFactory)
            val unitUsahaList by transactionViewModel.allUnitUsaha.collectAsStateWithLifecycle(emptyList())
            UnitUsahaManagementScreen(
                unitUsahaList = unitUsahaList,
                onAddUnitUsaha = { unitName -> transactionViewModel.insert(UnitUsaha(name = unitName)) },
                onDeleteUnitUsaha = { unitUsaha -> transactionViewModel.delete(unitUsaha) },
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable("report_screen") {
            val transactionViewModel: TransactionViewModel = viewModel(factory = transactionViewModelFactory)
            val allTransactions by transactionViewModel.allTransactions.collectAsStateWithLifecycle(emptyList())
            val reportData by transactionViewModel.reportData.collectAsStateWithLifecycle()

            ReportScreen(
                reportData = reportData,
                reportTransactions = allTransactions.filter { it.date >= reportData.startDate && it.date <= reportData.endDate },
                onGenerateReport = { startDate, endDate ->
                    transactionViewModel.generateReport(startDate, endDate)
                },
                onNavigateUp = { navController.popBackStack() },
                onItemClick = { transaction ->
                    navController.navigate("edit_transaction/${transaction.localId}")
                }
            )
        }
    }
}