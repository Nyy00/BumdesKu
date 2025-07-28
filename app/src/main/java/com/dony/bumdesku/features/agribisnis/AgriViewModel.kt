package com.dony.bumdesku.features.agribisnis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.Harvest
import com.dony.bumdesku.repository.AgriRepository
import com.dony.bumdesku.repository.UnitUsahaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AgriViewModel(
    private val agriRepository: AgriRepository,
    private val unitUsahaRepository: UnitUsahaRepository
) : ViewModel() {

    val allHarvests: Flow<List<Harvest>> = agriRepository.allHarvests

    fun insert(harvest: Harvest) = viewModelScope.launch {
        agriRepository.insert(harvest)
    }

    // Fungsi-fungsi lain untuk logika UI akan kita tambahkan di sini nanti
}