package com.dony.bumdesku.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.RentalItem
import com.dony.bumdesku.data.RentalTransaction
import com.dony.bumdesku.repository.RentalRepository
import com.dony.bumdesku.repository.CustomerRepository
import com.dony.bumdesku.data.Customer
import com.dony.bumdesku.data.PaymentStatus
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
    val customers: List<Customer> = emptyList(),
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
    private val customerRepository: CustomerRepository,
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
                customerRepository.getCustomers(unit.id),
                rentalRepository.getRentalTransactions(unit.id)
            ) { items, customers, transactions ->
                Log.d(
                    "RentalViewModel",
                    "Data diterima: ${items.size} item, ${customers.size} pelanggan, ${transactions.size} transaksi"
                )

                val active = transactions.filter { it.status == "Disewa" || it.status == "Dipesan" }
                val completed = transactions.filter { it.status == "Selesai" }

                val due = active.filter { it.isOverdue() || it.isDueSoon() }
                _dueTransactions.value = due

                val rentedStockMap = items.associate { item ->
                    val rentedQty = active
                        .filter { it.rentalItemId == item.id }
                        .sumOf { it.quantity }
                    item.id to rentedQty
                }

                RentalUiState(
                    isLoading = false,
                    rentalItems = items,
                    customers = customers,
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

    fun saveCustomer(customer: Customer) = viewModelScope.launch {
        _saveState.value = RentalSaveState.LOADING
        try {
            val unitId = activeUnitUsaha.value?.id
            if (unitId != null) {
                customerRepository.saveCustomer(customer.copy(unitUsahaId = unitId))
                _saveState.value = RentalSaveState.SUCCESS
            } else {
                throw IllegalStateException("Unit Usaha tidak aktif.")
            }
        } catch (e: Exception) {
            _saveState.value = RentalSaveState.ERROR(e.message ?: "Gagal menyimpan pelanggan.")
        }
    }

    fun deleteCustomer(customer: Customer) = viewModelScope.launch {
        _saveState.value = RentalSaveState.LOADING
        try {
            customerRepository.deleteCustomer(customer)
            _saveState.value = RentalSaveState.SUCCESS
        } catch (e: Exception) {
            _saveState.value = RentalSaveState.ERROR(e.message ?: "Gagal menghapus pelanggan.")
        }
    }

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
                val availableStock =
                    rentalRepository.checkAvailability(itemId, unitUsahaId, startDate, endDate)
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

    fun getTransactionsForCustomer(customerId: String): Flow<List<RentalTransaction>> {
        return rentalRepository.getTransactionsForCustomer(customerId)
    }

    // Perbaikan: Fungsi ini sekarang hanya menerima satu parameter, yaitu objek RentalTransaction
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

    fun processPayment(transactionId: String, paymentAmount: Double) = viewModelScope.launch {
        _saveState.value = RentalSaveState.LOADING
        try {
            rentalRepository.processPayment(transactionId, paymentAmount)
            _saveState.value = RentalSaveState.SUCCESS
        } catch (e: Exception) {
            _saveState.value = RentalSaveState.ERROR(e.message ?: "Gagal memproses pembayaran.")
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
        notes: String,
        remainingPayment: Double
    ) = viewModelScope.launch {
        _saveState.value = RentalSaveState.LOADING
        try {
            rentalRepository.processReturn(
                transaction,
                returnedConditions,
                damageCost,
                notes,
                remainingPayment
            )
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
        val receiptWidth = 32 // Lebar struk untuk printer 58mm
        val unitUsahaName = activeUnitUsaha.value?.name ?: "BUMDes Jangkang"

        // Fungsi bantuan untuk membuat baris dengan teks kiri dan kanan
        fun createRow(left: String, right: String): String {
            val spaces = receiptWidth - left.length - right.length
            return left + " ".repeat(kotlin.math.max(0, spaces)) + right
        }

        // Fungsi untuk memformat mata uang tanpa "Rp"
        fun formatCurrencyValue(value: Double): String {
            val format = NumberFormat.getNumberInstance(Locale("in", "ID"))
            return format.format(value.toLong())
        }

        val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
        val simpleDateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

        val builder = StringBuilder()
        val esc: Char = 27.toChar()
        val initPrinter = byteArrayOf(esc.code.toByte(), 64)
        val alignCenter = byteArrayOf(esc.code.toByte(), 97, 1)
        val alignLeft = byteArrayOf(esc.code.toByte(), 97, 0)
        val boldOn = byteArrayOf(esc.code.toByte(), 69, 1)
        val boldOff = byteArrayOf(esc.code.toByte(), 69, 0)

        builder.append(String(initPrinter))
        builder.append(String(alignCenter))
        builder.append(String(boldOn))
        builder.append("$unitUsahaName\n")
        builder.append(String(boldOff))
        builder.append("BUKTI SEWA\n\n")

        builder.append(String(alignLeft))
        val transactionId = transaction.id
        builder.append(createRow("No:", transactionId.take(8).uppercase()))
        builder.append("\n")
        val receiptDate = transaction.returnDate ?: transaction.rentalDate
        builder.append(createRow("Tgl:", simpleDateFormat.format(Date(receiptDate))))
        builder.append("\n")
        builder.append(createRow("Penyewa:", transaction.customerName))
        builder.append("\n")

        builder.append("-".repeat(receiptWidth)).append("\n")
        builder.append("DETAIL SEWA\n")
        builder.append(createRow("Barang:", transaction.itemName))
        builder.append("\n")
        builder.append(createRow("Jumlah:", transaction.quantity.toString()))
        builder.append("\n")
        builder.append(createRow("Harga/Hari:", formatCurrencyValue(transaction.pricePerDay)))
        builder.append("\n\n")

        val tglSewa = simpleDateFormat.format(Date(transaction.rentalDate))
        val tglKembali = transaction.returnDate?.let { simpleDateFormat.format(Date(it)) } ?: "-"
        builder.append(createRow("Tgl Sewa:", tglSewa))
        builder.append("\n")
        builder.append(createRow("Tgl Kembali:", tglKembali))
        builder.append("\n")

        builder.append("-".repeat(receiptWidth)).append("\n")

        val isCompletedOrPaid = transaction.status == "Selesai" || transaction.paymentStatus == PaymentStatus.LUNAS

        if (isCompletedOrPaid) {
            builder.append(createRow("Total Dibayar:", formatCurrencyValue(transaction.downPayment)))
            builder.append("\n")
        } else {
            builder.append(createRow("Total Biaya:", formatCurrencyValue(transaction.totalPrice)))
            builder.append("\n")
            builder.append(createRow("Uang Muka (DP):", formatCurrencyValue(transaction.downPayment)))
            builder.append("\n")
            val sisa = transaction.totalPrice - transaction.downPayment
            if (sisa > 0.01) {
                builder.append(createRow("Sisa Bayar:", formatCurrencyValue(sisa)))
                builder.append("\n")
            }
        }

        builder.append(String(boldOn))
        builder.append(createRow("TOTAL BIAYA SEWA:", formatCurrencyValue(transaction.totalPrice)))
        builder.append("\n")
        builder.append(String(boldOff))

        if (transaction.notesOnReturn.isNotBlank()) {
            builder.append("-".repeat(receiptWidth)).append("\n")
            builder.append("Catatan:\n${transaction.notesOnReturn}\n")
        }

        builder.append("\n\n")
        builder.append(String(alignCenter))
        builder.append("Terima kasih!\n\n\n\n")

        return builder.toString()
    }
}