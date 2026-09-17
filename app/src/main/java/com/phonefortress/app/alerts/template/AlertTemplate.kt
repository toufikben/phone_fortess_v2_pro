package com.phonefortress.app.alerts.template

import com.phonefortress.app.domain.model.AlertPayload
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.domain.model.ThreatLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * قوالب نصية جاهزة لبناء محتوى التنبيه.
 */
object AlertTemplate {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun build(event: SecurityEvent): AlertPayload {
        val title = if (event.isTest) "🧪 اختبار تنبيه" else "🚨 محاولة فتح غير مصرح بها"
        val body = buildString {
            appendLine("🛡️ Phone Fortress")
            appendLine()
            if (event.isTest) {
                appendLine("هذا تنبيه تجريبي للتحقق من إعدادات القناة.")
            } else {
                appendLine("تم رصد ${event.failedAttempts} محاولات فتح فاشلة.")
            }
            appendLine()
            appendLine("⏰ الوقت: ${dateFormat.format(Date(event.timestamp))}")
            appendLine("${event.threatLevel.emoji} مستوى الخطر: ${levelName(event.threatLevel)} (${event.threatScore}/100)")
            if (event.threatReasons.isNotEmpty()) {
                appendLine()
                appendLine("🔍 أسباب التقييم:")
                event.threatReasons.forEach { reason ->
                    appendLine("  • $reason")
                }
            }
            appendLine("🔢 عدد المحاولات: ${event.failedAttempts} / ${event.threshold}")

            if (event.latitude != null && event.longitude != null) {
                appendLine("📍 الموقع: ${"%.4f".format(event.latitude)}, ${"%.4f".format(event.longitude)}")
                appendLine("🗺️ الخريطة: https://maps.google.com/?q=${event.latitude},${event.longitude}")
            } else {
                appendLine("📍 الموقع: غير متاح")
            }

            appendLine("📷 الصورة: ${if (event.photoPath != null) "مرفقة" else "غير متاحة"}")
            appendLine("🎤 التسجيل الصوتي: ${if (event.audioPath != null) "مرفق" else "غير متاح"}")
            appendLine()
            appendLine("— رسالة تلقائية من Phone Fortress")
        }
        return AlertPayload(
            title = title,
            body = body,
            photoPath = event.photoPath,
            audioPath = event.audioPath,
            latitude = event.latitude,
            longitude = event.longitude,
            eventId = event.id,
            timestamp = event.timestamp
        )
    }

    private fun levelName(level: ThreatLevel) = when (level) {
        ThreatLevel.LOW -> "منخفض"
        ThreatLevel.MEDIUM -> "متوسط"
        ThreatLevel.HIGH -> "مرتفع"
        ThreatLevel.CRITICAL -> "حرج"
    }
}
