package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // State untuk mengetahui status login pengguna
    private val _userLoggedIn = MutableStateFlow(auth.currentUser != null)
    val userLoggedIn: StateFlow<Boolean> = _userLoggedIn

    // State untuk menampilkan pesan error atau loading
    private val _authStatus = MutableStateFlow<AuthStatus>(AuthStatus.Idle)
    val authStatus: StateFlow<AuthStatus> = _authStatus

    fun signUp(email: String, pass: String) {
        _authStatus.value = AuthStatus.Loading
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                _userLoggedIn.value = true
                _authStatus.value = AuthStatus.Success("Registrasi berhasil!")
            } else {
                _authStatus.value = AuthStatus.Error(task.exception?.message ?: "Registrasi gagal")
            }
        }
    }

    fun signIn(email: String, pass: String) {
        _authStatus.value = AuthStatus.Loading
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                _userLoggedIn.value = true
                _authStatus.value = AuthStatus.Success("Login berhasil!")
            } else {
                _authStatus.value = AuthStatus.Error(task.exception?.message ?: "Login gagal")
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _userLoggedIn.value = false
    }

    // Fungsi untuk mereset status agar pesan tidak muncul terus
    fun resetAuthStatus() {
        _authStatus.value = AuthStatus.Idle
    }
}

// Sealed class untuk merepresentasikan status proses otentikasi
sealed class AuthStatus {
    object Idle : AuthStatus()
    object Loading : AuthStatus()
    data class Success(val message: String) : AuthStatus()
    data class Error(val message: String) : AuthStatus()
}