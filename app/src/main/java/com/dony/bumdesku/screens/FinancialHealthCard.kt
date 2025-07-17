package com.dony.bumdesku.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dony.bumdesku.data.FinancialHealthData
import com.dony.bumdesku.data.HealthStatus

@Composable
fun FinancialHealthCard(data: FinancialHealthData) {
    val statusColor = when(data.status) {
        HealthStatus.SEHAT -> Color(0xFF2E7D32) // Hijau Tua
        HealthStatus.WASPADA -> Color(0xFFF9A825) // Kuning Tua
        HealthStatus.TIDAK_SEHAT -> Color(0xFFC62828) // Merah Tua
        HealthStatus.TIDAK_TERDEFINISI -> Color.Gray
    }

    val statusText = when(data.status) {
        HealthStatus.SEHAT -> "Sehat"
        HealthStatus.WASPADA -> "Waspada"
        HealthStatus.TIDAK_SEHAT -> "Tidak Sehat"
        HealthStatus.TIDAK_TERDEFINISI -> "Data Kurang"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Kesehatan Keuangan", color = Color.White)
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                String.format("%.2f : 1", data.currentRatio),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}