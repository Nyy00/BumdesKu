package com.dony.bumdesku.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    userRole: String // ✅ Tambahkan kembali parameter ini
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Menu yang hanya bisa dilihat oleh PENGURUS
            if (userRole == "pengurus") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AddCircle,
                        title = "Tambah Transaksi",
                        onClick = { onNavigate("add_transaction") }
                    )
                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Inventory,
                        title = "Manajemen Aset",
                        onClick = { onNavigate("asset_list") }
                    )
                }
            }

            // Menu yang bisa dilihat oleh SEMUA PERAN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.FormatListBulleted,
                    title = "Lihat Transaksi",
                    onClick = { onNavigate("transaction_list") }
                )
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Assessment,
                    title = "Laporan",
                    onClick = { onNavigate("report_screen") }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Store,
                    title = "Unit Usaha",
                    onClick = { onNavigate("unit_usaha_management") }
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun FeatureCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f) // Membuat kartu menjadi persegi
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}