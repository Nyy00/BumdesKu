
package com.dony.bumdesku.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dony.bumdesku.data.UnitUsaha
import com.dony.bumdesku.viewmodel.AuthState
import com.dony.bumdesku.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authState: AuthState,
    onLoginClick: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            Text(
                "Selamat Datang",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Login untuk melanjutkan ke BumdesKu",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = authState != AuthState.LOADING
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
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
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        onLoginClick(email, password)
                    } else {
                        Toast.makeText(context, "Email dan password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = authState != AuthState.LOADING
            ) {
                Text("Login", fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Belum punya akun?", style = MaterialTheme.typography.bodyMedium)
                TextButton(
                    onClick = onNavigateToRegister,
                    enabled = authState != AuthState.LOADING
                ) {
                    Text("Daftar di sini", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (authState == AuthState.LOADING) {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onRegisterClick: (String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val unitUsahaList by authViewModel.allUnitUsahaList.collectAsStateWithLifecycle()
    var selectedUnitUsaha by remember { mutableStateOf<UnitUsaha?>(null) }
    var isUnitUsahaExpanded by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Buat Akun Baru",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Daftar sebagai pengelola unit usaha",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = authState != AuthState.LOADING
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
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
                shape = RoundedCornerShape(12.dp),
                visualTransformation = PasswordVisualTransformation(),
                enabled = authState != AuthState.LOADING
            )
            Spacer(modifier = Modifier.height(16.dp))

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
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
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
            Spacer(modifier = Modifier.height(32.dp))

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
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = authState != AuthState.LOADING
            ) {
                Text("Daftar", fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sudah punya akun?", style = MaterialTheme.typography.bodyMedium)
                TextButton(
                    onClick = onNavigateToLogin,
                    enabled = authState != AuthState.LOADING
                ) {
                    Text("Login", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (authState == AuthState.LOADING) {
            CircularProgressIndicator()
        }
    }
}
