package com.dony.bumdesku.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dony.bumdesku.data.FinancialHealthData
import com.dony.bumdesku.viewmodel.DebtSummary
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    userRole: String,
    financialHealthData: FinancialHealthData,
    debtSummary: DebtSummary // ✅ 1. Terima data ringkasan
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BUMDesKu Menu Utama") },
                actions = {
                    IconButton(onClick = { onNavigate("profile") }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profil"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp) // Hanya padding horizontal di sini
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(0.dp)) // Spacer untuk padding atas

            FinancialHealthCard(data = financialHealthData)

            // ✅ 3. Tampilkan kartu ringkasan yang baru
            DebtSummaryCard(summary = debtSummary, onNavigate = onNavigate)

            // --- Menu Laporan (Semua Peran) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Assessment,
                    title = "Laba Rugi",
                    onClick = { onNavigate("report_screen") }
                )
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AccountBalance,
                    title = "Neraca",
                    onClick = { onNavigate("neraca_screen") }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Balance,
                    title = "Neraca Saldo",
                    onClick = { onNavigate("neraca_saldo_screen") }
                )
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.TrendingUp,
                    title = "Perubahan Modal",
                    onClick = { onNavigate("lpe_screen") }
                )
            }

            // --- Menu Operasional (Semua Peran) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.FormatListBulleted,
                    title = "Buku Besar",
                    onClick = { onNavigate("transaction_list") }
                )
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "Daftar Akun (COA)",
                    onClick = { onNavigate("account_list") }
                )
            }

            // --- Menu Manajemen (Hanya untuk PENGURUS) ---
            if (userRole == "pengurus") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AddCircle,
                        title = "Input Jurnal",
                        onClick = { onNavigate("add_transaction") }
                    )
                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Store,
                        title = "Unit Usaha",
                        onClick = { onNavigate("unit_usaha_management") }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Inventory,
                        title = "Manajemen Aset",
                        onClick = { onNavigate("asset_list") }
                    )
                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Lock,
                        title = "Kunci Jurnal",
                        onClick = { onNavigate("lock_journal") }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.VerticalAlignBottom,
                        title = "Utang Usaha",
                        onClick = { onNavigate("payable_list") }
                    )
                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.VerticalAlignTop,
                        title = "Piutang Usaha",
                        onClick = { onNavigate("receivable_list") }
                    )
                }
            }
            Spacer(modifier = Modifier.height(0.dp)) // Spacer untuk padding bawah
        }
    }
}

// ✅ 2. Buat Composable baru untuk kartu ringkasan
@Composable
fun DebtSummaryCard(summary: DebtSummary, onNavigate: (String) -> Unit) {
    val localeID = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Ringkasan Utang & Piutang", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("payable_list") }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Utang Belum Dibayar:")
                Text(
                    text = currencyFormat.format(summary.totalPayable),
                    color = Color.Red,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Divider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("receivable_list") }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Piutang Belum Diterima:")
                Text(
                    text = currencyFormat.format(summary.totalReceivable),
                    color = Color(0xFF008800),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


// Composable FeatureCard tidak perlu diubah
@Composable
fun FeatureCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 2
            )
        }
    }
}
