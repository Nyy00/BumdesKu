package com.dony.bumdesku.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.Asset
import com.dony.bumdesku.repository.AssetRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class UploadState { IDLE, UPLOADING, SUCCESS, ERROR }

class AssetViewModel(private val repository: AssetRepository) : ViewModel() {

    val allAssets: Flow<List<Asset>> = repository.allAssets

    // --- STATE BARU UNTUK DAFTAR KATEGORI ---
    // StateFlow ini akan otomatis berisi daftar kategori yang unik dari semua aset
    val allCategories: StateFlow<List<String>> = allAssets
        .map { assets -> assets.map { it.category }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    // ------------------------------------------

    private val _uploadState = MutableStateFlow(UploadState.IDLE)
    val uploadState: StateFlow<UploadState> = _uploadState

    fun getAssetById(id: Int): Flow<Asset?> {
        return repository.getAssetById(id)
    }

    fun insert(asset: Asset, imageUri: Uri?) {
        viewModelScope.launch {
            _uploadState.value = UploadState.UPLOADING
            try {
                val newAsset = asset.copy(id = UUID.randomUUID().toString())
                repository.insert(newAsset, imageUri)
                _uploadState.value = UploadState.SUCCESS
            } catch (e: Exception) {
                Log.e("AssetViewModel", "Insert asset failed", e)
                _uploadState.value = UploadState.ERROR
            }
        }
    }

    fun update(asset: Asset) = viewModelScope.launch {
        repository.update(asset)
    }

    fun delete(asset: Asset) = viewModelScope.launch {
        repository.delete(asset)
    }

    fun resetUploadState() {
        _uploadState.value = UploadState.IDLE
    }
}

class AssetViewModelFactory(private val repository: AssetRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AssetViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}