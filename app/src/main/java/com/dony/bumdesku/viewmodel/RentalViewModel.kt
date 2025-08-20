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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.dony.bumdesku.util.BluetoothPrinterService

data class RentalUiState(
    val isLoading: Boolean = true,
    val rentalItems: List<RentalItem> = emptyList(),
    val activeTransactions: List<RentalTransaction> = emptyList(),
    val completedTransactions: List<RentalTransaction> = emptyList(),
    val errorMessage: String? = null,
    val rentedStockMap: Map<String, Int> = emptyMap()
)

sealed class RentalSaveState {
    object IDLE : RentalSaveState()
    object LOADING : RentalSaveState()
    object SUCCESS : RentalSaveState()
    data class ERROR(val message: String) : RentalSaveState()
}

class RentalViewModel(
    private val rentalRepository: RentalRepository,
    private val authViewModel: AuthViewModel,
    private val bluetoothPrinterService: BluetoothPrinterService
) : ViewModel() {

    private val _availabilityState = MutableStateFlow<Result<Int>?>(null)
    val availabilityState: StateFlow<Result<Int>?> = _availabilityState

    private val _dueTransactions = MutableStateFlow<List<RentalTransaction>>(emptyList())
    val dueTransactions: StateFlow<List<RentalTransaction>> = _dueTransactions.asStateFlow()

    private val activeUnitUsaha = authViewModel.activeUnitUsaha
    private val _saveState = MutableStateFlow<RentalSaveState>(RentalSaveState.IDLE)
    val saveState: StateFlow<RentalSaveState> = _saveState

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<RentalUiState> = activeUnitUsaha.flatMapLatest { unit ->
        Log.d("RentalViewModel", "Unit usaha aktif berubah, ID: ${unit?.id}")

        if (unit == null) {
            flowOf(RentalUiState(isLoading = false))
        } else {
            combine(
                rentalRepository.getRentalItems(unit.id),
                rentalRepository.getRentalTransactions(unit.id)
            ) { items, transactions ->
                Log.d("RentalViewModel", "Data diterima dari Room: ${items.size} item, ${transactions.size} transaksi")

                // --- Perbaikan di sini ---
                // Pisahkan transaksi aktif dari yang sudah selesai
                val active = transactions.filter { it.status == "Disewa" || it.status == "Dipesan" }
                val completed = transactions.filter { it.status == "Selesai" }

                // Cek transaksi yang jatuh tempo
                val due = active.filter { it.isOverdue() || it.isDueSoon() }
                _dueTransactions.value = due // Perbarui StateFlow notifikasi

                val rentedStockMap = items.associate { item ->
                    val rentedQty = active
                        .filter { it.rentalItemId == item.id }
                        .sumOf { it.quantity }
                    item.id to rentedQty
                }
                // --- Akhir perbaikan ---

                RentalUiState(
                    isLoading = false,
                    rentalItems = items,
                    activeTransactions = active,
                    completedTransactions = completed,
                    rentedStockMap = rentedStockMap
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RentalUiState(isLoading = true)
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

    fun getRentalTransactionById(id: String): Flow<RentalTransaction?> {
        return rentalRepository.getRentalTransactionById(id)
    }

    fun createRental(transaction: RentalTransaction) {
        viewModelScope.launch {
            _saveState.value = RentalSaveState.LOADING
            try {
                rentalRepository.processNewRental(transaction)
                _saveState.value = RentalSaveState.SUCCESS
            } catch (e: Exception) {
                _saveState.value = RentalSaveState.ERROR(e.message ?: "Terjadi kesalahan")
            }
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
        damageCost: Double,
        notes: String
    ) = viewModelScope.launch {
        _saveState.value = RentalSaveState.LOADING
        try {
            rentalRepository.processReturn(transaction, returnedConditions, damageCost, notes)
            _saveState.value = RentalSaveState.SUCCESS
        } catch (e: Exception) {
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
            _saveState.value = RentalSaveState.ERROR(e.message ?: "Gagal memproses perbaikan.")
        }
    }

    fun resetSaveState() {
        _saveState.value = RentalSaveState.IDLE
    }

    fun getCurrentUserId(): String {
        return authViewModel.userProfile.value?.uid ?: ""
    }

    fun buildRentalReceiptText(transaction: RentalTransaction): String {
        val unitUsahaName = activeUnitUsaha.value?.name ?: "BUMDesku"
        val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

        val builder = StringBuilder()
        val esc: Char = 27.toChar()
        val gs: Char = 29.toChar()
        val initPrinter = byteArrayOf(esc.code.toByte(), 64)
        val alignCenter = byteArrayOf(esc.code.toByte(), 97, 1)
        val alignLeft = byteArrayOf(esc.code.toByte(), 97, 0)
        val boldOn = byteArrayOf(esc.code.toByte(), 69, 1)
        val boldOff = byteArrayOf(esc.code.toByte(), 69, 0)

        builder.append(String(initPrinter))
        builder.append(String(alignCenter))
        builder.append(String(boldOn))
        builder.append("$unitUsahaName - Jasa Sewa\n")
        builder.append(String(boldOff))
        builder.append("--------------------------------\n")
        builder.append(String(alignLeft))
        builder.append("Tgl: ${dateFormat.format(Date(transaction.returnDate ?: 0L))}\n")
        builder.append("Penyewa: ${transaction.customerName}\n")
        builder.append("--------------------------------\n")
        builder.append("Barang: ${transaction.itemName}\n")
        builder.append("Jumlah: ${transaction.quantity}\n")
        builder.append("Sewa dari: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.rentalDate))}\n")
        builder.append("Selesai pada: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.returnDate ?: 0L))}\n")
        builder.append("--------------------------------\n")
        builder.append("Total Biaya: ${currencyFormat.format(transaction.totalPrice)}\\n")
        if (transaction.notesOnReturn.isNotBlank()) {
            builder.append("Catatan: ${transaction.notesOnReturn}\n")
        }
        builder.append("--------------------------------\n\n")
        builder.append(String(alignCenter))
        builder.append("Terima kasih!\n\n\n\n")

        return builder.toString()
    }
}