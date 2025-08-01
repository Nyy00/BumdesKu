package com.dony.bumdesku.features.agribisnis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.ProduceSale
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AgriSaleDetailViewModel : ViewModel() {

    private val _saleDetail = MutableStateFlow<ProduceSale?>(null)
    val saleDetail: StateFlow<ProduceSale?> = _saleDetail.asStateFlow()

    private val _cartItems = MutableStateFlow<List<AgriCartItem>>(emptyList())
    val cartItems: StateFlow<List<AgriCartItem>> = _cartItems.asStateFlow()

    fun loadSale(sale: ProduceSale) {
        viewModelScope.launch {
            _saleDetail.value = sale
            // Parse JSON dari sale.itemsJson menjadi List<AgriCartItem>
            val gson = Gson()
            val type = object : TypeToken<List<AgriCartItem>>() {}.type
            val items: List<AgriCartItem> = gson.fromJson(sale.itemsJson, type)
            _cartItems.value = items
        }
    }
}