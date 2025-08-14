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

sealed class RentalSaveState {
    object IDLE : RentalSaveState()
    object LOADING : RentalSaveState()
    object SUCCESS : RentalSaveState()
    data class ERROR(val message: String) : RentalSaveState()
}

class RentalViewModel(
    private val rentalRepository: RentalRepository,
    private val authViewModel: AuthViewModel
) : ViewModel() {

    private val _availabilityState = MutableStateFlow<Result<Int>?>(null)
    val availabilityState: StateFlow<Result<Int>?> = _availabilityState

    private val activeUnitUsaha = authViewModel.activeUnitUsaha
    private val _saveState = MutableStateFlow<RentalSaveState>(RentalSaveState.IDLE)
    val saveState: StateFlow<RentalSaveState> = _saveState

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<RentalUiState> = activeUnitUsaha.flatMapLatest { unit ->
        Log.d("RentalViewModel", "Unit usaha aktif berubah, ID: ${unit?.id}")

        if (unit == null) {
            // Jika tidak ada unit aktif, tampilkan state kosong
            flowOf(RentalUiState(isLoading = false))
        } else {
            // Langsung ambil data dari Room DB.
            combine(
                rentalRepository.getRentalItems(unit.id),
                rentalRepository.getRentalTransactions(unit.id)
            ) { items, transactions ->
                Log.d("RentalViewModel", "Data diterima dari Room: ${items.size} item, ${transactions.size} transaksi")
                val active = transactions.filter { it.status == "Disewa" || it.status == "Dipesan" }
                val completed = transactions.filter { it.status == "Selesai" }
                RentalUiState(
                    isLoading = false,
                    rentalItems = items,
                    activeTransactions = active,
                    completedTransactions = completed
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RentalUiState(isLoading = true) // Set isLoading jadi true di awal
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
            // FIX: Pass the error message
            _saveState.value = RentalSaveState.ERROR(e.message ?: "Gagal menyimpan item.")
        }
    }

    fun checkItemAvailability(itemId: String, unitUsahaId: String, startDate: Long, endDate: Long) {
        viewModelScope.launch {
            try {
                _availabilityState.value = null
                val availableStock = rentalRepository.checkAvailability(itemId, unitUsahaId, startDate, endDate)
                _availabilityState.value = Result.success(availableStock)
            } catch (e: Exception) {
                _availabilityState.value = Result.failure(e)
            }
        }
    }

    fun clearAvailabilityCheck() {
        _availabilityState.value = null
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
            // FIX: Pass the error message
            _saveState.value = RentalSaveState.ERROR(e.message ?: "Gagal membuat transaksi.")
        }
    }

    fun deleteItem(item: RentalItem) = viewModelScope.launch {
        try {
            rentalRepository.deleteItem(item)
        } catch (e: Exception) {
            // This is just a log, so it's fine, but we can also push a state error if needed.
            Log.e("RentalViewModel", "Gagal menghapus item: ${e.message}")
        }
    }

    fun completeRental(
        transaction: RentalTransaction,
        returnedConditions: Map<String, Int>,
        damageCost: Double, // Parameter from our previous implementation
        notes: String
    ) = viewModelScope.launch {
        _saveState.value = RentalSaveState.LOADING
        try {
            // Make sure to pass all required parameters to the repository
            rentalRepository.processReturn(transaction, returnedConditions, damageCost, notes)
            _saveState.value = RentalSaveState.SUCCESS
        } catch (e: Exception) {
            // FIX: Pass the error message
            _saveState.value = RentalSaveState.ERROR(e.message ?: "Gagal menyelesaikan transaksi.")
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
            // FIX: Pass the error message
            _saveState.value = RentalSaveState.ERROR(e.message ?: "Gagal memproses perbaikan.")
        }
    }

    fun resetSaveState() {
        _saveState.value = RentalSaveState.IDLE
    }
}