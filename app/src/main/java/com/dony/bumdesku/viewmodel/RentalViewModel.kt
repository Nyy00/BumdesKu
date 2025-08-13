package com.dony.bumdesku.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.RentalItem
import com.dony.bumdesku.data.RentalTransaction
import com.dony.bumdesku.repository.RentalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RentalUiState(
    val isLoading: Boolean = true,
    val rentalItems: List<RentalItem> = emptyList(),
    val activeTransactions: List<RentalTransaction> = emptyList(),
    val completedTransactions: List<RentalTransaction> = emptyList(),
    val errorMessage: String? = null
)

enum class RentalSaveState {
    IDLE, LOADING, SUCCESS, ERROR
}

class RentalViewModel(
    private val rentalRepository: RentalRepository,
    private val authViewModel: AuthViewModel
) : ViewModel() {

    private val activeUnitUsaha = authViewModel.activeUnitUsaha
    private val _saveState = MutableStateFlow(RentalSaveState.IDLE)
    val saveState: StateFlow<RentalSaveState> = _saveState

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<RentalUiState> = activeUnitUsaha.flatMapLatest { unit ->
        if (unit == null) {
            flowOf(RentalUiState(isLoading = false))
        } else {
            combine(
                rentalRepository.getRentalItems(unit.id),
                rentalRepository.getRentalTransactions(unit.id)
            ) { items, transactions ->
                RentalUiState(
                    isLoading = false,
                    rentalItems = items,
                    activeTransactions = transactions.filter { it.status == "Disewa" },
                    completedTransactions = transactions.filter { it.status == "Selesai" }
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RentalUiState()
    )

    fun saveItem(item: RentalItem) = viewModelScope.launch {
        _saveState.value = RentalSaveState.LOADING
        try {
            val unitId = activeUnitUsaha.value?.id
            if (unitId != null) {
                rentalRepository.saveItem(item.copy(unitUsahaId = unitId))
                _saveState.value = RentalSaveState.SUCCESS
            } else {
                throw IllegalStateException("Unit Usaha tidak aktif.")
            }
        } catch (e: Exception) {
            _saveState.value = RentalSaveState.ERROR
        }
    }

    fun createRental(transaction: RentalTransaction) = viewModelScope.launch {
        _saveState.value = RentalSaveState.LOADING
        try {
            val unitId = activeUnitUsaha.value?.id
            if (unitId != null) {
                rentalRepository.processNewRental(transaction.copy(unitUsahaId = unitId))
                _saveState.value = RentalSaveState.SUCCESS
            } else {
                throw IllegalStateException("Unit Usaha tidak aktif.")
            }
        } catch (e: Exception) {
            _saveState.value = RentalSaveState.ERROR
        }
    }

    fun deleteItem(item: RentalItem) = viewModelScope.launch {
        try {
            rentalRepository.deleteItem(item)
        } catch (e: Exception) {
            Log.e("RentalViewModel", "Gagal menghapus item: ${e.message}")
        }
    }

    fun completeRental(
        transaction: RentalTransaction,
        returnedConditions: Map<String, Int>,
        notes: String
    ) = viewModelScope.launch {
        _saveState.value = RentalSaveState.LOADING
        try {
            rentalRepository.processReturn(transaction, returnedConditions, notes)
            _saveState.value = RentalSaveState.SUCCESS
        } catch (e: Exception) {
            _saveState.value = RentalSaveState.ERROR
        }
    }

    fun repairItem(
        item: RentalItem,
        quantity: Int,
        fromCondition: String,
        repairCost: Double
    ) = viewModelScope.launch {
        _saveState.value = RentalSaveState.LOADING
        try {
            rentalRepository.processItemRepair(item, quantity, fromCondition, repairCost)
            _saveState.value = RentalSaveState.SUCCESS
        } catch (e: Exception) {
            _saveState.value = RentalSaveState.ERROR
        }
    }

    fun resetSaveState() {
        _saveState.value = RentalSaveState.IDLE
    }
}
