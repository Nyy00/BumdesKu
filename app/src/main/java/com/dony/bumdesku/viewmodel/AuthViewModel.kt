package com.dony.bumdesku.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.data.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class LoginNavigationState {
    IDLE,
    NAVIGATE_TO_HOME_UMUM,
    NAVIGATE_TO_HOME_SPESIFIK,
    NAVIGATE_TO_SELECTION
}

enum class AuthState {
    IDLE, LOADING, SUCCESS, ERROR
}

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _authState = MutableStateFlow(AuthState.IDLE)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _loginNavigationState = MutableStateFlow(LoginNavigationState.IDLE)
    val loginNavigationState: StateFlow<LoginNavigationState> = _loginNavigationState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _activeUnitUsaha = MutableStateFlow<UnitUsaha?>(null)
    val activeUnitUsaha: StateFlow<UnitUsaha?> = _activeUnitUsaha.asStateFlow()

    private val _userUnitUsahaList = MutableStateFlow<List<UnitUsaha>>(emptyList())
    val userUnitUsahaList: StateFlow<List<UnitUsaha>> = _userUnitUsahaList.asStateFlow()

    private val _allUnitUsahaList = MutableStateFlow<List<UnitUsaha>>(emptyList())
    val allUnitUsahaList: StateFlow<List<UnitUsaha>> = _allUnitUsahaList.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _targetUserId = MutableStateFlow<String?>(null)
    val targetUserId: StateFlow<String?> = _targetUserId.asStateFlow()

    init {
        auth.currentUser?.uid?.let { userId ->
            loadUserSessionData(userId)
        }
        fetchAllUnitUsahaForRegistration()
    }

    fun loginUser(email: String, pass: String) {
        viewModelScope.launch {
            _authState.update { AuthState.LOADING }
            try {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                result.user?.uid?.let {
                    loadUserSessionData(it, isLoginProcess = true)
                }
                _authState.update { AuthState.SUCCESS }
            } catch (e: Exception) {
                _errorMessage.update { e.message }
                _authState.update { AuthState.ERROR }
            }
        }
    }

    fun registerUser(email: String, pass: String, selectedUnitUsahaId: String) {
        viewModelScope.launch {
            _authState.update { AuthState.LOADING }
            try {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                result.user?.uid?.let { userId ->
                    val userProfileData = UserProfile(
                        uid = userId,
                        email = email,
                        role = "pengurus",
                        managedUnitUsahaIds = listOf(selectedUnitUsahaId)
                    )
                    firestore.collection("users").document(userId).set(userProfileData).await()
                }
                _authState.update { AuthState.SUCCESS }
            } catch (e: Exception) {
                _errorMessage.update { e.message }
                _authState.update { AuthState.ERROR }
            }
        }
    }

    fun logout() {
        auth.signOut()
        _userProfile.update { null }
        _targetUserId.update { null }
        _userUnitUsahaList.update { emptyList() }
        _activeUnitUsaha.update { null }
    }

    fun setActiveUnitUsaha(unitUsaha: UnitUsaha) {
        _activeUnitUsaha.update { unitUsaha }
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

    fun resetAuthState() {
        _authState.update { AuthState.IDLE }
        _errorMessage.update { null }
    }

    fun resetNavigationState() {
        _loginNavigationState.update { LoginNavigationState.IDLE }
    }

    private fun loadUserSessionData(userId: String, isLoginProcess: Boolean = false) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").document(userId).get().await()
                val profile = snapshot.toObject(UserProfile::class.java)?.copy(uid = userId)
                _userProfile.update { profile }

                determineTargetUserId(profile)
                fetchUserManagedUnitUsaha(profile, isLoginProcess)

            } catch (e: Exception) {
                _errorMessage.update { "Gagal memuat profil pengguna." }
                if (isLoginProcess) _authState.update { AuthState.ERROR }
            }
        }
    }

    // --- FUNGSI INI DIPERBAIKI SECARA FINAL ---
    private fun fetchUserManagedUnitUsaha(userProfile: UserProfile?, isLoginProcess: Boolean) {
        viewModelScope.launch {
            if (userProfile == null || userProfile.managedUnitUsahaIds.isEmpty()) {
                _userUnitUsahaList.update { emptyList() }
                if (isLoginProcess) _loginNavigationState.update { LoginNavigationState.NAVIGATE_TO_HOME_UMUM }
                return@launch
            }

            try {
                val snapshot = firestore.collection("unit_usaha")
                    .whereIn(FieldPath.documentId(), userProfile.managedUnitUsahaIds)
                    .get().await()

                val unitUsahaWithIds = snapshot.toObjects(UnitUsaha::class.java).mapIndexed { i, unit ->
                    unit.apply { id = snapshot.documents[i].id }
                }
                _userUnitUsahaList.update { unitUsahaWithIds }

                // --- LOGIKA KUNCI ADA DI SINI ---
                // Jika hanya ada satu unit usaha, langsung set sebagai aktif.
                // Ini akan berjalan baik saat login maupun saat aplikasi restart.
                if (unitUsahaWithIds.size == 1) {
                    setActiveUnitUsaha(unitUsahaWithIds.first())
                }
                // ---------------------------------

                // Logika navigasi hanya dijalankan saat proses login baru.
                if (isLoginProcess) {
                    val navState = when {
                        unitUsahaWithIds.size > 1 -> LoginNavigationState.NAVIGATE_TO_SELECTION
                        unitUsahaWithIds.size == 1 -> LoginNavigationState.NAVIGATE_TO_HOME_SPESIFIK
                        else -> LoginNavigationState.NAVIGATE_TO_HOME_UMUM
                    }
                    _loginNavigationState.update { navState }
                }

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Gagal mengambil unit usaha yang dikelola: ${e.message}")
                if (isLoginProcess) _loginNavigationState.update { LoginNavigationState.NAVIGATE_TO_HOME_UMUM }
            }
        }
    }

    private fun fetchAllUnitUsahaForRegistration() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("unit_usaha").get().await()
                val unitUsahaWithIds = snapshot.toObjects(UnitUsaha::class.java).mapIndexed { i, unit ->
                    unit.apply { id = snapshot.documents[i].id }
                }
                _allUnitUsahaList.update { unitUsahaWithIds }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Gagal mengambil daftar unit usaha untuk registrasi: ", e)
            }
        }
    }

    private fun determineTargetUserId(profile: UserProfile?) {
        viewModelScope.launch {
            if (profile?.role == "auditor") {
                val pengurusQuery = firestore.collection("users").whereEqualTo("role", "pengurus").limit(1).get().await()
                _targetUserId.update { pengurusQuery.documents.firstOrNull()?.id }
            } else {
                _targetUserId.update { profile?.uid }
            }
        }
    }
}