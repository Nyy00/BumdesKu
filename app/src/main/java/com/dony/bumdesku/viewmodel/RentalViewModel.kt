package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.RentalItem
import com.dony.bumdesku.data.RentalTransaction
import com.dony.bumdesku.repository.RentalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// --- Definisi State (Tidak Berubah) ---
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

// --- Perbaikan Utama di sini ---
class RentalViewModel(
    private val rentalRepository: RentalRepository,
    private val authViewModel: AuthViewModel // Mengambil AuthViewModel sebagai dependency
) : ViewModel() {

    // Mengambil StateFlow unit usaha yang aktif dari AuthViewModel
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
                rentalRepository.saveRentalItem(item.copy(unitUsahaId = unitId))
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
                rentalRepository.createRentalTransaction(transaction.copy(unitUsahaId = unitId))
                _saveState.value = RentalSaveState.SUCCESS
            } else {
                throw IllegalStateException("Unit Usaha tidak aktif.")
            }
        } catch (e: Exception) {
            _saveState.value = RentalSaveState.ERROR
        }
    }

    fun completeRental(transaction: RentalTransaction) = viewModelScope.launch {
        try {
            rentalRepository.completeRentalTransaction(transaction)
        } catch (e: Exception) {
            // Handle error jika ada
        }
    }

    fun resetSaveState() {
        _saveState.value = RentalSaveState.IDLE
    }
}