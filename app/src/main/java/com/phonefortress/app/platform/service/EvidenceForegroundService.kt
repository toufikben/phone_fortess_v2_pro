package com.phonefortress.app.platform.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

class EvidenceForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(1001, notification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel("protection", "Phone Fortress protection", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun notification(): Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(this, "protection").setContentTitle("Phone Fortress").setContentText("Protection is active").setSmallIcon(com.phonefortress.app.R.drawable.ic_fortress).build()
    } else {
        @Suppress("DEPRECATION") Notification.Builder(this).setContentTitle("Phone Fortress").setContentText("Protection is active").setSmallIcon(com.phonefortress.app.R.drawable.ic_fortress).build()
    }
}
