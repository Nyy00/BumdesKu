package com.dony.bumdesku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.dony.bumdesku.data.UserProfile
import com.google.firebase.firestore.ktx.firestore
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
    private val firestore = Firebase.firestore

    private val _authState = MutableStateFlow(AuthState.IDLE)
    val authState: StateFlow<AuthState> = _authState

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        // Saat ViewModel dibuat, langsung cek apakah ada pengguna yang sudah login
        // Jika ada, ambil profilnya.
        auth.currentUser?.uid?.let { fetchUserProfile(it) }
    }

    private fun fetchUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").document(userId).get().await()
                val profile = snapshot.toObject(UserProfile::class.java)?.copy(uid = userId)
                _userProfile.value = profile
            } catch (e: Exception) {
                // Gagal mengambil profil, mungkin karena offline
                _errorMessage.value = "Gagal memuat profil pengguna."
            }
        }
    }

    fun registerUser(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.LOADING
            try {
                // 1. Buat pengguna di Firebase Authentication
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                val userId = result.user?.uid

                if (userId != null) {
                    // 2. Buat dokumen profil untuk pengguna di Firestore
                    val userProfile = hashMapOf(
                        "email" to email,
                        "role" to "pengurus" // Peran default saat mendaftar
                    )
                    firestore.collection("users").document(userId).set(userProfile).await()
                }

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
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                // Setelah berhasil login, ambil profilnya
                result.user?.uid?.let { fetchUserProfile(it) }
                _authState.value = AuthState.SUCCESS
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _authState.value = AuthState.ERROR
            }
        }
    }

    fun changePassword(onResult: (Boolean, String) -> Unit) {
        val email = auth.currentUser?.email
        if (email != null) {
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, "Link untuk ubah password telah dikirim ke email Anda.")
                    } else {
                        onResult(false, task.exception?.message ?: "Gagal mengirim link.")
                    }
                }
        } else {
            onResult(false, "Tidak ada pengguna yang login.")
        }
    }

    fun logout() {
        auth.signOut()
        _userProfile.value = null
    }

    // Fungsi untuk mereset state setelah proses selesai
    fun resetAuthState() {
        _authState.value = AuthState.IDLE
        _errorMessage.value = null
    }
}