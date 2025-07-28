package com.dony.bumdesku.features.agribisnis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.*
import com.dony.bumdesku.repository.AccountRepository
import com.dony.bumdesku.repository.AgriCycleRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AgriCycleViewModel(
    private val cycleRepository: AgriCycleRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _activeUnitUsahaId = MutableStateFlow<String?>(null)
    private val _selectedCycle = MutableStateFlow<ProductionCycle?>(null)
    val selectedCycle: StateFlow<ProductionCycle?> = _selectedCycle.asStateFlow()
    private val _cycleCosts = MutableStateFlow<List<CycleCost>>(emptyList())
    val cycleCosts: StateFlow<List<CycleCost>> = _cycleCosts.asStateFlow()

    private var cycleJob: Job? = null
    private var costsJob: Job? = null

    val costAccounts: StateFlow<List<Account>> = accountRepository.allAccounts
        .map { accounts -> accounts.filter { it.category == AccountCategory.BEBAN } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val productionCycles: StateFlow<List<ProductionCycle>> = _activeUnitUsahaId
        .filterNotNull()
        .flatMapLatest { unitId -> cycleRepository.getCyclesForUnit(unitId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setActiveUnit(unitUsahaId: String) {
        _activeUnitUsahaId.value = unitUsahaId
    }

    fun createProductionCycle(name: String, startDate: Long) {
        viewModelScope.launch {
            val unitId = _activeUnitUsahaId.value ?: return@launch
            val newCycle = ProductionCycle(
                name = name,
                startDate = startDate,
                unitUsahaId = unitId,
                status = CycleStatus.BERJALAN
            )
            cycleRepository.insertCycle(newCycle)
        }
    }

    // Fungsi addCostToCycle diperbarui untuk menerima unitUsahaId
    fun addCostToCycle(cycleId: String, unitUsahaId: String, description: String, amount: Double, costAccount: Account, paymentAccount: Account) {
        viewModelScope.launch {
            val newCost = CycleCost(
                cycleId = cycleId,
                unitUsahaId = unitUsahaId, // <-- Tambahkan unitUsahaId di sini
                date = System.currentTimeMillis(),
                description = description,
                amount = amount,
                costCategoryId = costAccount.id
            )
            // Di repository, ini akan membuat jurnal juga
            cycleRepository.insertCost(newCost, paymentAccount)
        }
    }

    fun getCycleDetails(cycleId: String) {
        cycleJob?.cancel()
        costsJob?.cancel()

        cycleJob = viewModelScope.launch {
            cycleRepository.getCycleById(cycleId).collect { cycle ->
                _selectedCycle.value = cycle
            }
        }

        costsJob = viewModelScope.launch {
            cycleRepository.getCostsForCycle(cycleId).collect { costs ->
                _cycleCosts.value = costs
            }
        }
    }

    fun finishProductionCycle(cycle: ProductionCycle, totalHarvest: Double) {
        viewModelScope.launch {
            cycleRepository.finishCycle(cycle, totalHarvest)
        }
    }

    /**
     * Fungsi baru untuk memanggil repository agar mengarsipkan siklus.
     */
    fun archiveCycle(cycle: ProductionCycle) {
        viewModelScope.launch {
            cycleRepository.archiveCycle(cycle)
        }
    }
}

class AgriCycleViewModelFactory(
    private val cycleRepository: AgriCycleRepository,
    private val accountRepository: AccountRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AgriCycleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AgriCycleViewModel(cycleRepository, accountRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
