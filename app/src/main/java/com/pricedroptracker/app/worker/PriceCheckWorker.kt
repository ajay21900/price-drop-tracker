package com.pricedroptracker.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pricedroptracker.app.data.AppDatabase
import com.pricedroptracker.app.notification.NotificationHelper
import com.pricedroptracker.app.scraper.PriceScraper
import kotlinx.coroutines.delay

class PriceCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(context)
        val productDao = database.productDao()

        val activeProducts = productDao.getActiveProducts()
        if (activeProducts.isEmpty()) {
            return Result.success()
        }

        for (product in activeProducts) {
            try {
                val scrapeResult = PriceScraper.scrape(product.url)
                if (scrapeResult.isSuccess) {
                    val scraped = scrapeResult.getOrThrow()
                    val newPrice = scraped.price
                    val oldPrice = product.currentPrice

                    var alertTriggered = product.isAlertTriggered

                    // Check if price met target or dropped below previous price
                    if (newPrice <= product.targetPrice) {
                        if (!product.isAlertTriggered || newPrice < oldPrice) {
                            NotificationHelper.sendPriceAlert(context, product, oldPrice, newPrice)
                            alertTriggered = true
                        }
                    } else if (newPrice < oldPrice && oldPrice > 0) {
                        // Not reached target yet, but dropped
                        NotificationHelper.sendPriceAlert(context, product, oldPrice, newPrice)
                    } else if (newPrice > product.targetPrice) {
                        // Price went back above target, re-arm alert
                        alertTriggered = false
                    }

                    // Update product record
                    val updatedProduct = product.copy(
                        currentPrice = newPrice,
                        title = if (product.title.isBlank() || product.title == "Loading...") scraped.title else product.title,
                        imageUrl = scraped.imageUrl ?: product.imageUrl,
                        storeName = scraped.storeName,
                        lastCheckedTimestamp = System.currentTimeMillis(),
                        isAlertTriggered = alertTriggered
                    )

                    productDao.updateProduct(updatedProduct)
                }
            } catch (_: Exception) {
                // Ignore individual product failure and continue checking next product
            }

            // Politeness delay between scraping individual URLs
            delay(2000)
        }

        return Result.success()
    }
}
