package com.dony.bumdesku.features.agribisnis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.Harvest
import com.dony.bumdesku.data.ProduceSale
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.data.UserProfile
import com.dony.bumdesku.repository.AgriRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Data class untuk item di keranjang agribisnis
data class AgriCartItem(
    val harvest: Harvest,
    var quantity: Double // Gunakan Double karena panen bisa dalam desimal (cth: 1.5 Kg)
)

// Enum untuk status penjualan
enum class ProduceSaleState {
    IDLE, LOADING, SUCCESS, ERROR
}

class AgriViewModel(
    private val agriRepository: AgriRepository
) : ViewModel() {

    // --- State untuk Daftar Stok Panen & Laporan ---
    val allHarvests: Flow<List<Harvest>> = agriRepository.allHarvests
    val allProduceSales: Flow<List<ProduceSale>> = agriRepository.allProduceSales

    // --- StateFlows baru untuk fitur kasir ---
    private val _cartItems = MutableStateFlow<List<AgriCartItem>>(emptyList())
    val cartItems: StateFlow<List<AgriCartItem>> = _cartItems.asStateFlow()

    private val _totalPrice = MutableStateFlow(0.0)
    val totalPrice: StateFlow<Double> = _totalPrice.asStateFlow()

    private val _saleState = MutableStateFlow(ProduceSaleState.IDLE)
    val saleState: StateFlow<ProduceSaleState> = _saleState.asStateFlow()

    // --- Fungsi Logika untuk Keranjang Belanja ---

    fun addToCart(harvest: Harvest, quantity: Double) {
        viewModelScope.launch {
            if (quantity <= 0) return@launch

            val currentCart = _cartItems.value.toMutableList()
            val existingItem = currentCart.find { it.harvest.id == harvest.id }

            if (existingItem != null) {
                val newQuantity = existingItem.quantity + quantity
                if (newQuantity <= harvest.quantity) {
                    existingItem.quantity = newQuantity
                }
            } else {
                if (quantity <= harvest.quantity) {
                    currentCart.add(AgriCartItem(harvest = harvest, quantity = quantity))
                }
            }
            _cartItems.value = currentCart
            updateTotalPrice()
        }
    }

    fun removeFromCart(cartItem: AgriCartItem) {
        viewModelScope.launch {
            val newCart = _cartItems.value.toMutableList().apply { remove(cartItem) }
            _cartItems.value = newCart
            updateTotalPrice()
        }
    }

    private fun updateTotalPrice() {
        _totalPrice.value = _cartItems.value.sumOf { it.harvest.sellingPrice * it.quantity }
    }

    // --- Fungsi untuk memproses penjualan ---
    fun completeSale(user: UserProfile, activeUnitUsaha: UnitUsaha) {
        viewModelScope.launch {
            if (_cartItems.value.isEmpty()) return@launch

            _saleState.value = ProduceSaleState.LOADING
            try {
                agriRepository.processProduceSale(
                    cartItems = _cartItems.value,
                    totalPrice = _totalPrice.value,
                    user = user,
                    activeUnitUsaha = activeUnitUsaha
                )
                _cartItems.value = emptyList()
                updateTotalPrice()
                _saleState.value = ProduceSaleState.SUCCESS
            } catch (e: Exception) {
                _saleState.value = ProduceSaleState.ERROR
            }
        }
    }

    fun resetSaleState() {
        _saleState.value = ProduceSaleState.IDLE
    }

    // --- Fungsi untuk mencatat panen baru ---
    fun insert(harvest: Harvest) = viewModelScope.launch {
        agriRepository.insert(harvest)
    }
}