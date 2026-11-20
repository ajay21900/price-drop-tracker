package com.pricedroptracker.app

import android.app.Application
import com.pricedroptracker.app.notification.NotificationHelper
import com.pricedroptracker.app.worker.WorkScheduler

class PriceDropApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Android Notification Channel
        NotificationHelper.createNotificationChannel(this)

        // 2. Schedule Background Periodic Work (Checks prices every 1 hour)
        WorkScheduler.schedulePeriodicPriceCheck(this, intervalHours = 1)
    }
}
