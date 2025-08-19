package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dony.bumdesku.repository.RentalRepository
import com.dony.bumdesku.util.BluetoothPrinterService

class RentalViewModelFactory(
    private val rentalRepository: RentalRepository,
    private val authViewModel: AuthViewModel,
    private val bluetoothPrinterService: BluetoothPrinterService // ✅ Tambahkan parameter ini
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RentalViewModel::class.java)) {
            // ✅ Berikan semua dependensi saat membuat ViewModel
            return RentalViewModel(rentalRepository, authViewModel, bluetoothPrinterService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}