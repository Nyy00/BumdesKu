package com.dony.bumdesku.features.agribisnis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AgriSaleDetailViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AgriSaleDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AgriSaleDetailViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}