package com.pricedroptracker.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val currentPrice: Double,
    val initialPrice: Double,
    val targetPrice: Double,
    val currency: String = "₹",
    val imageUrl: String? = null,
    val storeName: String = "Store",
    val lastCheckedTimestamp: Long = System.currentTimeMillis(),
    val isAlertTriggered: Boolean = false,
    val isActive: Boolean = true
) {
    val isTargetMet: Boolean
        get() = currentPrice > 0 && currentPrice <= targetPrice

    val discountPercentage: Int
        get() {
            if (initialPrice <= 0 || currentPrice >= initialPrice) return 0
            return (((initialPrice - currentPrice) / initialPrice) * 100).toInt()
        }
}
