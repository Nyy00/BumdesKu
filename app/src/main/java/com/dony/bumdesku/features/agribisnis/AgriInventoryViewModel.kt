package com.dony.bumdesku.features.agribisnis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.AgriInventory
import com.dony.bumdesku.repository.AgriRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AgriInventoryViewModel(
    private val agriRepository: AgriRepository
) : ViewModel() {

    val allAgriInventory: Flow<List<AgriInventory>> = agriRepository.allAgriInventory

    fun getInventoryById(id: String): Flow<AgriInventory?> {
        return agriRepository.getInventoryById(id)
    }

    fun insert(inventory: AgriInventory) = viewModelScope.launch {
        agriRepository.insert(inventory)
    }

    fun update(inventory: AgriInventory) = viewModelScope.launch {
        agriRepository.update(inventory)
    }
}