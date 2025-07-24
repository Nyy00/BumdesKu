package com.dony.bumdesku.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.Asset
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.repository.AssetRepository
import com.dony.bumdesku.repository.UnitUsahaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class UploadState { IDLE, UPLOADING, SUCCESS, ERROR }

class AssetViewModel(
    private val repository: AssetRepository,
    private val unitUsahaRepository: UnitUsahaRepository
) : ViewModel() {

    private val _selectedUnitFilter = MutableStateFlow<UnitUsaha?>(null)
    val selectedUnitFilter: StateFlow<UnitUsaha?> = _selectedUnitFilter.asStateFlow()

    val filteredAssets: StateFlow<List<Asset>> = combine(
        repository.allAssets,
        _selectedUnitFilter
    ) { assets, selectedUnit ->
        if (selectedUnit == null) {
            assets
        } else {
            assets.filter { it.unitUsahaId == selectedUnit.id }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUnitUsaha: Flow<List<UnitUsaha>> = unitUsahaRepository.allUnitUsaha

    val allCategories: StateFlow<List<String>> = repository.allAssets
        .map { assets -> assets.map { it.category }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uploadState = MutableStateFlow(UploadState.IDLE)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    /**
     * ✅ PERBAIKAN UTAMA DI SINI
     * Ubah parameter 'id' dari Int menjadi String agar sesuai dengan
     * perubahan yang telah kita buat di DAO dan MainActivity.
     */
    fun getAssetById(id: String): Flow<Asset?> { // <-- PASTIKAN PARAMETERNYA ADALAH STRING
        return repository.getAssetById(id)
    }

    fun selectUnitFilter(unitUsaha: UnitUsaha?) {
        _selectedUnitFilter.value = unitUsaha
    }

    fun insert(asset: Asset, imageUri: Uri?) {
        viewModelScope.launch {
            _uploadState.value = UploadState.UPLOADING
            try {
                repository.insert(asset, imageUri)
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

class AssetViewModelFactory(
    private val repository: AssetRepository,
    private val unitUsahaRepository: UnitUsahaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AssetViewModel(repository, unitUsahaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}