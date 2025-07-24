package com.dony.bumdesku.data

// Kelas ini tidak memerlukan anotasi @Entity karena hanya digunakan untuk
// menampung state sementara di dalam UI (ViewModel), bukan untuk disimpan di database.
data class CartItem(
    val asset: Asset, // Item yang dijual, diambil dari daftar aset/inventaris
    var quantity: Int     // Jumlah item yang dibeli
)