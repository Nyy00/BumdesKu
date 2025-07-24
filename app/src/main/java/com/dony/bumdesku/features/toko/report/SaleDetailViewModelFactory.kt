package com.dony.bumdesku.features.toko.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SaleDetailViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SaleDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SaleDetailViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}