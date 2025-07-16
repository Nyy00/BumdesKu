package com.dony.bumdesku.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dony.bumdesku.viewmodel.ChartData
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

@Composable
fun MonthlyBarChart(chartData: ChartData) {
    if (chartData.monthlyIncome.isEmpty() && chartData.monthlyExpenses.isEmpty()) {
        return
    }

    // ✅ Mengambil daftar bulan dari salah satu map (karena keduanya memiliki kunci yang sama dan terurut)
    val allMonths = chartData.monthlyIncome.keys.toList()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Grafik Keuangan Bulanan", style = MaterialTheme.typography.titleLarge)
            AndroidView(
                factory = { context ->
                    BarChart(context).apply {
                        description.isEnabled = false
                        legend.isEnabled = true
                        axisLeft.axisMinimum = 0f
                        axisRight.isEnabled = false
                        setDrawGridBackground(false)

                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            setDrawGridLines(false)
                            granularity = 1f
                            isGranularityEnabled = true
                            setCenterAxisLabels(true)
                            valueFormatter = IndexAxisValueFormatter(allMonths)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                update = { barChart ->
                    val incomeEntries = allMonths.mapIndexed { index, month ->
                        BarEntry(index.toFloat(), chartData.monthlyIncome[month] ?: 0f)
                    }
                    val expenseEntries = allMonths.mapIndexed { index, month ->
                        BarEntry(index.toFloat(), chartData.monthlyExpenses[month] ?: 0f)
                    }

                    val incomeDataSet = BarDataSet(incomeEntries, "Pemasukan").apply {
                        color = android.graphics.Color.rgb(100, 221, 23)
                        valueTextColor = android.graphics.Color.BLACK
                    }
                    val expenseDataSet = BarDataSet(expenseEntries, "Pengeluaran").apply {
                        color = android.graphics.Color.rgb(255, 69, 58)
                        valueTextColor = android.graphics.Color.BLACK
                    }

                    val barData = BarData(incomeDataSet, expenseDataSet)
                    val groupSpace = 0.3f
                    val barSpace = 0.05f
                    val barWidth = 0.3f
                    barData.barWidth = barWidth

                    barChart.data = barData
                    barChart.xAxis.axisMinimum = 0f
                    barChart.xAxis.axisMaximum = allMonths.size.toFloat()
                    barChart.groupBars(0f, groupSpace, barSpace)
                    barChart.invalidate()
                }
            )
        }
    }
}