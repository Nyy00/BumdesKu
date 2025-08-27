package com.dony.bumdesku.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dony.bumdesku.data.FinancialHealthData
import com.dony.bumdesku.data.UnitUsahaType
import com.dony.bumdesku.viewmodel.DebtSummary
import java.text.NumberFormat
import java.util.*

data class QuickAction(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val isManagerOnly: Boolean = false,
    val isWriteAction: Boolean = false
)

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    userRole: String,
    financialHealthData: FinancialHealthData,
    debtSummary: DebtSummary,
    activeUnitUsahaType: UnitUsahaType = UnitUsahaType.UMUM
) {
    val baseActions = listOf(
        QuickAction("Input Jurnal", Icons.Default.AddCircleOutline, "add_transaction", isWriteAction = true),
        QuickAction("Aset Tetap", Icons.Default.Business, "fixed_asset_list"),
        QuickAction("Buku Besar", Icons.AutoMirrored.Filled.MenuBook, "transaction_list"),
        QuickAction("Laba Rugi", Icons.Default.Assessment, "report_screen"),
        QuickAction("Aset & Stok", Icons.Default.Inventory, "asset_list", isWriteAction = true),
        QuickAction("Utang", Icons.Default.ArrowDownward, "payable_list", isWriteAction = true),
        QuickAction("Piutang", Icons.Default.ArrowUpward, "receivable_list", isWriteAction = true),
        QuickAction("Neraca Saldo", Icons.Default.Balance, "neraca_saldo_screen")
    )

    val unitSpecificActions = when (activeUnitUsahaType) {
        UnitUsahaType.TOKO -> listOf(
            QuickAction("Kasir (POS)", Icons.Default.PointOfSale, "pos_screen", isWriteAction = true),
            QuickAction("Laporan Penjualan", Icons.Default.Summarize, "sales_report")
        )
        UnitUsahaType.AGRIBISNIS -> listOf(
            QuickAction("Catat Panen", Icons.Default.AddBusiness, "add_harvest", isWriteAction = true),
            QuickAction("Stok Panen", Icons.AutoMirrored.Filled.ListAlt, "harvest_list"),
            QuickAction("Siklus Produksi", Icons.Default.Autorenew, "production_cycle_list", isWriteAction = true),
            QuickAction("Jual Hasil", Icons.Default.ShoppingCart, "produce_sale", isWriteAction = true),
            QuickAction("Inventaris Agri", Icons.Default.Inventory2, "agri_inventory_list", isWriteAction = true),
            QuickAction("Laporan Penjualan", Icons.Default.Summarize, "agri_sale_report")
        )
        UnitUsahaType.JASA_SEWA -> listOf(
            QuickAction("Dasbor Sewa", Icons.Default.EventSeat, "rental_dashboard", isWriteAction = true),
            QuickAction("Tambah Barang", Icons.Default.AddBusiness, "add_rental_item", isWriteAction = true),
            QuickAction("Riwayat Sewa", Icons.Default.History, "rental_history_screen"),
            QuickAction("Manajemen Pelanggan", Icons.Default.PeopleAlt, "rental/customers", isWriteAction = true)
        )
        else -> emptyList()
    }

    val managerActions = listOf(
        QuickAction("Unit Usaha", Icons.Default.Store, "unit_usaha_management", isManagerOnly = true, isWriteAction = true),
        QuickAction("Daftar Akun", Icons.Default.AccountBalanceWallet, "account_list", isManagerOnly = true, isWriteAction = true),
        QuickAction("Kunci Jurnal", Icons.Default.Lock, "lock_journal", isManagerOnly = true, isWriteAction = true),
        QuickAction("Neraca", Icons.Default.AccountBalance, "neraca_screen", isManagerOnly = true),
        QuickAction("Perubahan Modal", Icons.Default.TrendingUp, "lpe_screen", isManagerOnly = true)
    )

    val filteredBaseActions = if (activeUnitUsahaType == UnitUsahaType.AGRIBISNIS) {
        baseActions.filter { it.route != "asset_list" }
    } else {
        baseActions
    }

    val allActions = filteredBaseActions + unitSpecificActions + managerActions

    val availableActions = when (userRole) {
        "manager" -> allActions
        "pengurus" -> allActions.filter { !it.isManagerOnly }
        "auditor" -> allActions.filter { !it.isManagerOnly && !it.isWriteAction }
        else -> emptyList()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            HomeHeader(onNavigate)
            BalanceCard(financialHealthData)
            DebtSummaryInfo(summary = debtSummary, onNavigate = onNavigate)
            QuickActionsGrid(actions = availableActions, onNavigate = onNavigate)
        }
    }
}

@Composable
fun HomeHeader(onNavigate: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Selamat Datang,",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Text(
                text = "Pengelola BUMDes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        IconButton(onClick = { onNavigate("profile") }) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profil",
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun BalanceCard(data: FinancialHealthData) {
    val localeID = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply {
        maximumFractionDigits = 0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(180.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF004E92), Color(0xFF00294D))
                    )
                )
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text(
                    "Total Laba Bersih",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    currencyFormat.format(data.labaRugi),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Column {
                        Text("Pendapatan", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                        Text(currencyFormat.format(data.totalPendapatan), color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Column {
                        Text("Beban", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                        Text(currencyFormat.format(data.totalBeban), color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun DebtSummaryInfo(summary: DebtSummary, onNavigate: (String) -> Unit) {
    val localeID = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onNavigate("payable_list") }
        ) {
            Text("Total Utang", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            Text(
                text = currencyFormat.format(summary.totalPayable),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onNavigate("receivable_list") }
        ) {
            Text("Total Piutang", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            Text(
                text = currencyFormat.format(summary.totalReceivable),
                color = Color(0xFF008800),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun QuickActionsGrid(actions: List<QuickAction>, onNavigate: (String) -> Unit) {
    val gridHeight = (((actions.size - 1) / 4 + 1) * 110).dp

    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(
            "Menu & Fitur",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.height(gridHeight),
            userScrollEnabled = false
        ) {
            items(actions) { action ->
                QuickActionItem(action = action, onClick = { onNavigate(action.route) })
            }
        }
    }
}

@Composable
fun QuickActionItem(action: QuickAction, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .shadow(elevation = 4.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = action.title,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}