package com.dony.bumdesku.data

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val role: String = "pengurus" // Default role
)