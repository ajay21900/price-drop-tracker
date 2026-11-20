package com.pricedroptracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.pricedroptracker.app.scraper.ScrapedProduct
import com.pricedroptracker.app.ui.theme.PriceGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(
    isLoading: Boolean,
    previewProduct: ScrapedProduct?,
    errorMessage: String?,
    onFetchPreview: (String) -> Unit,
    onSaveProduct: (
        url: String,
        title: String,
        currentPrice: Double,
        targetPrice: Double,
        currency: String,
        imageUrl: String?,
        storeName: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var urlInput by remember { mutableStateOf("") }
    var targetPriceInput by remember { mutableStateOf("") }

    // When preview arrives, default the target price to 5% below current price
    LaunchedEffect(previewProduct) {
        if (previewProduct != null && targetPriceInput.isBlank()) {
            val suggested = (previewProduct.price * 0.95).toInt()
            targetPriceInput = suggested.toString()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Dialog Title Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Track New Product",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // URL Input Field
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Product URL (Amazon, Flipkart...)") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Fetch Details Button
                Button(
                    onClick = { onFetchPreview(urlInput) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = urlInput.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fetching details...")
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fetch Product Info")
                    }
                }

                // Error Message Display
                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Product Preview Card
                if (previewProduct != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!previewProduct.imageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = previewProduct.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = previewProduct.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Current: ${previewProduct.currency}${String.format("%.2f", previewProduct.price)}",
                                    fontWeight = FontWeight.Bold,
                                    color = PriceGreen,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Target Price Input
                    OutlinedTextField(
                        value = targetPriceInput,
                        onValueChange = { targetPriceInput = it },
                        label = { Text("Notify me when price drops to (${previewProduct.currency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Discount Helpers (-5%, -10%, -20%)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(5, 10, 20).forEach { discountPercent ->
                            val discountedPrice = (previewProduct.price * (1.0 - discountPercent / 100.0)).toInt()
                            OutlinedButton(
                                onClick = { targetPriceInput = discountedPrice.toString() },
                                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text("-$discountPercent%", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Final Save Button
                    Button(
                        onClick = {
                            val targetPrice = targetPriceInput.toDoubleOrNull() ?: 0.0
                            if (targetPrice > 0) {
                                onSaveProduct(
                                    urlInput,
                                    previewProduct.title,
                                    previewProduct.price,
                                    targetPrice,
                                    previewProduct.currency,
                                    previewProduct.imageUrl,
                                    previewProduct.storeName
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PriceGreen),
                        enabled = (targetPriceInput.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("Start Tracking", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
