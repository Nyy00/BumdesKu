package com.dony.bumdesku.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.data.UserProfile
import com.dony.bumdesku.repository.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ListenerRegistration
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

class AuthViewModel(
    private val unitUsahaRepository: UnitUsahaRepository,
    private val transactionRepository: TransactionRepository,
    private val assetRepository: AssetRepository,
    private val posRepository: PosRepository,
    private val accountRepository: AccountRepository,
    private val debtRepository: DebtRepository,
    private val agriRepository: AgriRepository,
    private val agriCycleRepository: AgriCycleRepository
) : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore
    private val activeListeners = mutableListOf<ListenerRegistration>()

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

    init {
        auth.currentUser?.uid?.let { userId ->
            loadUserSessionData(userId)
        }
        fetchAllUnitUsahaForRegistration()
    }

    private fun clearActiveListeners() {
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
        Log.d("AuthViewModel", "All active listeners cleared.")
    }

    private fun triggerSyncForManagerAndAuditor() {
        clearActiveListeners()
        try {
            Log.d("AuthViewModel", "Manager/Auditor sync started.")
            activeListeners.add(accountRepository.syncAccounts(auth.currentUser!!.uid))
            activeListeners.add(unitUsahaRepository.syncAllUnitUsahaForManager())
            activeListeners.add(assetRepository.syncAllAssetsForManager())
            activeListeners.add(transactionRepository.syncAllTransactionsForManager())
            activeListeners.add(posRepository.syncAllSalesForManager())
            activeListeners.add(debtRepository.syncAllPayablesForManager())
            activeListeners.add(debtRepository.syncAllReceivablesForManager())
            activeListeners.add(agriRepository.syncAllHarvestsForManager())
            activeListeners.add(agriRepository.syncAllProduceSalesForManager())
             activeListeners.add(agriCycleRepository.syncAllCyclesForManager())
             activeListeners.add(agriCycleRepository.syncAllCostsForManager())
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Failed to trigger sync for manager/auditor", e)
        }
    }

    private fun loadUserSessionData(userId: String, isLoginProcess: Boolean = false) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").document(userId).get().await()
                val profile = snapshot.toObject(UserProfile::class.java)?.copy(uid = userId)
                _userProfile.update { profile }

                when (profile?.role) {
                    "manager", "auditor" -> {
                        triggerSyncForManagerAndAuditor()
                        if (isLoginProcess) {
                            _loginNavigationState.update { LoginNavigationState.NAVIGATE_TO_HOME_UMUM }
                        }
                    }
                    "pengurus" -> {
                        clearActiveListeners()
                        activeListeners.add(accountRepository.syncAccounts(userId))

                        if (profile.managedUnitUsahaIds.isNotEmpty()) {
                            activeListeners.add(transactionRepository.syncTransactionsForUser(profile.managedUnitUsahaIds))
                            activeListeners.add(assetRepository.syncAssetsForUser(profile.managedUnitUsahaIds))
                            activeListeners.add(posRepository.syncSalesForUser(profile.managedUnitUsahaIds))
                            activeListeners.add(debtRepository.syncPayablesForUser(profile.managedUnitUsahaIds))
                            activeListeners.add(debtRepository.syncReceivablesForUser(profile.managedUnitUsahaIds))
                            activeListeners.add(agriRepository.syncHarvestsForUser(profile.managedUnitUsahaIds))
                            activeListeners.add(agriRepository.syncProduceSalesForUser(profile.managedUnitUsahaIds))
                             activeListeners.add(agriCycleRepository.syncCyclesForUser(profile.managedUnitUsahaIds))
                             activeListeners.add(agriCycleRepository.syncCostsForUser(userId))
                        }

                        fetchUserManagedUnitUsaha(profile, isLoginProcess)
                    }
                    else -> {
                        clearActiveListeners()
                        activeListeners.add(accountRepository.syncAccounts(userId))
                    }
                }

            } catch (e: Exception) {
                _errorMessage.update { "Gagal memuat profil pengguna." }
                if (isLoginProcess) _authState.update { AuthState.ERROR }
            }
        }
    }

    private fun fetchUserManagedUnitUsaha(userProfile: UserProfile?, isLoginProcess: Boolean) {
        viewModelScope.launch {
            if (userProfile == null || userProfile.managedUnitUsahaIds.isEmpty()) {
                _userUnitUsahaList.update { emptyList() }
                if (isLoginProcess) _loginNavigationState.update { LoginNavigationState.NAVIGATE_TO_HOME_UMUM }
                return@launch
            }

            try {
                val snapshot = firestore.collection("unit_usaha")
                    .whereIn(FieldPath.documentId(), userProfile.managedUnitUsahaIds.take(30))
                    .get().await()

                val unitUsahaWithIds = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UnitUsaha::class.java)?.apply { id = doc.id }
                }
                _userUnitUsahaList.update { unitUsahaWithIds }

                if (unitUsahaWithIds.size == 1) {
                    setActiveUnitUsaha(unitUsahaWithIds.first())
                }

                if (isLoginProcess) {
                    val navState = when {
                        unitUsahaWithIds.size > 1 -> LoginNavigationState.NAVIGATE_TO_SELECTION
                        unitUsahaWithIds.size == 1 -> LoginNavigationState.NAVIGATE_TO_HOME_SPESIFIK
                        else -> LoginNavigationState.NAVIGATE_TO_HOME_UMUM
                    }
                    _loginNavigationState.update { navState }
                }

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Gagal mengambil unit usaha yang dikelola: ${e.message}", e)
                if (isLoginProcess) _loginNavigationState.update { LoginNavigationState.NAVIGATE_TO_HOME_UMUM }
            }
        }
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
        clearActiveListeners()
        _userProfile.update { null }
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

    override fun onCleared() {
        super.onCleared()
        clearActiveListeners()
    }
}
