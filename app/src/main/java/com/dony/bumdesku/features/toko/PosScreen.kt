package com.dony.bumdesku.features.toko

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dony.bumdesku.data.Asset
import com.dony.bumdesku.data.CartItem
import kotlinx.coroutines.launch
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

    val categories by posViewModel.productCategories.collectAsState()
    val selectedCategory by posViewModel.selectedCategory.collectAsState()

    val cartItems by posViewModel.cartItems.collectAsState()
    val totalPrice by posViewModel.totalPrice.collectAsState()
    val saleState by posViewModel.saleState.collectAsState()

    // Deklarasikan state dan coroutine scope untuk Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(saleState) {
        when(saleState) {
            SaleState.SUCCESS -> {
                coroutineScope.launch {
                    val snackbarResult = snackbarHostState.showSnackbar(
                        message = "Transaksi berhasil!",
                        actionLabel = "Kembali ke Utama",
                        duration = SnackbarDuration.Short
                    )

                    if (snackbarResult == SnackbarResult.ActionPerformed) {
                        onSaleComplete()
                    }
                }
                posViewModel.resetSaleState()
            }
            SaleState.ERROR -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Gagal memproses transaksi.",
                        actionLabel = "Tutup",
                        duration = SnackbarDuration.Long
                    )
                }
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
                onCompleteSale = { posViewModel.completeSale() },
                isProcessing = saleState == SaleState.LOADING,
                isCartEmpty = cartItems.isEmpty()
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) } // Tambahkan SnackbarHost di sini
    ) {paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.weight(1.5f)) {
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

                if (categories.size > 1) {
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

                ProductList(
                    products = products,
                    onProductClick = { product ->
                        posViewModel.addToCart(product)
                    }
                )
            }

            Divider(modifier = Modifier.padding(horizontal = 8.dp))

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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(item.asset.name, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                formatCurrency(item.asset.sellingPrice),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { onQuantityChange(item.quantity - 1) },
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Remove, "Kurangi")
                }
                Text(
                    item.quantity.toString(),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                OutlinedButton(
                    onClick = { onQuantityChange(item.quantity + 1) },
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Add, "Tambah")
                }
                Spacer(modifier = Modifier.weight(1f))
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