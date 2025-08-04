package com.dony.bumdesku.viewmodel

import androidx.lifecycle.*
import com.dony.bumdesku.data.FixedAsset
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.repository.FixedAssetRepository
import com.dony.bumdesku.repository.UnitUsahaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log

// ✅ --- TAMBAHKAN ENUM INI ---
enum class FixedAssetUploadState { IDLE, LOADING, SUCCESS, ERROR }

class FixedAssetViewModel(
    private val fixedAssetRepository: FixedAssetRepository,
    private val unitUsahaRepository: UnitUsahaRepository
) : ViewModel() {

    private val _selectedUnitFilter = MutableStateFlow<UnitUsaha?>(null)
    val selectedUnitFilter: StateFlow<UnitUsaha?> = _selectedUnitFilter.asStateFlow()

    // ✅ --- TAMBAHKAN STATEFLOW BARU ---
    private val _uploadState = MutableStateFlow(FixedAssetUploadState.IDLE)
    val uploadState: StateFlow<FixedAssetUploadState> = _uploadState.asStateFlow()


    val allUnitUsaha: Flow<List<UnitUsaha>> = unitUsahaRepository.allUnitUsaha

    val filteredAssets: StateFlow<List<FixedAsset>> = combine(
        fixedAssetRepository.allAssets,
        _selectedUnitFilter
    ) { assets, selectedUnit ->
        if (selectedUnit == null) assets else assets.filter { it.unitUsahaId == selectedUnit.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    fun selectUnitFilter(unitUsaha: UnitUsaha?) {
        _selectedUnitFilter.value = unitUsaha
    }

    // ✅ --- PERBARUI FUNGSI INSERT ---
    fun insert(asset: FixedAsset) = viewModelScope.launch {
        _uploadState.value = FixedAssetUploadState.LOADING
        try {
            fixedAssetRepository.insert(asset)
            _uploadState.value = FixedAssetUploadState.SUCCESS
        } catch (e: Exception) {
            Log.e("FixedAssetViewModel", "Insert asset failed", e)
            _uploadState.value = FixedAssetUploadState.ERROR
        }
    }

    fun update(asset: FixedAsset) = viewModelScope.launch {
        // Anda juga bisa menambahkan state untuk update jika diperlukan
        fixedAssetRepository.update(asset)
    }

    fun delete(asset: FixedAsset) = viewModelScope.launch {
        fixedAssetRepository.delete(asset)
    }

    fun getAssetById(id: String): Flow<FixedAsset?> = fixedAssetRepository.getAssetById(id)

    // ✅ --- TAMBAHKAN FUNGSI RESET ---
    fun resetUploadState() {
        _uploadState.value = FixedAssetUploadState.IDLE
    }
}

// ... (Factory tidak berubah)
class FixedAssetViewModelFactory(
    private val fixedAssetRepository: FixedAssetRepository,
    private val unitUsahaRepository: UnitUsahaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FixedAssetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FixedAssetViewModel(fixedAssetRepository, unitUsahaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}