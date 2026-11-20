package com.pricedroptracker.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pricedroptracker.app.ui.components.AddProductDialog
import com.pricedroptracker.app.ui.components.ProductCard
import com.pricedroptracker.app.ui.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ProductViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoadingPreview by viewModel.isLoadingPreview.collectAsState()
    val previewProduct by viewModel.previewProduct.collectAsState()
    val previewError by viewModel.previewError.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }

    val filterOptions = listOf("All", "Target Met", "Tracking")

    val filteredProducts = when (selectedFilterIndex) {
        1 -> products.filter { it.isTargetMet }
        2 -> products.filter { !it.isTargetMet && it.isActive }
        else -> products
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Price Drop Tracker", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshAllPrices() },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Check Prices Now"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.clearPreview()
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Track New Product")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Tabs
            if (products.isNotEmpty()) {
                TabRow(
                    selectedTabIndex = selectedFilterIndex,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    filterOptions.forEachIndexed { index, title ->
                        val count = when (index) {
                            0 -> products.size
                            1 -> products.count { it.isTargetMet }
                            2 -> products.count { !it.isTargetMet && it.isActive }
                            else -> 0
                        }
                        Tab(
                            selected = selectedFilterIndex == index,
                            onClick = { selectedFilterIndex = index },
                            text = { Text("$title ($count)") }
                        )
                    }
                }
            }

            // Product List or Empty State
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No products tracked yet!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the + button below to paste an Amazon or Flipkart link and set your target drop price.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            onDelete = { viewModel.deleteProduct(it) }
                        )
                    }
                }
            }
        }

        // Add Product Dialog
        if (showAddDialog) {
            AddProductDialog(
                isLoading = isLoadingPreview,
                previewProduct = previewProduct,
                errorMessage = previewError,
                onFetchPreview = { url -> viewModel.fetchPreview(url) },
                onSaveProduct = { url, title, curPrice, targetPrice, currency, imageUrl, store ->
                    viewModel.addProduct(url, title, curPrice, targetPrice, currency, imageUrl, store)
                    showAddDialog = false
                },
                onDismiss = {
                    showAddDialog = false
                    viewModel.clearPreview()
                }
            )
        }
    }
}
