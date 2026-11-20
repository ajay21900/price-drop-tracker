package com.pricedroptracker.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pricedroptracker.app.data.Product
import com.pricedroptracker.app.ui.theme.AmazonOrange
import com.pricedroptracker.app.ui.theme.FlipkartBlue
import com.pricedroptracker.app.ui.theme.PriceGreen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProductCard(
    product: Product,
    onDelete: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storeBadgeColor = when {
        product.storeName.contains("Amazon", ignoreCase = true) -> AmazonOrange
        product.storeName.contains("Flipkart", ignoreCase = true) -> FlipkartBlue
        else -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Store badge and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Store Tag
                Box(
                    modifier = Modifier
                        .background(storeBadgeColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = product.storeName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Target Reached Badge
                if (product.isTargetMet) {
                    Box(
                        modifier = Modifier
                            .background(PriceGreen, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🎯 TARGET REACHED",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Middle Row: Image + Title + Price Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Product Image
                if (!product.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.title,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Title and Prices
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Current Price
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${product.currency}${String.format("%.2f", product.currentPrice)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (product.isTargetMet) PriceGreen else MaterialTheme.colorScheme.onSurface
                        )

                        if (product.discountPercentage > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "-${product.discountPercentage}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PriceGreen
                            )
                        }
                    }

                    // Target Price
                    Text(
                        text = "Target: ${product.currency}${String.format("%.2f", product.targetPrice)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Row: Last Checked & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val timeFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                val lastCheckedStr = timeFormat.format(Date(product.lastCheckedTimestamp))

                Text(
                    text = "Checked: $lastCheckedStr",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Row {
                    // Open in Browser/App
                    IconButton(onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(product.url))
                        context.startActivity(browserIntent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Open in Store",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Delete
                    IconButton(onClick = { onDelete(product) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Product",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
