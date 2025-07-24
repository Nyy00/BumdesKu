package com.dony.bumdesku.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dony.bumdesku.data.UnitUsaha

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitUsahaSelectionScreen(
    unitUsahaList: List<UnitUsaha>,
    onUnitSelected: (UnitUsaha) -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pilih Unit Usaha") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout")
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
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Pilih unit usaha yang ingin Anda kelola saat ini.",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(unitUsahaList, key = { it.id }) { unit ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUnitSelected(unit) },
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(unit.name, fontWeight = FontWeight.Bold)
                            Text(unit.type.name.replace("_", " "), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}