package com.dony.bumdesku.data

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val role: String = "pengurus",
    // TAMBAHKAN BARIS INI untuk memperbaiki error
    val managedUnitUsahaIds: List<String> = emptyList()
)