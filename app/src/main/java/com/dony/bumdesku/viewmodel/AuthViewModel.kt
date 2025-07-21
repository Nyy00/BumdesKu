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

enum class AuthState {
    IDLE, LOADING, SUCCESS, ERROR
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

    // ✅ STATEFLOW BARU UNTUK MENENTUKAN TARGET PENGGUNA
    private val _targetUserId = MutableStateFlow<String?>(null)
    val targetUserId: StateFlow<String?> = _targetUserId

    init {
        auth.currentUser?.uid?.let {
            fetchUserProfile(it)
        }
    }

    private fun fetchUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").document(userId).get().await()
                val profile = snapshot.toObject(UserProfile::class.java)?.copy(uid = userId)
                _userProfile.value = profile

                // ✅ LOGIKA BARU: TENTUKAN TARGET USER ID SETELAH PROFIL DIDAPATKAN
                determineTargetUserId(profile)

            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat profil pengguna."
                _targetUserId.value = null // Reset jika gagal
            }
        }
    }

    // ✅ FUNGSI BANTUAN BARU
    private fun determineTargetUserId(profile: UserProfile?) {
        viewModelScope.launch {
            if (profile?.role == "auditor") {
                // Jika auditor, cari userId milik pengurus
                val pengurusQuery = firestore.collection("users").whereEqualTo("role", "pengurus").limit(1).get().await()
                _targetUserId.value = pengurusQuery.documents.firstOrNull()?.id
            } else {
                // Jika pengurus atau peran lain, gunakan ID sendiri
                _targetUserId.value = profile?.uid
            }
        }
    }

    fun registerUser(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.LOADING
            try {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                val userId = result.user?.uid

                if (userId != null) {
                    val userProfile = UserProfile(
                        email = email,
                        role = if (email.contains("auditor")) "auditor" else "pengurus" // Otomatisasi peran
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
        _targetUserId.value = null // Reset target saat logout
    }

    fun resetAuthState() {
        _authState.value = AuthState.IDLE
        _errorMessage.value = null
    }
}
