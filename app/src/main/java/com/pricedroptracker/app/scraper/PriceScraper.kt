package com.pricedroptracker.app.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.util.regex.Pattern

object PriceScraper {

    private const val MOBILE_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; SM-A536B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36"

    private const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    suspend fun scrape(url: String): Result<ScrapedProduct> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = normalizeUrl(url)
            val domain = extractDomain(cleanUrl)

            when {
                domain.contains("amazon") -> scrapeAmazon(cleanUrl)
                domain.contains("flipkart") -> scrapeFlipkart(cleanUrl)
                else -> scrapeGeneric(cleanUrl)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun scrapeAmazon(url: String): Result<ScrapedProduct> {
        val doc: Document = Jsoup.connect(url)
            .userAgent(DESKTOP_USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Sec-Ch-Ua-Mobile", "?0")
            .timeout(20000)
            .followRedirects(true)
            .get()

        // Extract Title
        val title = doc.select("#productTitle").text().ifBlank {
            doc.select("h1#title").text().ifBlank {
                doc.select("meta[property=og:title]").attr("content").ifBlank {
                    doc.title()
                }
            }
        }.trim()

        // Extract Price
        val priceSelectors = listOf(
            ".a-price .a-offscreen",
            "#corePrice_desktop .a-price .a-offscreen",
            "#corePriceDisplay_desktop_feature_div .a-price .a-offscreen",
            "span.apexPriceToPay span.a-offscreen",
            "#priceblock_ourprice",
            "#priceblock_dealprice",
            "#priceblock_saleprice",
            "#price"
        )

        var rawPrice = ""
        for (selector in priceSelectors) {
            val element = doc.selectFirst(selector)
            if (element != null && element.text().isNotBlank()) {
                rawPrice = element.text()
                break
            }
        }

        // Meta fallback for Amazon
        if (rawPrice.isBlank()) {
            val metaPrice = doc.select("meta[name=twitter:data1]").attr("content")
            if (metaPrice.isNotBlank() && metaPrice.any { it.isDigit() }) {
                rawPrice = metaPrice
            }
        }

        val (price, currency) = parsePriceAndCurrency(rawPrice)

        // Extract Image
        val imageUrl = doc.select("#landingImage").attr("src").ifBlank {
            doc.select("#imgBlkFront").attr("src").ifBlank {
                doc.select("meta[property=og:image]").attr("content")
            }
        }

        if (price <= 0.0) {
            return Result.failure(Exception("Could not extract price from Amazon page. Amazon may have served a captcha or layout changed."))
        }

        return Result.success(
            ScrapedProduct(
                title = title,
                price = price,
                currency = currency,
                imageUrl = imageUrl.ifBlank { null },
                storeName = "Amazon"
            )
        )
    }

    private fun scrapeFlipkart(url: String): Result<ScrapedProduct> {
        val doc: Document = Jsoup.connect(url)
            .userAgent(MOBILE_USER_AGENT)
            .header("Accept-Language", "en-US,en;q=0.9")
            .timeout(20000)
            .followRedirects(true)
            .get()

        // 1. Try JSON-LD Schema First (Flipkart has solid structured JSON-LD)
        val jsonLdScripts = doc.select("script[type=application/ld+json]")
        for (script in jsonLdScripts) {
            try {
                val json = JSONObject(script.data())
                if (json.optString("@type") == "Product" || json.has("offers")) {
                    val offers = json.optJSONObject("offers")
                    val priceVal = offers?.optDouble("price", -1.0) ?: json.optDouble("price", -1.0)
                    val priceCurr = offers?.optString("priceCurrency", "₹") ?: "₹"
                    val title = json.optString("name", "")
                    val image = json.optString("image", "")

                    if (priceVal > 0) {
                        return Result.success(
                            ScrapedProduct(
                                title = title.ifBlank { doc.title() }.trim(),
                                price = priceVal,
                                currency = if (priceCurr.equals("INR", true)) "₹" else priceCurr,
                                imageUrl = image.ifBlank { null },
                                storeName = "Flipkart"
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // Continue to CSS selectors
            }
        }

        // 2. CSS Selectors for Flipkart
        val priceSelectors = listOf(
            "div.Nx9bqj.CxhGGd",
            "div._30jeq3._16Jk6d",
            "div.Nx9bqj",
            "div._30jeq3",
            "div._25b18c div._30jeq3"
        )

        var rawPrice = ""
        for (selector in priceSelectors) {
            val element = doc.selectFirst(selector)
            if (element != null && element.text().isNotBlank()) {
                rawPrice = element.text()
                break
            }
        }

        val title = doc.select("span.VU-ZEz").text().ifBlank {
            doc.select("span.B_NuCI").text().ifBlank {
                doc.select("meta[property=og:title]").attr("content").ifBlank {
                    doc.title()
                }
            }
        }.trim()

        val imageUrl = doc.select("img._396cs4").attr("src").ifBlank {
            doc.select("img.DByuf4").attr("src").ifBlank {
                doc.select("meta[property=og:image]").attr("content")
            }
        }

        val (price, currency) = parsePriceAndCurrency(rawPrice)

        if (price <= 0.0) {
            return Result.failure(Exception("Could not extract price from Flipkart page."))
        }

        return Result.success(
            ScrapedProduct(
                title = title,
                price = price,
                currency = currency,
                imageUrl = imageUrl.ifBlank { null },
                storeName = "Flipkart"
            )
        )
    }

    private fun scrapeGeneric(url: String): Result<ScrapedProduct> {
        val doc: Document = Jsoup.connect(url)
            .userAgent(DESKTOP_USER_AGENT)
            .timeout(20000)
            .followRedirects(true)
            .get()

        // 1. Try OpenGraph / Schema Meta
        val metaPrice = doc.select("meta[property=og:price:amount]").attr("content").ifBlank {
            doc.select("meta[property=product:price:amount]").attr("content")
        }
        val metaCurrency = doc.select("meta[property=og:price:currency]").attr("content").ifBlank {
            doc.select("meta[property=product:price:currency]").attr("content").ifBlank { "₹" }
        }

        var price = metaPrice.toDoubleOrNull() ?: 0.0
        var currency = if (metaCurrency.equals("INR", true)) "₹" else metaCurrency

        // 2. Selectors fallback
        if (price <= 0.0) {
            val fallbackSelectors = listOf(
                "[itemprop=price]",
                ".price",
                ".product-price",
                ".offer-price",
                "#price"
            )
            for (selector in fallbackSelectors) {
                val element = doc.selectFirst(selector)
                if (element != null) {
                    val parsed = parsePriceAndCurrency(element.text())
                    if (parsed.first > 0) {
                        price = parsed.first
                        currency = parsed.second
                        break
                    }
                }
            }
        }

        val title = doc.select("meta[property=og:title]").attr("content").ifBlank {
            doc.title()
        }.trim()

        val image = doc.select("meta[property=og:image]").attr("content")

        if (price <= 0.0) {
            return Result.failure(Exception("Could not detect product price on this page."))
        }

        return Result.success(
            ScrapedProduct(
                title = title,
                price = price,
                currency = currency,
                imageUrl = image.ifBlank { null },
                storeName = extractDomain(url).replace("www.", "").substringBefore(".")
            )
        )
    }

    fun parsePriceAndCurrency(raw: String): Pair<Double, String> {
        if (raw.isBlank()) return Pair(0.0, "₹")

        val currency = when {
            raw.contains("₹") || raw.contains("Rs") || raw.contains("INR") -> "₹"
            raw.contains("$") || raw.contains("USD") -> "$"
            raw.contains("€") || raw.contains("EUR") -> "€"
            raw.contains("£") || raw.contains("GBP") -> "£"
            else -> "₹"
        }

        // Match numeric digits with commas and dots
        val pattern = Pattern.compile("[0-9]{1,3}(,[0-9]{3})*(\\.[0-9]+)?|[0-9]+(\\.[0-9]+)?")
        val matcher = pattern.matcher(raw)

        if (matcher.find()) {
            val numStr = matcher.group().replace(",", "")
            val parsedDouble = numStr.toDoubleOrNull() ?: 0.0
            return Pair(parsedDouble, currency)
        }

        return Pair(0.0, currency)
    }

    private fun normalizeUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://$clean"
        }
        return clean
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            uri.host?.lowercase() ?: ""
        } catch (_: Exception) {
            ""
        }
    }
}
