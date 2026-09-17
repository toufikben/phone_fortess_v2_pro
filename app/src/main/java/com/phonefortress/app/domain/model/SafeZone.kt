package com.phonefortress.app.domain.model

import kotlinx.serialization.Serializable

/**
 * منطقة جغرافية ذكية.
 * كل منطقة لها عتبة محاولات خاصة تُطبَّق عند التواجد داخلها.
 */
@Serializable
data class SafeZone(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val type: ZoneType,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * يحسب العتبة الفعلية بناءً على نوع المنطقة.
     */
    fun effectiveThreshold(): Int = when (type) {
        ZoneType.SAFE -> 5
        ZoneType.NEUTRAL -> 3
        ZoneType.DANGER -> 1
    }
}

enum class ZoneType {
    SAFE,      // آمنة: عتبة عالية (5) — تقليل الإزعاج
    NEUTRAL,   // محايدة: عتبة متوسطة (3)
    DANGER;    // خطر: عتبة منخفضة (1) — حساسية عالية

    val labelAr: String
        get() = when (this) {
            SAFE -> "آمنة"
            NEUTRAL -> "محايدة"
            DANGER -> "خطر"
        }

    val emoji: String
        get() = when (this) {
            SAFE -> "🟢"
            NEUTRAL -> "🟡"
            DANGER -> "🔴"
        }
}
