package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.Asset
import com.dony.bumdesku.repository.AssetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AssetViewModel(private val repository: AssetRepository) : ViewModel() {

    val allAssets: Flow<List<Asset>> = repository.allAssets

    fun insert(asset: Asset) = viewModelScope.launch {
        repository.insert(asset)
    }

    fun update(asset: Asset) = viewModelScope.launch {
        repository.update(asset)
    }

    fun delete(asset: Asset) = viewModelScope.launch {
        repository.delete(asset)
    }
}

// Factory untuk membuat AssetViewModel
class AssetViewModelFactory(private val repository: AssetRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AssetViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}