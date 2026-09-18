package com.phonefortress.app.alerts.channels

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.phonefortress.app.MainActivity
import com.phonefortress.app.R
import com.phonefortress.app.alerts.template.AlertTemplate
import com.phonefortress.app.data.prefs.AlertPrefs
import com.phonefortress.app.domain.model.AlertChannel
import com.phonefortress.app.domain.model.AlertPayload
import com.phonefortress.app.domain.model.AlertResult
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * قناة إشعار محلي — تعمل دائماً كحل احتياطي.
 */
@Singleton
class LocalNotificationChannel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: AlertPrefs
) : AlertChannel {

    override val id: String = "local"
    override val displayName: String = "إشعار محلي"
    override val requiresConfig: Boolean = false

    override suspend fun isEnabled(): Boolean = prefs.localEnabled.first()

    override suspend fun isConfigured(): Boolean = isEnabled()

    override suspend fun send(event: SecurityEvent, payload: AlertPayload): AlertResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return AlertResult.Fatal("Notification permission denied", id)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("phone_fortress_event_id", event.id)
            data = android.net.Uri.parse("phonefortress://event/${event.id}")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, event.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(payload.title)
            .setContentText(payload.body.lineSequence().take(3).joinToString(" "))
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        val notificationId = (event.id.hashCode() and Int.MAX_VALUE).coerceAtLeast(1)
        manager.notify(notificationId, notification)
        return AlertResult.Success("local-${event.id}", id)
    }

    override suspend fun test(): AlertResult {
        val now = System.currentTimeMillis()
        val testEvent = SecurityEvent(
            id = "test-${now}",
            timestamp = now,
            failedAttempts = 3, threshold = 3, isTest = true,
            threatScore = 50,
            threatLevel = com.phonefortress.app.domain.model.ThreatLevel.MEDIUM
        )
        return send(testEvent, AlertTemplate.build(testEvent))
    }
}
