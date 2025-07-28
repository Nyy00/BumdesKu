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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi Database dan Repositori
        val database = BumdesDatabase.getDatabase(this)
        val transactionDao = database.transactionDao()
        val unitUsahaDao = database.unitUsahaDao()
        val assetDao = database.assetDao()
        val accountDao = database.accountDao()
        val debtDao = database.debtDao()
        val saleDao = database.saleDao()
        val agriDao = database.agriDao()
        val cycleDao = database.cycleDao()

        val accountRepository = AccountRepository(accountDao)
        val unitUsahaRepository = UnitUsahaRepository(unitUsahaDao)
        val assetRepository = AssetRepository(assetDao)
        val transactionRepository = TransactionRepository(transactionDao)
        val debtRepository = DebtRepository(debtDao)
        val posRepository = PosRepository(saleDao, assetRepository, transactionRepository, accountRepository)
        val agriRepository = AgriRepository(agriDao, transactionRepository, accountRepository)
        val agriCycleRepository = AgriCycleRepository(cycleDao, transactionRepository)

        // Inisialisasi ViewModel Factories
        val transactionViewModelFactory = TransactionViewModelFactory(transactionRepository, unitUsahaRepository, accountRepository)
        val assetViewModelFactory = AssetViewModelFactory(assetRepository, unitUsahaRepository)
        val accountViewModelFactory = AccountViewModelFactory(accountRepository)
        val debtViewModelFactory = DebtViewModelFactory(debtRepository, transactionRepository, accountRepository, unitUsahaRepository)
        val posViewModelFactory = PosViewModelFactory(assetRepository, posRepository)
        val salesReportViewModelFactory = SalesReportViewModelFactory(posRepository)
        val saleDetailViewModelFactory = SaleDetailViewModelFactory()
        val agriViewModelFactory = AgriViewModelFactory(agriRepository)
        val agriCycleViewModelFactory = AgriCycleViewModelFactory(agriCycleRepository, accountRepository)

        val authViewModelFactory = AuthViewModelFactory(
            unitUsahaRepository,
            transactionRepository,
            assetRepository,
            posRepository,
            accountRepository,
            debtRepository,
            agriRepository,
            agriCycleRepository
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
                    agriViewModelFactory = agriViewModelFactory,
                    agriCycleViewModelFactory = agriCycleViewModelFactory
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
    agriCycleViewModelFactory: AgriCycleViewModelFactory
) {
    val navController = rememberNavController()
    val auth = Firebase.auth
    val context = LocalContext.current

    val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
    val transactionViewModel: TransactionViewModel = viewModel(factory = transactionViewModelFactory)

    val startDestination = if (auth.currentUser != null) "home" else "login"

    NavHost(navController = navController, startDestination = startDestination) {

        // ... (Rute lain seperti "home", "login", "transaction_list", dll tidak berubah)
        composable("home") {
            val debtViewModel: DebtViewModel = viewModel(factory = debtViewModelFactory)
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            val healthData by transactionViewModel.financialHealthData.collectAsStateWithLifecycle()
            val debtSummary by debtViewModel.debtSummary.collectAsStateWithLifecycle()
            val activeUnitUsaha by authViewModel.activeUnitUsaha.collectAsStateWithLifecycle()

            HomeScreen(
                userRole = userProfile?.role ?: "pengurus",
                financialHealthData = healthData,
                debtSummary = debtSummary,
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

            LaunchedEffect(authState) {
                if (authState == AuthState.ERROR) {
                    Toast.makeText(context, errorMessage ?: "Login Gagal", Toast.LENGTH_SHORT)
                        .show()
                    authViewModel.resetAuthState()
                }
            }
        }

        composable("unit_usaha_selection") {
            val unitUsahaList by authViewModel.userUnitUsahaList.collectAsStateWithLifecycle()

            UnitUsahaSelectionScreen(
                unitUsahaList = unitUsahaList,
                onUnitSelected = { selectedUnit ->
                    authViewModel.setActiveUnitUsaha(selectedUnit)
                    navController.navigate("home") {
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
                authViewModel = authViewModel,
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
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable("transaction_list") {
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
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId")
            val localContext = LocalContext.current

            if (transactionId != null) {
                val transactionToEdit by transactionViewModel.getTransactionById(transactionId)
                    .collectAsStateWithLifecycle(initialValue = null)

                LaunchedEffect(transactionToEdit) {
                    transactionToEdit?.let { transaction ->
                        if (transaction.isLocked) {
                            Toast.makeText(
                                localContext,
                                "Transaksi ini sudah dikunci dan tidak bisa diubah.",
                                Toast.LENGTH_LONG
                            ).show()
                            navController.popBackStack()
                        }
                    }
                }

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
            val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()

            val assetViewModel: AssetViewModel = viewModel(factory = assetViewModelFactory)

            AssetListScreen(
                viewModel = assetViewModel,
                userRole = userProfile?.role ?: "pengurus",
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
                userRole = userProfile?.role ?: "pengurus",
                activeUnitUsaha = activeUnitUsaha,
                onSaveComplete = { navController.popBackStack() },
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable(
            "edit_asset/{assetId}",
            arguments = listOf(navArgument("assetId") { type = NavType.StringType })
        ) { backStackEntry ->
            val assetId = backStackEntry.arguments?.getString("assetId")
            if (assetId != null) {
                val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
                val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
                val activeUnitUsaha by authViewModel.activeUnitUsaha.collectAsStateWithLifecycle()

                val assetViewModel: AssetViewModel = viewModel(factory = assetViewModelFactory)
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
            val reportData by transactionViewModel.reportData.collectAsState()
            val reportTransactions by transactionViewModel.filteredReportTransactions.collectAsState()
            val userProfile by authViewModel.userProfile.collectAsState()
            val unitUsahaList by transactionViewModel.allUnitUsaha.collectAsState(initial = emptyList())
            val allAccounts by transactionViewModel.allAccounts.collectAsState(initial = emptyList())

            ReportScreen(
                reportData = reportData,
                unitUsahaList = unitUsahaList,
                userRole = userProfile?.role ?: "pengurus",
                reportTransactions = reportTransactions,
                allAccounts = allAccounts,
                onGenerateReport = { startDate, endDate, unitUsaha ->
                    transactionViewModel.generateReport(startDate, endDate, unitUsaha)
                },
                onNavigateUp = { navController.popBackStack() }
            )
        }


        composable("neraca_screen") {
            val neracaData by transactionViewModel.neracaData.collectAsStateWithLifecycle()
            val allAccounts by transactionViewModel.allAccounts.collectAsState(initial = emptyList())

            NeracaScreen(
                neracaData = neracaData,
                onNavigateUp = { navController.popBackStack() },
                onAccountClick = { neracaItem ->
                    val targetAccount =
                        allAccounts.find { it.accountName == neracaItem.accountName }
                    if (targetAccount != null) {
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
            val lpeData by transactionViewModel.lpeData.collectAsStateWithLifecycle()

            LpeScreen(
                lpeData = lpeData,
                onGenerateLpe = { startDate, endDate ->
                    transactionViewModel.generateLpe(startDate, endDate)
                },
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable("receivable_list") {
            val debtViewModel: DebtViewModel = viewModel(factory = debtViewModelFactory)
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
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
                receivableToEdit = null,
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
            arguments = listOf(navArgument("receivableId") { type = NavType.StringType })
        ) { backStackEntry ->
            val debtViewModel: DebtViewModel = viewModel(factory = debtViewModelFactory)
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            val activeUnitUsaha by authViewModel.activeUnitUsaha.collectAsStateWithLifecycle()
            val receivableId = backStackEntry.arguments?.getString("receivableId")
            val receivableToEdit by debtViewModel.getReceivableById(receivableId ?: "").collectAsState(initial = null)

            if (receivableToEdit != null) {
                ReceivableEntryScreen(
                    receivableToEdit = receivableToEdit,
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
                        Toast.makeText(localContext, "Jurnal berhasil dikunci!", Toast.LENGTH_SHORT)
                            .show()
                    }
                },
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable("pos_screen") {
            val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            val activeUnitUsaha by authViewModel.activeUnitUsaha.collectAsStateWithLifecycle()
            val posViewModel: PosViewModel = viewModel(factory = posViewModelFactory)

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

        // --- Rute Fitur Agribisnis ---
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
            val agriCycleViewModel: AgriCycleViewModel = viewModel(factory = agriCycleViewModelFactory)
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            val activeUnitUsaha by authViewModel.activeUnitUsaha.collectAsStateWithLifecycle()
            AddHarvestScreen(
                viewModel = agriViewModel,
                cycleViewModel = agriCycleViewModel,
                userRole = userProfile?.role ?: "pengurus",
                activeUnitUsaha = activeUnitUsaha,
                onSaveComplete = { navController.popBackStack() },
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable("produce_sale") {
            val agriViewModel: AgriViewModel = viewModel(factory = agriViewModelFactory)
            val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
            val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
            val activeUnitUsaha by authViewModel.activeUnitUsaha.collectAsStateWithLifecycle()

            ProduceSaleScreen(
                viewModel = agriViewModel,
                userProfile = userProfile,
                activeUnitUsaha = activeUnitUsaha,
                onNavigateUp = { navController.popBackStack() },
                onSaleComplete = { navController.popBackStack() }
            )
        }

        composable("agri_sale_report") {
            val agriViewModel: AgriViewModel = viewModel(factory = agriViewModelFactory)
            AgriSaleReportScreen(
                viewModel = agriViewModel,
                onNavigateUp = { navController.popBackStack() }
            )
        }

        // --- Rute Fitur Siklus Produksi ---
        composable("production_cycle_list") {
            val agriCycleViewModel: AgriCycleViewModel = viewModel(factory = agriCycleViewModelFactory)
            val activeUnitUsaha by authViewModel.activeUnitUsaha.collectAsStateWithLifecycle()

            LaunchedEffect(activeUnitUsaha) {
                activeUnitUsaha?.id?.let {
                    agriCycleViewModel.setActiveUnit(it)
                }
            }

            ProductionCycleListScreen(
                viewModel = agriCycleViewModel,
                onNavigateUp = { navController.popBackStack() },
                onNavigateToCycleDetail = { cycleId ->
                    navController.navigate("cycle_detail/$cycleId")
                }
            )
        }

        composable(
            "cycle_detail/{cycleId}",
            arguments = listOf(navArgument("cycleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val cycleId = backStackEntry.arguments?.getString("cycleId")
            if (cycleId != null) {
                val agriCycleViewModel: AgriCycleViewModel = viewModel(factory = agriCycleViewModelFactory)

                LaunchedEffect(key1 = cycleId) {
                    agriCycleViewModel.getCycleDetails(cycleId)
                }

                CycleDetailScreen(
                    viewModel = agriCycleViewModel,
                    onNavigateUp = { navController.popBackStack() }
                )
            }
        }
    }
}
