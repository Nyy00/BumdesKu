package com.dony.bumdesku.features.toko.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dony.bumdesku.repository.PosRepository

class SalesReportViewModelFactory(
    private val posRepository: PosRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SalesReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SalesReportViewModel(posRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}