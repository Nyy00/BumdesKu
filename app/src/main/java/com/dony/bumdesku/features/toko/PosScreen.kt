package com.dony.bumdesku.features.toko

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dony.bumdesku.data.Asset
import com.dony.bumdesku.data.CartItem
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    posViewModel: PosViewModel,
    onNavigateUp: () -> Unit,
    onSaleComplete: () -> Unit
) {
    val products by posViewModel.filteredProducts.collectAsState()
    val searchQuery by posViewModel.searchQuery.collectAsState()

    // --- Ambil state baru untuk kategori ---
    val categories by posViewModel.productCategories.collectAsState()
    val selectedCategory by posViewModel.selectedCategory.collectAsState()
    // --------------------------------------

    val cartItems by posViewModel.cartItems.collectAsState()
    val totalPrice by posViewModel.totalPrice.collectAsState()
    val saleState by posViewModel.saleState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(saleState) {
        when(saleState) {
            SaleState.SUCCESS -> {
                Toast.makeText(context, "Transaksi berhasil!", Toast.LENGTH_SHORT).show()
                posViewModel.resetSaleState()
                onSaleComplete()
            }
            SaleState.ERROR -> {
                Toast.makeText(context, "Gagal memproses transaksi.", Toast.LENGTH_SHORT).show()
                posViewModel.resetSaleState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kasir Toko") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                }
            )
        },
        bottomBar = {
            PosBottomBar(
                totalPrice = totalPrice,
                // Panggil completeSale() tanpa parameter
                onCompleteSale = { posViewModel.completeSale() },
                isProcessing = saleState == SaleState.LOADING,
                isCartEmpty = cartItems.isEmpty()
            )
        }
    ) {paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Kolom Kiri: Daftar Produk dengan Search Bar dan Tab Kategori
            Column(modifier = Modifier.weight(1f)) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { posViewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    label = { Text("Cari Produk...") },
                    leadingIcon = { Icon(Icons.Default.Search, "Cari") },
                    singleLine = true
                )

                // --- TAB KATEGORI BARU ---
                if (categories.size > 1) { // Hanya tampilkan jika ada kategori
                    ScrollableTabRow(
                        selectedTabIndex = categories.indexOf(selectedCategory),
                        edgePadding = 8.dp
                    ) {
                        categories.forEachIndexed { index, category ->
                            Tab(
                                selected = selectedCategory == category,
                                onClick = { posViewModel.onCategorySelected(category) },
                                text = { Text(category) }
                            )
                        }
                    }
                }
                // -------------------------

                ProductList(
                    products = products,
                    onProductClick = { product ->
                        posViewModel.addToCart(product)
                    }
                )
            }

            Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

            // Kolom Kanan: Keranjang Belanja
            CartPanel(
                modifier = Modifier.weight(1f),
                cartItems = cartItems,
                onQuantityChange = { item, newQty ->
                    posViewModel.updateQuantity(item, newQty)
                },
                onRemoveItem = { item ->
                    posViewModel.removeFromCart(item)
                }
            )
        }
    }
}


@Composable
fun ProductList(modifier: Modifier = Modifier, products: List<Asset>, onProductClick: (Asset) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ✅ PERBAIKAN DI SINI: Ubah 'it.localId' menjadi 'it.id'
        items(products.filter { it.quantity > 0 }, key = { it.id }) { product ->
            Card(
                onClick = { onProductClick(product) },
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(product.name, fontWeight = FontWeight.Bold, maxLines = 2)
                    Text("Stok: ${product.quantity}")
                    Text(formatCurrency(product.sellingPrice))
                }
            }
        }
    }
}

@Composable
fun CartPanel(
    modifier: Modifier = Modifier,
    cartItems: List<CartItem>,
    onQuantityChange: (CartItem, Int) -> Unit,
    onRemoveItem: (CartItem) -> Unit
) {
    Column(modifier = modifier.padding(8.dp)) {
        Text("Keranjang", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        if (cartItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Keranjang kosong")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cartItems.size) { index ->
                    val item = cartItems[index]
                    CartItemRow(
                        item = item,
                        onQuantityChange = { newQty -> onQuantityChange(item, newQty) },
                        onRemoveItem = { onRemoveItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemoveItem: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally // Pusatkan semua item
        ) {
            // Nama Produk di paling atas
            Text(item.asset.name, fontWeight = FontWeight.Bold, maxLines = 1)

            Spacer(modifier = Modifier.height(8.dp))

            // Harga Jual di tengah
            Text(
                formatCurrency(item.asset.sellingPrice), // Gunakan harga jual
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tombol-tombol di paling bawah
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Tombol Kurangi
                OutlinedButton(
                    onClick = { onQuantityChange(item.quantity - 1) },
                    modifier = Modifier.size(40.dp), // Ukuran tombol
                    contentPadding = PaddingValues(0.dp) // Hapus padding internal
                ) {
                    Icon(Icons.Default.Remove, "Kurangi")
                }

                // Teks Kuantitas
                Text(
                    item.quantity.toString(),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Tombol Tambah
                OutlinedButton(
                    onClick = { onQuantityChange(item.quantity + 1) },
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Add, "Tambah")
                }

                Spacer(modifier = Modifier.weight(1f)) // Spacer untuk mendorong tombol hapus ke kanan

                // Tombol Hapus
                IconButton(onClick = onRemoveItem) {
                    Icon(Icons.Default.Delete, "Hapus Item", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
@Composable
fun PosBottomBar(
    totalPrice: Double,
    onCompleteSale: () -> Unit,
    isProcessing: Boolean,
    isCartEmpty: Boolean
) {
    BottomAppBar(
        modifier = Modifier.height(80.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Total Harga")
                Text(formatCurrency(totalPrice), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onCompleteSale,
                enabled = !isProcessing && !isCartEmpty,
                modifier = Modifier.height(50.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.ShoppingCartCheckout, "Selesaikan Transaksi")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bayar")
                }
            }
        }
    }
}

fun formatCurrency(amount: Double): String {
    val localeID = Locale("in", "ID")
    val currencyFormat = NumberFormat.getCurrencyInstance(localeID)
    return currencyFormat.format(amount)
}