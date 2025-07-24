package com.dony.bumdesku.features.toko.report

import androidx.lifecycle.ViewModel
import com.dony.bumdesku.data.Sale
import com.dony.bumdesku.repository.PosRepository
import kotlinx.coroutines.flow.Flow

class SalesReportViewModel(
    private val posRepository: PosRepository
) : ViewModel() {
    val salesHistory: Flow<List<Sale>> = posRepository.getAllSales()
}