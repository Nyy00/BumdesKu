package com.dony.bumdesku.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserGuideScreen(onNavigateUp: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panduan Pengguna") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { GuideSection("Selamat Datang di BumdesKu", "Aplikasi BumdesKu adalah solusi lengkap untuk mengelola keuangan dan operasional unit usaha BUMDes Anda. Dengan fitur-fitur yang terintegrasi, Anda dapat mencatat transaksi, memantau laporan, dan mengelola stok dengan lebih akurat dan efisien.") }
            item { GuideSection("Pengelolaan Pengguna dan Akses", "Aplikasi ini membedakan tiga peran pengguna untuk memastikan keamanan dan integritas data:\n\n• Manager: Memiliki kontrol penuh atas semua data dan unit usaha. Dapat menambah atau mengelola unit usaha, akun, dan pengguna.\n\n• Pengurus: Diberi wewenang untuk mengelola data pada unit usaha tertentu yang menjadi tanggung jawabnya.\n\n• Auditor: Memiliki akses baca-saja (read-only) ke semua laporan dan data. Peran ini ideal untuk keperluan audit dan monitoring tanpa risiko perubahan data.") }
            item { GuideSection("Pencatatan Jurnal Umum", "Fitur ini berfungsi sebagai jantung pencatatan transaksi keuangan:\n\n1. Akses 'Input Jurnal' dari beranda atau dashboard.\n2. Lengkapi detail transaksi seperti deskripsi, nominal, akun **Debit** (tujuan dana) dan **Kredit** (sumber dana).\n3. Pengurus akan otomatis terhubung dengan unit usahanya, sementara Manager dapat memilih unit usaha yang relevan.\n4. Konfirmasi dengan menekan 'Simpan Jurnal' untuk mencatat transaksi.") }
            item { GuideSection("Kasir Penjualan & Jasa Sewa", "Mempercepat proses penjualan dan pencatatan transaksi kasir:\n\n1. Pilih produk atau barang yang akan dijual dari daftar yang tersedia.\n2. Item akan ditambahkan ke keranjang, di mana Anda dapat menyesuaikan jumlahnya.\n3. Tekan 'Bayar' untuk menyelesaikan transaksi. Stok barang akan otomatis diperbarui dan jurnal penjualan akan dibuat secara real-time.\n4. Untuk jasa sewa, Anda dapat mencatat transaksi dengan atau tanpa uang muka, dan aplikasi akan mengelola piutang secara otomatis.") }
            item { GuideSection("Siklus Produksi (Agribisnis)", "Fitur khusus untuk manajemen produksi yang akurat:\n\n1. Mulai siklus baru untuk proyek agribisnis Anda.\n2. Catat semua biaya yang terkait dengan siklus tersebut (misalnya, bibit, pupuk, tenaga kerja).\n3. Setelah panen, masukkan total hasil panen. Aplikasi akan secara otomatis menghitung **Harga Pokok Produksi (HPP)** per unit.\n4. HPP ini akan menjadi acuan harga modal untuk pencatatan stok hasil panen.") }
            item { GuideSection("Laporan & Ekspor Data", "Akses laporan keuangan dan penjualan untuk analisis yang mendalam:\n\n• Laporan Keuangan: Filter laporan berdasarkan tanggal dan unit usaha (khusus Manager/Auditor). Semua laporan dapat diekspor ke format PDF untuk dokumentasi.\n\n• Laporan Penjualan: Lihat riwayat transaksi penjualan. Tekan pada transaksi untuk melihat detail lengkap.\n\n• Cetak Struk: Tersedia di halaman detail transaksi. Pastikan printer kasir Anda sudah terhubung via Bluetooth.") }
            item { GuideSection("Kunci Jurnal", "Fitur ini berfungsi untuk mengamankan data keuangan pada akhir periode:\n\n• Hanya peran Manager yang dapat menggunakan fitur ini.\n\n• Setelah jurnal dikunci hingga tanggal tertentu, semua transaksi sebelum dan pada tanggal tersebut tidak dapat diubah atau dihapus, sehingga menjaga integritas dan keakuratan laporan keuangan.") }
        }
    }
}

@Composable
fun GuideSection(title: String, content: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Divider(modifier = Modifier.padding(vertical = 4.dp))
        Text(text = content, style = MaterialTheme.typography.bodyLarge)
    }
}