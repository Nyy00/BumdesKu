package com.dony.bumdesku.features.agribisnis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dony.bumdesku.repository.AgriRepository

class AgriInventoryViewModelFactory(
    private val agriRepository: AgriRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AgriInventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AgriInventoryViewModel(agriRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}