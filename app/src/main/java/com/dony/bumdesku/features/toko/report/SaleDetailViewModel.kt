package com.dony.bumdesku.features.toko.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.CartItem
import com.dony.bumdesku.data.Sale
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SaleDetailViewModel : ViewModel() {

    private val _saleDetail = MutableStateFlow<Sale?>(null)
    val saleDetail: StateFlow<Sale?> = _saleDetail.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    fun loadSale(sale: Sale) {
        viewModelScope.launch {
            _saleDetail.value = sale
            // Parse JSON string dari sale.itemsJson menjadi List<CartItem>
            val gson = Gson()
            val type = object : TypeToken<List<CartItem>>() {}.type
            val items: List<CartItem> = gson.fromJson(sale.itemsJson, type)
            _cartItems.value = items
        }
    }
}