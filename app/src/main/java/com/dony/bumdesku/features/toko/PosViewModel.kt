package com.dony.bumdesku.features.toko

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.*
import com.dony.bumdesku.repository.AssetRepository
import com.dony.bumdesku.repository.PosRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException

enum class SaleState {
    IDLE, LOADING, SUCCESS, ERROR
}

@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModel(
    private val assetRepository: AssetRepository,
    private val posRepository: PosRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // --- State Baru untuk Logika Kategori ---
    private val _selectedCategory = MutableStateFlow("Semua") // Default ke "Semua"
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // State ini akan berisi daftar kategori unik dari semua produk.
    val productCategories: StateFlow<List<String>> = assetRepository.allAssets
        .map { assets ->
            // Tambahkan "Semua" di awal daftar, lalu kategori unik lainnya
            listOf("Semua") + assets.map { it.category }.distinct().sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Semua"))
    // ---------------------------------------


    // Daftar produk sekarang difilter berdasarkan KATEGORI dan PENCARIAN
    val filteredProducts: StateFlow<List<Asset>> = combine(
        assetRepository.allAssets,
        _searchQuery,
        _selectedCategory
    ) { products, query, category ->
        products.filter { asset ->
            val categoryMatch = category == "Semua" || asset.category == category
            val queryMatch = query.isBlank() || asset.name.contains(query, ignoreCase = true)
            categoryMatch && queryMatch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _totalPrice = MutableStateFlow(0.0)
    val totalPrice: StateFlow<Double> = _totalPrice.asStateFlow()

    private val _saleState = MutableStateFlow(SaleState.IDLE)
    val saleState: StateFlow<SaleState> = _saleState.asStateFlow()

    val userProfile = MutableStateFlow<UserProfile?>(null)
    val activeUnitUsaha = MutableStateFlow<UnitUsaha?>(null)


    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    // --- Fungsi Baru untuk Memilih Kategori ---
    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }
    // ----------------------------------------

    fun addToCart(product: Asset) {
        viewModelScope.launch {
            val currentCart = _cartItems.value
            val existingItem = currentCart.find { it.asset.id == product.id }

            if (existingItem != null) {
                updateQuantity(existingItem, existingItem.quantity + 1)
            } else {
                if (product.quantity > 0) {
                    val newCart = currentCart + CartItem(asset = product, quantity = 1)
                    _cartItems.value = newCart
                    updateTotalPrice()
                }
            }
        }
    }

    fun removeFromCart(cartItem: CartItem) {
        viewModelScope.launch {
            val newCart = _cartItems.value.toMutableList().apply { remove(cartItem) }
            _cartItems.value = newCart
            updateTotalPrice()
        }
    }

    fun updateQuantity(item: CartItem, newQuantity: Int) {
        viewModelScope.launch {
            if (newQuantity <= 0) {
                removeFromCart(item)
                return@launch
            }

            val currentCart = _cartItems.value
            val index = currentCart.indexOfFirst { it.asset.id == item.asset.id }

            if (index != -1 && newQuantity <= item.asset.quantity) {
                val updatedItem = currentCart[index].copy(quantity = newQuantity)
                val newCart = currentCart.toMutableList().apply { set(index, updatedItem) }
                _cartItems.value = newCart
                updateTotalPrice()
            }
        }
    }

    private fun updateTotalPrice() {
        val total = _cartItems.value.sumOf { it.asset.sellingPrice * it.quantity }
        _totalPrice.value = total
    }

    fun completeSale() {
        viewModelScope.launch {
            // Ambil data dari StateFlow internal
            val currentUser = userProfile.value
            val currentUnitUsaha = activeUnitUsaha.value
            val currentCart = _cartItems.value
            val currentTotalPrice = _totalPrice.value

            if (currentCart.isEmpty() || currentUser == null || currentUnitUsaha == null) {
                _saleState.value = SaleState.ERROR
                Log.e("PosViewModel", "Data penjualan tidak lengkap untuk diproses.")
                return@launch
            }

            _saleState.value = SaleState.LOADING
            try {
                // Panggil repository dengan data yang valid
                posRepository.processSale(currentCart, currentTotalPrice, currentUser, currentUnitUsaha)

                _cartItems.value = emptyList()
                updateTotalPrice()
                _saleState.value = SaleState.SUCCESS
            } catch (e: IOException) { // Tangani error stok tidak cukup
                _saleState.value = SaleState.ERROR
                Log.e("PosViewModel", "Gagal menyelesaikan penjualan: ${e.message}", e)
                // Di sini Anda bisa meneruskan e.message ke UI untuk ditampilkan
            } catch (e: Exception) { // Tangani error umum lainnya
                _saleState.value = SaleState.ERROR
                Log.e("PosViewModel", "Gagal menyelesaikan penjualan: ${e.message}", e)
            }
        }
    }

    fun resetSaleState() {
        _saleState.value = SaleState.IDLE
    }
}