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
            item { GuideSection("Selamat Datang", "Aplikasi BumdesKu dirancang untuk membantu Anda mengelola keuangan dan operasional unit usaha BUMDes secara digital, akurat, dan efisien.") }
            item { GuideSection("Peran Pengguna (Roles)", "Aplikasi ini memiliki 3 peran:\n\n• Manager: Dapat melihat dan mengelola semua data di semua unit usaha, termasuk menambah unit usaha dan akun baru.\n\n• Pengurus: Hanya dapat mengelola data pada unit usaha yang ditugaskan kepadanya.\n\n• Auditor: Dapat melihat semua data dan laporan dari semua unit usaha, tetapi tidak dapat mengubah, menambah, atau menghapus data (read-only).") }
            item { GuideSection("Input Jurnal Umum", "Ini adalah fitur utama untuk mencatat semua transaksi keuangan.\n\n1. Tekan tombol 'Input Jurnal' di Halaman Utama.\n2. Isi deskripsi, nominal, dan pilih akun Debit (untuk apa uang digunakan) dan Kredit (dari mana uang berasal).\n3. Jika Anda seorang Pengurus, unit usaha akan terisi otomatis. Jika Manager, Anda harus memilih unit usaha terkait.\n4. Tekan 'Simpan Jurnal'.") }
            item { GuideSection("Kasir Toko & Jual Hasil Panen", "Fitur ini mempercepat transaksi penjualan.\n\n1. Pilih produk/hasil panen yang ingin dijual dari daftar di bagian atas.\n2. Produk akan masuk ke keranjang di bagian bawah. Anda bisa mengubah jumlahnya dengan menekan tombol +/- atau mengetik manual lalu tekan 'Selesai' di keyboard.\n3. Tekan tombol 'Bayar' untuk menyelesaikan transaksi. Stok akan otomatis berkurang dan jurnal penjualan akan tercatat.") }
            item { GuideSection("Siklus Produksi (Agribisnis)", "Fitur ini khusus untuk menghitung biaya dan HPP (Harga Pokok Produksi) agribisnis.\n\n1. Mulai siklus baru dari halaman 'Siklus Produksi'.\n2. Masuk ke detail siklus, lalu tambahkan semua biaya yang terjadi selama proses produksi (pembelian bibit, pupuk, dll).\n3. Setelah panen, tekan tombol 'Selesaikan Siklus' dan masukkan total hasil panen (misal: 1500 Kg). Aplikasi akan otomatis menghitung HPP per unitnya.\n4. HPP ini nantinya bisa digunakan sebagai harga modal saat mencatat stok hasil panen.") }
            item { GuideSection("Laporan & Cetak Struk", "Semua laporan dapat diakses dari Halaman Utama.\n\n• Laporan Keuangan: Anda bisa memfilter berdasarkan rentang tanggal dan unit usaha (khusus Manager/Auditor), lalu mengekspornya ke format PDF.\n\n• Laporan Penjualan: Setelah melihat daftar penjualan, klik salah satu transaksi untuk melihat detailnya.\n\n• Cetak Struk: Di halaman detail transaksi, tekan ikon printer di pojok kanan atas. Pastikan printer kasir Anda sudah terhubung via Bluetooth dengan ponsel.") }
            item { GuideSection("Kunci Jurnal (Khusus Manager)", "Fitur ini digunakan untuk menutup buku pada akhir periode (misal: akhir bulan atau tahun).\n\nSetelah jurnal dikunci hingga tanggal tertentu, semua transaksi sebelum dan pada tanggal tersebut tidak dapat diubah atau dihapus lagi untuk menjaga integritas data.") }
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