package com.dony.bumdesku.features.agribisnis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dony.bumdesku.repository.AgriRepository

class AgriViewModelFactory(
    private val agriRepository: AgriRepository
    // ✅ unitUsahaRepository dihapus dari sini
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AgriViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // ✅ Hanya berikan agriRepository saat membuat ViewModel
            return AgriViewModel(agriRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}