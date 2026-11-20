# Price Drop Tracker 📉 (Android APK)

A native Android application built with **Jetpack Compose**, **Room Database**, **Jsoup**, and **Android WorkManager** that tracks product prices from **Amazon**, **Flipkart**, and other e-commerce sites, sending high-priority local notifications when the price drops or hits your target!

---

## 🚀 Features

- **Multi-Store Support**: Specialized scrapers for **Amazon** (including mobile/desktop price blocks, ASIN handling) and **Flipkart** (JSON-LD structured data & dynamic selectors), plus a generic OpenGraph/schema fallback for other stores.
- **Instant Preview**: Paste any product URL and immediately fetch the title, product image thumbnail, and live price before saving.
- **Custom Target Price**: Set any target price you want to be alerted on, with quick percentage discount shortcuts (`-5%`, `-10%`, `-20%`).
- **Autonomous Background Worker**: Uses Android `WorkManager` to periodically check prices in the background without draining your battery (requires network connectivity).
- **Rich Notifications**: Sends high-priority notifications with direct tap actions to open the deal in your browser or store app.
- **On-Demand Price Checks**: Pull-to-refresh or tap the refresh button anytime to immediately check all tracked products.
- **Offline-First Storage**: Powered by Room SQLite database so all your tracked items and price history are saved securely on your phone.

---

## 📦 How to Build the APK via GitHub Actions (Zero Local Setup)

You don't need Android Studio or gigabytes of SDKs installed on your computer. A preconfigured **GitHub Actions CI/CD pipeline** is included in `.github/workflows/build-apk.yml`.

### Step 1: Create a GitHub Repository
1. Go to [github.com/new](https://github.com/new) and create a new repository (e.g. `price-drop-tracker`).
2. Make it either **Public** or **Private** (GitHub Actions is free for both!).

### Step 2: Push or Upload this Project
If you have Git installed:
```bash
cd C:\Users\ajay2\.gemini\antigravity\scratch\price-drop-tracker
git init
git add .
git commit -m "Initial Price Drop Tracker commit"
git branch -M main
git remote add origin https://github.com/<your-username>/price-drop-tracker.git
git push -u origin main
```
*Tip*: You can also drag-and-drop the files directly on GitHub or use **GitHub Desktop**.

### Step 3: Download Your APK
1. Open your repository on GitHub.
2. Click on the **Actions** tab at the top.
3. You will see the **Build Android APK** workflow running. It compiles the app in ~2 minutes.
4. Once finished (green checkmark), click on the workflow run.
5. Scroll down to **Artifacts** and download `PriceDropTracker-Debug-APK`.
6. Transfer the `.apk` file to your Android phone, tap to install, and start tracking prices!

---

## 🛠️ Architecture & Tech Stack

| Layer | Technology |
|---|---|
| **UI** | Jetpack Compose + Material 3 |
| **Language** | Kotlin 1.9.22 |
| **Image Loading** | Coil Compose |
| **Local Database** | Room 2.6.1 (KSP) |
| **Web Scraping** | Jsoup 1.17.2 with Mobile & Desktop header spoofing |
| **Background Processing**| Android WorkManager 2.9.0 |
| **Notifications** | Android NotificationManager (Channel: `price_drop_alerts_channel`) |
| **Target Android OS** | Android 8.0 (API 26) up to Android 14 (API 34) |

---

## 🔋 Android Battery Optimization Tip

To ensure Android does not throttle background checks when your phone is in deep sleep (Doze mode):
1. On your phone, go to **Settings > Apps > Price Drop Tracker**.
2. Tap **Battery**.
3. Select **Unrestricted** (or turn off "Battery Optimization" for this app).
