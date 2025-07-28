package com.dony.bumdesku.features.agribisnis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dony.bumdesku.repository.AgriRepository
import com.dony.bumdesku.repository.UnitUsahaRepository

class AgriViewModelFactory(
    private val agriRepository: AgriRepository,
    private val unitUsahaRepository: UnitUsahaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AgriViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AgriViewModel(agriRepository, unitUsahaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}