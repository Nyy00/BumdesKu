package com.dony.bumdesku.data

data class CartItem(
    val asset: Asset, // Item yang dijual, diambil dari daftar aset/inventaris
    var quantity: Int     // Jumlah item yang dibeli
)