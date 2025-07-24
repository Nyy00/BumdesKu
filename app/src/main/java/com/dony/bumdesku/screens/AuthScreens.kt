package com.dony.bumdesku.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.viewmodel.AuthState
import com.dony.bumdesku.viewmodel.AuthViewModel
import java.util.Locale

@Composable
fun LoginScreen(
    authState: AuthState, // Parameter baru ditambahkan di sini
    onLoginClick: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Login BumdesKu", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = authState != AuthState.LOADING // Nonaktifkan saat loading
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                enabled = authState != AuthState.LOADING // Nonaktifkan saat loading
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        onLoginClick(email, password)
                    } else {
                        Toast.makeText(context, "Email dan password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = authState != AuthState.LOADING // Nonaktifkan saat loading
            ) {
                Text("Login")
            }

            TextButton(
                onClick = onNavigateToRegister,
                enabled = authState != AuthState.LOADING // Nonaktifkan saat loading
            ) {
                Text("Belum punya akun? Daftar di sini")
            }
        }

        // Tampilkan indikator loading
        if (authState == AuthState.LOADING) {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel, // Gunakan instance ViewModel langsung
    onRegisterClick: (String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // --- State untuk Dropdown Unit Usaha ---
    val unitUsahaList by authViewModel.allUnitUsahaList.collectAsStateWithLifecycle()
    var selectedUnitUsaha by remember { mutableStateOf<UnitUsaha?>(null) }
    var isUnitUsahaExpanded by remember { mutableStateOf(false) }
    // ---------------------------------------

    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Daftar Akun Baru", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = authState != AuthState.LOADING
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                enabled = authState != AuthState.LOADING
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Konfirmasi Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                enabled = authState != AuthState.LOADING
            )
            Spacer(modifier = Modifier.height(16.dp))

            // --- Dropdown untuk Memilih Peran ---
            ExposedDropdownMenuBox(
                expanded = isUnitUsahaExpanded,
                onExpandedChange = { isUnitUsahaExpanded = !isUnitUsahaExpanded }
            ) {
                OutlinedTextField(
                    value = selectedUnitUsaha?.name ?: "Pilih Unit Usaha",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Daftar sebagai pengelola") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitUsahaExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isUnitUsahaExpanded,
                    onDismissRequest = { isUnitUsahaExpanded = false }
                ) {
                    if (unitUsahaList.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Tidak ada unit usaha tersedia") },
                            onClick = { isUnitUsahaExpanded = false }
                        )
                    } else {
                        unitUsahaList.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.name) },
                                onClick = {
                                    selectedUnitUsaha = unit
                                    isUnitUsahaExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            // ----------------------------------------

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()) {
                        if (password == confirmPassword) {
                            if (selectedUnitUsaha != null) {
                                onRegisterClick(email, password, selectedUnitUsaha!!.id)
                            } else {
                                Toast.makeText(context, "Harap pilih unit usaha", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Password tidak cocok", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Semua kolom harus diisi", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = authState != AuthState.LOADING
            ) {
                Text("Daftar")
            }
            TextButton(
                onClick = onNavigateToLogin,
                enabled = authState != AuthState.LOADING
            ) {
                Text("Sudah punya akun? Login")
            }
        }

        if (authState == AuthState.LOADING) {
            CircularProgressIndicator()
        }
    }
}