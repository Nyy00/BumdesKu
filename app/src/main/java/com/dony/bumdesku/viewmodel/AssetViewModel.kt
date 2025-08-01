package com.dony.bumdesku.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.Asset
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.repository.AssetRepository
import com.dony.bumdesku.repository.AgriRepository // <-- 1. IMPORT BARU
import com.dony.bumdesku.repository.UnitUsahaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class UploadState { IDLE, UPLOADING, SUCCESS, ERROR }

class AssetViewModel(
    private val repository: AssetRepository,
    private val unitUsahaRepository: UnitUsahaRepository,
    private val agriRepository: AgriRepository // <-- 2. TAMBAHKAN REPOSITORY BARU
) : ViewModel() {

    private val _selectedUnitFilter = MutableStateFlow<UnitUsaha?>(null)
    val selectedUnitFilter: StateFlow<UnitUsaha?> = _selectedUnitFilter.asStateFlow()

    // <-- 3. GABUNGKAN DUA SUMBER DATA MENJADI SATU
    val combinedAssets: StateFlow<List<Asset>> = combine(
        repository.allAssets,
        agriRepository.allAgriInventory, // Ambil data inventaris agri
        _selectedUnitFilter
    ) { assets, agriInventory, selectedUnit ->
        // Konversi AgriInventory menjadi tipe data Asset agar bisa ditampilkan bersama
        val agriAsAssets = agriInventory.map { inv ->
            Asset(
                id = inv.id,
                userId = inv.userId,
                unitUsahaId = inv.unitUsahaId,
                name = inv.name,
                description = "Dibeli pada ${Date(inv.purchaseDate)}",
                quantity = inv.quantity.toInt(), // Konversi Double ke Int untuk tampilan
                purchasePrice = inv.cost / if(inv.quantity > 0) inv.quantity else 1.0, // Hitung harga satuan
                sellingPrice = 0.0, // Inventaris tidak punya harga jual langsung
                imageUrl = "", // Tidak ada gambar untuk inventaris
                category = "Inventaris Agribisnis"
            )
        }

        // Gabungkan kedua daftar
        val allItems = assets + agriAsAssets

        // Terapkan filter jika ada
        if (selectedUnit == null) {
            allItems
        } else {
            allItems.filter { it.unitUsahaId == selectedUnit.id }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val allUnitUsaha: Flow<List<UnitUsaha>> = unitUsahaRepository.allUnitUsaha

    val allCategories: StateFlow<List<String>> = repository.allAssets
        .map { assets -> assets.map { it.category }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uploadState = MutableStateFlow(UploadState.IDLE)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    fun getAssetById(id: String): Flow<Asset?> {
        return repository.getAssetById(id)
    }

    fun selectUnitFilter(unitUsaha: UnitUsaha?) {
        _selectedUnitFilter.value = unitUsaha
    }

    fun insert(asset: Asset) { // Hapus parameter imageUri
        viewModelScope.launch {
            _uploadState.value = UploadState.UPLOADING
            try {
                // Panggil repository tanpa imageUri
                repository.insert(asset)
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
        // Cek apakah ini inventaris atau aset biasa
        if (asset.category == "Inventaris Agribisnis") {
            // TODO: Tambahkan logika hapus untuk agri_inventory jika diperlukan
        } else {
            repository.delete(asset)
        }
    }

    fun resetUploadState() {
        _uploadState.value = UploadState.IDLE
    }
}

class AssetViewModelFactory(
    private val repository: AssetRepository,
    private val unitUsahaRepository: UnitUsahaRepository,
    private val agriRepository: AgriRepository // <-- 4. TAMBAHKAN DI FACTORY JUGA
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AssetViewModel(repository, unitUsahaRepository, agriRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}