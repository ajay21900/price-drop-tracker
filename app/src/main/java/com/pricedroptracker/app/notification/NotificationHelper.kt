package com.pricedroptracker.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pricedroptracker.app.data.Product

object NotificationHelper {

    private const val CHANNEL_ID = "price_drop_alerts_channel"
    private const val CHANNEL_NAME = "Price Drop Alerts"
    private const val CHANNEL_DESC = "Alerts when a product price drops or meets your target price"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendPriceAlert(context: Context, product: Product, oldPrice: Double, newPrice: Double) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open product URL in browser/store app
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(product.url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            product.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isTargetReached = newPrice <= product.targetPrice
        val title = if (isTargetReached) {
            "🎯 Target Reached on ${product.storeName}!"
        } else {
            "📉 Price Dropped on ${product.storeName}!"
        }

        val message = "${product.title}\nNow: ${product.currency}${String.format("%.2f", newPrice)} (Was: ${product.currency}${String.format("%.2f", oldPrice)} | Target: ${product.currency}${String.format("%.2f", product.targetPrice)})"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("Now ${product.currency}${String.format("%.2f", newPrice)}! Tap to open.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(product.id.toInt(), notification)
    }
}
