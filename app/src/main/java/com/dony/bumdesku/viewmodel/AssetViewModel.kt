package com.dony.bumdesku.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.Asset
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.repository.AssetRepository
import com.dony.bumdesku.repository.AgriRepository
import com.dony.bumdesku.repository.RentalRepository
import com.dony.bumdesku.repository.UnitUsahaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class UploadState { IDLE, UPLOADING, SUCCESS, ERROR }

class AssetViewModel(
    private val repository: AssetRepository,
    private val unitUsahaRepository: UnitUsahaRepository,
    private val agriRepository: AgriRepository,
    private val rentalRepository: RentalRepository
) : ViewModel() {

    private val _selectedUnitFilter = MutableStateFlow<UnitUsaha?>(null)
    val selectedUnitFilter: StateFlow<UnitUsaha?> = _selectedUnitFilter.asStateFlow()

    val combinedAssets: StateFlow<List<Asset>> = combine(
        repository.allAssets,
        agriRepository.allAgriInventory,
        rentalRepository.allRentalItems,
        _selectedUnitFilter
    ) { assets, agriInventory, rentalItems, selectedUnit ->
        val agriAsAssets = agriInventory.map { inv ->
            Asset(
                id = inv.id,
                userId = inv.userId,
                unitUsahaId = inv.unitUsahaId,
                name = inv.name,
                description = "Dibeli pada ${Date(inv.purchaseDate)}",
                quantity = inv.quantity.toInt(),
                purchasePrice = inv.cost / if (inv.quantity > 0) inv.quantity else 1.0,
                sellingPrice = 0.0,
                imageUrl = "",
                category = "Inventaris Agribisnis"
            )
        }

        val rentalAsAssets = rentalItems.map { item ->
            Asset(
                id = item.id,
                userId = "",
                unitUsahaId = item.unitUsahaId,
                name = item.name,
                description = item.description,
                quantity = item.getTotalStock(),
                purchasePrice = 0.0,
                sellingPrice = item.rentalPricePerDay,
                imageUrl = "",
                category = "Aset Jasa Sewa"
            )
        }

        val allItems = assets + agriAsAssets + rentalAsAssets

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

    fun insert(asset: Asset) {
        viewModelScope.launch {
            _uploadState.value = UploadState.UPLOADING
            try {
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
        if (asset.category == "Inventaris Agribisnis") {
            // TODO: Logika hapus agri_inventory
        } else if (asset.category == "Aset Jasa Sewa") {
            // TODO: Logika hapus rental_item jika diperlukan
        }
        else {
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
    private val agriRepository: AgriRepository,
    private val rentalRepository: RentalRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AssetViewModel(repository, unitUsahaRepository, agriRepository, rentalRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}