package com.dony.bumdesku.features.toko

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dony.bumdesku.repository.AssetRepository
import com.dony.bumdesku.repository.PosRepository // <-- Import baru

class PosViewModelFactory(
    private val assetRepository: AssetRepository,
    private val posRepository: PosRepository // <-- Ubah parameter di sini
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PosViewModel(
                assetRepository,
                posRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}