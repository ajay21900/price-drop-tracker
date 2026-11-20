package com.pricedroptracker.app.scraper

data class ScrapedProduct(
    val title: String,
    val price: Double,
    val currency: String = "₹",
    val imageUrl: String? = null,
    val storeName: String = "Store",
    val isAvailable: Boolean = true
)
