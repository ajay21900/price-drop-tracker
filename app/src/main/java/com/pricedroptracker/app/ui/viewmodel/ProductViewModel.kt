package com.pricedroptracker.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pricedroptracker.app.data.AppDatabase
import com.pricedroptracker.app.data.Product
import com.pricedroptracker.app.data.ProductRepository
import com.pricedroptracker.app.scraper.PriceScraper
import com.pricedroptracker.app.scraper.ScrapedProduct
import com.pricedroptracker.app.worker.WorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProductRepository

    val products: StateFlow<List<Product>>

    private val _previewProduct = MutableStateFlow<ScrapedProduct?>(null)
    val previewProduct: StateFlow<ScrapedProduct?> = _previewProduct.asStateFlow()

    private val _isLoadingPreview = MutableStateFlow(false)
    val isLoadingPreview: StateFlow<Boolean> = _isLoadingPreview.asStateFlow()

    private val _previewError = MutableStateFlow<String?>(null)
    val previewError: StateFlow<String?> = _previewError.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ProductRepository(database.productDao())
        products = repository.allProducts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    }

    fun fetchPreview(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _isLoadingPreview.value = true
            _previewError.value = null
            _previewProduct.value = null

            val result = PriceScraper.scrape(url)
            result.onSuccess { scraped ->
                _previewProduct.value = scraped
                _previewError.value = null
            }.onFailure { error ->
                _previewError.value = error.message ?: "Failed to fetch product details"
                _previewProduct.value = null
            }
            _isLoadingPreview.value = false
        }
    }

    fun clearPreview() {
        _previewProduct.value = null
        _previewError.value = null
        _isLoadingPreview.value = false
    }

    fun addProduct(
        url: String,
        title: String,
        currentPrice: Double,
        targetPrice: Double,
        currency: String,
        imageUrl: String?,
        storeName: String
    ) {
        viewModelScope.launch {
            val product = Product(
                url = url,
                title = title,
                currentPrice = currentPrice,
                initialPrice = currentPrice,
                targetPrice = targetPrice,
                currency = currency,
                imageUrl = imageUrl,
                storeName = storeName,
                lastCheckedTimestamp = System.currentTimeMillis()
            )
            repository.insertProduct(product)
            clearPreview()
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun toggleProductActive(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product.copy(isActive = !product.isActive))
        }
    }

    fun refreshAllPrices() {
        viewModelScope.launch {
            _isRefreshing.value = true
            WorkScheduler.triggerImmediatePriceCheck(getApplication())
            kotlinx.coroutines.delay(2000)
            _isRefreshing.value = false
        }
    }
}
