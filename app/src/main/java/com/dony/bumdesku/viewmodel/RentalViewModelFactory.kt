package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dony.bumdesku.repository.RentalRepository
import com.dony.bumdesku.repository.CustomerRepository // Tambahkan import ini
import com.dony.bumdesku.util.BluetoothPrinterService

class RentalViewModelFactory(
    private val rentalRepository: RentalRepository,
    private val customerRepository: CustomerRepository, // Tambahkan parameter ini
    private val authViewModel: AuthViewModel,
    private val bluetoothPrinterService: BluetoothPrinterService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RentalViewModel::class.java)) {
            return RentalViewModel(
                rentalRepository = rentalRepository,
                customerRepository = customerRepository, // Teruskan ke RentalViewModel
                authViewModel = authViewModel,
                bluetoothPrinterService = bluetoothPrinterService,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}