package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Enum untuk merepresentasikan state dari proses otentikasi
// PASTIKAN INI BERADA DI LUAR CLASS
enum class AuthState {
    IDLE,       // Keadaan awal
    LOADING,    // Sedang dalam proses
    SUCCESS,    // Berhasil
    ERROR       // Terjadi kesalahan
}

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth

    private val _authState = MutableStateFlow(AuthState.IDLE)
    val authState: StateFlow<AuthState> = _authState

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun registerUser(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.LOADING
            try {
                auth.createUserWithEmailAndPassword(email, pass).await()
                _authState.value = AuthState.SUCCESS
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _authState.value = AuthState.ERROR
            }
        }
    }

    fun loginUser(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.LOADING
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                _authState.value = AuthState.SUCCESS
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _authState.value = AuthState.ERROR
            }
        }
    }

    // Fungsi untuk mereset state setelah proses selesai
    fun resetAuthState() {
        _authState.value = AuthState.IDLE
        _errorMessage.value = null
    }
}