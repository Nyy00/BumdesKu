package com.dony.bumdesku.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.dony.bumdesku.data.UnitUsaha
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dony.bumdesku.util.ThousandSeparatorVisualTransformation
import com.dony.bumdesku.data.Account
import com.dony.bumdesku.data.AccountCategory
import com.dony.bumdesku.data.Payable
import com.dony.bumdesku.data.Receivable
import com.dony.bumdesku.viewmodel.DebtViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// --- Layar Daftar Utang (Payable) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayableListScreen(
    viewModel: DebtViewModel,
    userRole: String,
    onAddItemClick: () -> Unit,
    onEditItemClick: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onMarkAsPaid: (Payable) -> Unit,
    onDeleteItem: (Payable) -> Unit
) {
    val payables by viewModel.filteredPayables.collectAsStateWithLifecycle()
    val allUnitUsaha by viewModel.allUnitUsaha.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedUnit by viewModel.selectedUnitFilter.collectAsStateWithLifecycle()
    var itemToConfirmPaid by remember { mutableStateOf<Payable?>(null) }
    var itemToConfirmDelete by remember { mutableStateOf<Payable?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Daftar Utang Usaha") }, navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, "Kembali") } }) },
        floatingActionButton = { FloatingActionButton(onClick = onAddItemClick) { Icon(Icons.Default.Add, "Tambah Utang") } }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // [KONTROL AKSES] Hanya tampilkan filter untuk Manajer
            if (userRole == "manager") {
                var isExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = { isExpanded = !isExpanded },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedUnit?.name ?: "Semua Unit Usaha",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filter Unit Usaha") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Semua Unit Usaha") },
                            onClick = {
                                viewModel.selectUnitFilter(null)
                                isExpanded = false
                            }
                        )
                        allUnitUsaha.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.name) },
                                onClick = {
                                    viewModel.selectUnitFilter(unit)
                                    isExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (payables.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (selectedUnit != null) "Tidak ada utang untuk unit ini." else "Belum ada utang.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(payables, key = { it.id }) { payable ->
                        DebtItemCard(
                            contactName = payable.contactName,
                            description = payable.description,
                            amount = payable.amount,
                            dueDate = payable.dueDate,
                            isPaid = payable.isPaid,
                            onCardClick = { if (!payable.isPaid) onEditItemClick(payable.id) },
                            onMarkAsPaid = { itemToConfirmPaid = payable },
                            onDeleteClick = { itemToConfirmDelete = payable }
                        )
                    }
                }
            }
        }
    }

    itemToConfirmPaid?.let { payable ->
        AlertDialog(
            onDismissRequest = { itemToConfirmPaid = null },
            title = { Text("Konfirmasi Pelunasan") },
            text = { Text("Yakin ingin menandai utang ini sebagai LUNAS?") },
            confirmButton = { Button(onClick = { onMarkAsPaid(payable); itemToConfirmPaid = null }) { Text("Ya, Lunas") } },
            dismissButton = { Button(onClick = { itemToConfirmPaid = null }) { Text("Batal") } }
        )
    }

    itemToConfirmDelete?.let { payable ->
        AlertDialog(
            onDismissRequest = { itemToConfirmDelete = null },
            title = { Text("Konfirmasi Hapus") },
            text = { Text("Yakin ingin menghapus catatan ini? Aksi ini tidak bisa dibatalkan.") },
            confirmButton = { Button(onClick = { onDeleteItem(payable); itemToConfirmDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Hapus") } },
            dismissButton = { Button(onClick = { itemToConfirmDelete = null }) { Text("Batal") } }
        )
    }
}

// --- Layar Daftar Piutang (Receivable) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivableListScreen(
    viewModel: DebtViewModel,
    userRole: String,
    onAddItemClick: () -> Unit,
    onEditItemClick: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onMarkAsPaid: (Receivable) -> Unit,
    onDeleteItem: (Receivable) -> Unit
) {
    val receivables by viewModel.filteredReceivables.collectAsStateWithLifecycle()
    val allUnitUsaha by viewModel.allUnitUsaha.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedUnit by viewModel.selectedUnitFilter.collectAsStateWithLifecycle()
    var itemToConfirmPaid by remember { mutableStateOf<Receivable?>(null) }
    var itemToConfirmDelete by remember { mutableStateOf<Receivable?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Daftar Piutang Usaha") }, navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, "Kembali") } }) },
        floatingActionButton = { FloatingActionButton(onClick = onAddItemClick) { Icon(Icons.Default.Add, "Tambah Piutang") } }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // [KONTROL AKSES] Hanya tampilkan filter untuk Manajer
            if (userRole == "manager") {
                var isExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = { isExpanded = !isExpanded },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedUnit?.name ?: "Semua Unit Usaha",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filter Unit Usaha") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Semua Unit Usaha") },
                            onClick = {
                                viewModel.selectUnitFilter(null)
                                isExpanded = false
                            }
                        )
                        allUnitUsaha.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.name) },
                                onClick = {
                                    viewModel.selectUnitFilter(unit)
                                    isExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (receivables.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (selectedUnit != null) "Tidak ada piutang untuk unit ini." else "Belum ada piutang.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(receivables, key = { it.id }) { receivable ->
                        DebtItemCard(
                            contactName = receivable.contactName,
                            description = receivable.description,
                            amount = receivable.amount,
                            dueDate = receivable.dueDate,
                            isPaid = receivable.isPaid,
                            onCardClick = { if (!receivable.isPaid) onEditItemClick(receivable.id) },
                            onMarkAsPaid = { itemToConfirmPaid = receivable },
                            onDeleteClick = { itemToConfirmDelete = receivable }
                        )
                    }
                }
            }
        }
    }

    itemToConfirmPaid?.let { receivable ->
        AlertDialog(
            onDismissRequest = { itemToConfirmPaid = null },
            title = { Text("Konfirmasi Pelunasan") },
            text = { Text("Yakin ingin menandai piutang ini sebagai LUNAS?") },
            confirmButton = { Button(onClick = { onMarkAsPaid(receivable); itemToConfirmPaid = null }) { Text("Ya, Lunas") } },
            dismissButton = { Button(onClick = { itemToConfirmPaid = null }) { Text("Batal") } }
        )
    }

    itemToConfirmDelete?.let { receivable ->
        AlertDialog(
            onDismissRequest = { itemToConfirmDelete = null },
            title = { Text("Konfirmasi Hapus") },
            text = { Text("Yakin ingin menghapus catatan ini?") },
            confirmButton = { Button(onClick = { onDeleteItem(receivable); itemToConfirmDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Hapus") } },
            dismissButton = { Button(onClick = { itemToConfirmDelete = null }) { Text("Batal") } }
        )
    }
}


// --- Composable Card Umum ---
@Composable
fun DebtItemCard(
    contactName: String,
    description: String,
    amount: Double,
    dueDate: Long,
    isPaid: Boolean,
    onCardClick: () -> Unit,
    onMarkAsPaid: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val localeID = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }
    val dateFormat = SimpleDateFormat("dd MMM yyyy", localeID)

    val now = System.currentTimeMillis()
    val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L
    val isOverdue = now > dueDate && !isPaid
    val isDueSoon = dueDate - now in 1..sevenDaysInMillis && !isPaid

    val dueDateColor = when {
        isOverdue -> MaterialTheme.colorScheme.error
        isDueSoon -> Color(0xFFFFA000)
        else -> Color.Gray
    }

    val dueDateIcon = when {
        isOverdue -> Icons.Default.ErrorOutline
        isDueSoon -> Icons.Default.WarningAmber
        else -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onCardClick),
        colors = CardDefaults.cardColors(containerColor = if (isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(contactName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(currencyFormat.format(amount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    if (dueDateIcon != null && !isPaid) {
                        Icon(
                            imageVector = dueDateIcon,
                            contentDescription = "Status Jatuh Tempo",
                            tint = dueDateColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (isPaid) "LUNAS" else "Jatuh Tempo: ${dateFormat.format(Date(dueDate))}",
                        color = dueDateColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!isPaid) {
                    Button(onClick = onMarkAsPaid) { Text("Lunas") }
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.DeleteOutline, "Hapus", tint = Color.Gray)
                }
            }
        }
    }
}

// --- Layar Entri Utang (PayableEntryScreen) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayableEntryScreen(
    payableToEdit: Payable?,
    viewModel: DebtViewModel,
    userRole: String,
    activeUnitUsaha: UnitUsaha?,
    onSave: (Payable) -> Unit,
    onSaveWithJournal: (Payable, Account) -> Unit,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val isEditMode = payableToEdit != null

    var contactName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = payableToEdit?.dueDate ?: System.currentTimeMillis())

    val allUnitUsaha by viewModel.allUnitUsaha.collectAsStateWithLifecycle(emptyList())
    var selectedUnitUsaha by remember { mutableStateOf<UnitUsaha?>(null) }
    var isUnitUsahaExpanded by remember { mutableStateOf(false) }

    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val debitAccountOptions = allAccounts.filter { it.category == AccountCategory.ASET || it.category == AccountCategory.BEBAN }
    var selectedDebitAccount by remember { mutableStateOf<Account?>(null) }
    var isDebitAccountExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(activeUnitUsaha, payableToEdit, allUnitUsaha) {
        if (payableToEdit != null && allUnitUsaha.isNotEmpty()) {
            selectedUnitUsaha = allUnitUsaha.find { it.id == payableToEdit.unitUsahaId }
        } else if (userRole == "pengurus") {
            selectedUnitUsaha = activeUnitUsaha
        }
    }

    LaunchedEffect(payableToEdit) {
        if (isEditMode) {
            contactName = payableToEdit?.contactName ?: ""
            description = payableToEdit?.description ?: ""
            amount = payableToEdit?.amount?.toLong()?.toString() ?: ""
            dueDateMillis = payableToEdit?.dueDate ?: System.currentTimeMillis()
        }
    }

    LaunchedEffect(dateState.selectedDateMillis) {
        dateState.selectedDateMillis?.let { dueDateMillis = it }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (isEditMode) "Edit Utang" else "Tambah Utang Baru") }, navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, "Kembali") } }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dropdown Unit Usaha (hanya untuk manajer)
            if (userRole == "manager") {
                ExposedDropdownMenuBox(
                    expanded = isUnitUsahaExpanded,
                    onExpandedChange = { if (!isEditMode) isUnitUsahaExpanded = !isUnitUsahaExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedUnitUsaha?.name ?: "Pilih Unit Usaha",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit Usaha") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitUsahaExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isUnitUsahaExpanded,
                        onDismissRequest = { isUnitUsahaExpanded = false }
                    ) {
                        allUnitUsaha.forEach { unit ->
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

            OutlinedTextField(value = contactName, onValueChange = { contactName = it }, label = { Text("Nama Kreditor") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Deskripsi Utang") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(
                value = amount,
                onValueChange = { newValue -> amount = newValue.filter { it.isDigit() } },
                label = { Text("Jumlah Utang") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandSeparatorVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (!isEditMode) {
                ExposedDropdownMenuBox(expanded = isDebitAccountExpanded, onExpandedChange = { isDebitAccountExpanded = !isDebitAccountExpanded }) {
                    OutlinedTextField(
                        value = selectedDebitAccount?.let { "${it.accountNumber} - ${it.accountName}" } ?: "Pilih Akun Debit (Untuk Apa Utang Ini)",
                        onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDebitAccountExpanded) }
                    )
                    ExposedDropdownMenu(expanded = isDebitAccountExpanded, onDismissRequest = { isDebitAccountExpanded = false }) {
                        debitAccountOptions.forEach { account ->
                            DropdownMenuItem(text = { Text("${account.accountNumber} - ${account.accountName}") }, onClick = { selectedDebitAccount = account; isDebitAccountExpanded = false })
                        }
                    }
                }
            }

            Button(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Jatuh Tempo: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dueDateMillis))}")
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    val finalUnitUsahaId = selectedUnitUsaha?.id
                    if (contactName.isNotBlank() && description.isNotBlank() && amountDouble != null && finalUnitUsahaId != null) {
                        if (isEditMode) {
                            val updatedPayable = payableToEdit!!.copy(
                                contactName = contactName,
                                description = description,
                                amount = amountDouble,
                                dueDate = dueDateMillis,
                                unitUsahaId = finalUnitUsahaId // Pastikan unitUsahaId juga diupdate
                            )
                            onSave(updatedPayable)
                        } else {
                            if (selectedDebitAccount != null) {
                                onSaveWithJournal(
                                    Payable(
                                        contactName = contactName,
                                        description = description,
                                        amount = amountDouble,
                                        transactionDate = System.currentTimeMillis(),
                                        dueDate = dueDateMillis,
                                        unitUsahaId = finalUnitUsahaId
                                    ),
                                    selectedDebitAccount!!
                                )
                            } else {
                                Toast.makeText(context, "Harap pilih akun debit", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Harap isi semua kolom, termasuk Unit Usaha.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Simpan") }
        }
    }
    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { Button(onClick = { showDatePicker = false }) { Text("OK") } }) { DatePicker(state = dateState) }
    }
}

// --- Layar Entri Piutang (untuk Tambah & Edit) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivableEntryScreen(
    receivableToEdit: Receivable?,
    viewModel: DebtViewModel,
    userRole: String, // ✅ Tambahkan parameter ini
    activeUnitUsaha: UnitUsaha?, // ✅ Tambahkan parameter ini
    onSave: (Receivable) -> Unit,
    onSaveWithJournal: (Receivable, Account) -> Unit,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val isEditMode = receivableToEdit != null

    var contactName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateState = rememberDatePickerState(initialSelectedDateMillis = receivableToEdit?.dueDate ?: System.currentTimeMillis())

    // State untuk dropdown unit usaha
    val allUnitUsaha by viewModel.allUnitUsaha.collectAsStateWithLifecycle(emptyList())
    var selectedUnitUsaha by remember { mutableStateOf<UnitUsaha?>(null) }
    var isUnitUsahaExpanded by remember { mutableStateOf(false) }

    // State untuk dropdown akun kredit
    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val creditAccountOptions = allAccounts.filter { it.category == AccountCategory.PENDAPATAN }
    var selectedCreditAccount by remember { mutableStateOf<Account?>(null) }
    var isCreditAccountExpanded by remember { mutableStateOf(false) }

    // Efek untuk mengatur state awal saat layar dibuka
    LaunchedEffect(activeUnitUsaha, receivableToEdit, allUnitUsaha) {
        if (receivableToEdit != null && allUnitUsaha.isNotEmpty()) {
            selectedUnitUsaha = allUnitUsaha.find { it.id == receivableToEdit.unitUsahaId }
        } else if (userRole == "pengurus") {
            selectedUnitUsaha = activeUnitUsaha
        }
    }

    LaunchedEffect(receivableToEdit) {
        if (isEditMode) {
            contactName = receivableToEdit?.contactName ?: ""
            description = receivableToEdit?.description ?: ""
            amount = receivableToEdit?.amount?.toLong()?.toString() ?: ""
            dueDateMillis = receivableToEdit?.dueDate ?: System.currentTimeMillis()
        }
    }

    LaunchedEffect(dateState.selectedDateMillis) {
        dateState.selectedDateMillis?.let { dueDateMillis = it }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (isEditMode) "Edit Piutang" else "Tambah Piutang Baru") }, navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, "Kembali") } }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dropdown Unit Usaha (hanya untuk manajer)
            if (userRole == "manager") {
                ExposedDropdownMenuBox(
                    expanded = isUnitUsahaExpanded,
                    onExpandedChange = { if (!isEditMode) isUnitUsahaExpanded = !isUnitUsahaExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedUnitUsaha?.name ?: "Pilih Unit Usaha",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit Usaha") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitUsahaExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isUnitUsahaExpanded,
                        onDismissRequest = { isUnitUsahaExpanded = false }
                    ) {
                        allUnitUsaha.forEach { unit ->
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

            OutlinedTextField(value = contactName, onValueChange = { contactName = it }, label = { Text("Nama Debitor") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Deskripsi Piutang") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(
                value = amount,
                onValueChange = { newValue -> amount = newValue.filter { it.isDigit() } },
                label = { Text("Jumlah Piutang") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandSeparatorVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            // Dropdown Akun Kredit (hanya saat menambah baru)
            if (!isEditMode) {
                ExposedDropdownMenuBox(expanded = isCreditAccountExpanded, onExpandedChange = { isCreditAccountExpanded = !isCreditAccountExpanded }) {
                    OutlinedTextField(
                        value = selectedCreditAccount?.let { "${it.accountNumber} - ${it.accountName}" } ?: "Pilih Akun Kredit (Sumber Piutang)",
                        onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCreditAccountExpanded) }
                    )
                    ExposedDropdownMenu(expanded = isCreditAccountExpanded, onDismissRequest = { isCreditAccountExpanded = false }) {
                        creditAccountOptions.forEach { account ->
                            DropdownMenuItem(text = { Text("${account.accountNumber} - ${account.accountName}") }, onClick = { selectedCreditAccount = account; isCreditAccountExpanded = false })
                        }
                    }
                }
            }

            Button(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Jatuh Tempo: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dueDateMillis))}")
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    val finalUnitUsahaId = selectedUnitUsaha?.id
                    if (contactName.isNotBlank() && description.isNotBlank() && amountDouble != null && finalUnitUsahaId != null) {
                        if (isEditMode) {
                            val updatedReceivable = receivableToEdit!!.copy(
                                contactName = contactName,
                                description = description,
                                amount = amountDouble,
                                dueDate = dueDateMillis,
                                unitUsahaId = finalUnitUsahaId // Pastikan unitUsahaId diupdate
                            )
                            onSave(updatedReceivable)
                        } else {
                            if (selectedCreditAccount != null) {
                                onSaveWithJournal(
                                    Receivable(
                                        contactName = contactName,
                                        description = description,
                                        amount = amountDouble,
                                        transactionDate = System.currentTimeMillis(),
                                        dueDate = dueDateMillis,
                                        unitUsahaId = finalUnitUsahaId
                                    ),
                                    selectedCreditAccount!!
                                )
                            } else {
                                Toast.makeText(context, "Harap pilih akun kredit", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Harap isi semua kolom, termasuk Unit Usaha.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Simpan") }
        }
    }
    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { Button(onClick = { showDatePicker = false }) { Text("OK") } }) { DatePicker(state = dateState) }
    }
}