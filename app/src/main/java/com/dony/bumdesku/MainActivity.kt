package com.dony.bumdesku

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.dony.bumdesku.screens.*
import com.dony.bumdesku.ui.theme.BumdesKuTheme
import com.dony.bumdesku.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Factory untuk TransactionViewModel
        val transactionDb = BumdesDatabase.getDatabase(this)
        val transactionRepo = TransactionRepository(transactionDb.transactionDao())
        val unitUsahaRepo = UnitUsahaRepository(transactionDb.unitUsahaDao())
        val transactionFactory = TransactionViewModelFactory(transactionRepo, unitUsahaRepo)

        // Factory untuk AuthViewModel
        val authFactory = AuthViewModelFactory()

        setContent {
            BumdesKuTheme {
                BumdesApp(
                    transactionViewModelFactory = transactionFactory,
                    authViewModelFactory = authFactory
                )
            }
        }
    }
}

@Composable
fun BumdesApp(
    transactionViewModelFactory: TransactionViewModelFactory,
    authViewModelFactory: AuthViewModelFactory
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
    val transactionViewModel: TransactionViewModel = viewModel(factory = transactionViewModelFactory)

    val isLoggedIn by authViewModel.userLoggedIn.collectAsStateWithLifecycle()
    val authStatus by authViewModel.authStatus.collectAsStateWithLifecycle()

    // Menampilkan pesan Toast untuk status login/register
    LaunchedEffect(authStatus) {
        when (val status = authStatus) {
            is AuthStatus.Success -> Toast.makeText(navController.context, status.message, Toast.LENGTH_SHORT).show()
            is AuthStatus.Error -> Toast.makeText(navController.context, status.message, Toast.LENGTH_SHORT).show()
            else -> {}
        }
        if (authStatus !is AuthStatus.Idle) {
            authViewModel.resetAuthStatus()
        }
    }

    // Tentukan halaman awal berdasarkan status login
    val startDestination = if (isLoggedIn) "home" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        // --- RUTE OTENTIKASI ---
        composable("login") {
            LoginScreen(
                onLoginClick = { email, pass -> authViewModel.signIn(email, pass) },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterClick = { email, pass -> authViewModel.signUp(email, pass) },
                onNavigateToLogin = { navController.navigate("login") }
            )
        }

        // --- RUTE APLIKASI UTAMA ---
        composable("home") {
            HomeScreen(
                onNavigate = { route -> navController.navigate(route) },
                onLogoutClick = {
                    authViewModel.signOut()
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            )
        }
        composable("transaction_list") {
            val transactions by transactionViewModel.allTransactions.collectAsStateWithLifecycle(emptyList())
            val dashboardData by transactionViewModel.dashboardData.collectAsStateWithLifecycle()
            TransactionListScreen(
                transactions = transactions,
                dashboardData = dashboardData,
                onAddItemClick = { navController.navigate("add_transaction") },
                onItemClick = { transactionId -> navController.navigate("edit_transaction/$transactionId") },
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
            if (transactionId != null) {
                val transactionToEdit by transactionViewModel.getTransactionById(transactionId)
                    .collectAsStateWithLifecycle(initialValue = null)
                transactionToEdit?.let { transaction ->
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
        composable("unit_usaha_management") {
            val unitUsahaList by transactionViewModel.allUnitUsaha.collectAsStateWithLifecycle(emptyList())
            UnitUsahaManagementScreen(
                unitUsahaList = unitUsahaList,
                onAddUnitUsaha = { unitName -> transactionViewModel.insert(UnitUsaha(name = unitName)) },
                onDeleteUnitUsaha = { unitUsaha -> transactionViewModel.delete(unitUsaha) },
                onNavigateUp = { navController.popBackStack() }
            )
        }
        composable("report_screen") {
            val reportData by transactionViewModel.reportData.collectAsStateWithLifecycle()
            val reportTransactions by transactionViewModel.reportTransactions.collectAsStateWithLifecycle()
            ReportScreen(
                reportData = reportData,
                reportTransactions = reportTransactions,
                onGenerateReport = { startDate, endDate -> transactionViewModel.generateReport(startDate, endDate) },
                onNavigateUp = { navController.popBackStack() },
                onItemClick = { transactionId -> navController.navigate("edit_transaction/$transactionId") }
            )
        }
    }
}