package com.dony.bumdesku.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.Asset
import com.dony.bumdesku.repository.AssetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// DEFINISI ENUM YANG HILANG
enum class UploadState { IDLE, UPLOADING, SUCCESS, ERROR }

class AssetViewModel(private val repository: AssetRepository) : ViewModel() {

    val allAssets: Flow<List<Asset>> = repository.allAssets

    // DEFINISI STATEFLOW YANG HILANG
    private val _uploadState = MutableStateFlow(UploadState.IDLE)
    val uploadState: StateFlow<UploadState> = _uploadState

    fun insert(asset: Asset, imageUri: Uri?) {
        viewModelScope.launch {
            _uploadState.value = UploadState.UPLOADING
            try {
                // Panggil repository.insert, semua logika ada di sana
                repository.insert(asset, imageUri)
                _uploadState.value = UploadState.SUCCESS
            } catch (e: Exception) {
                // 'Log' sekarang akan dikenali karena sudah di-import
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