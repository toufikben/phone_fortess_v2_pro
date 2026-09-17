package com.phonefortress.app.domain.model

import kotlinx.serialization.Serializable

/**
 * تقييم التهديد — نتيجة تحليل الصورة والسياق.
 */
@Serializable
data class ThreatAssessment(
    val score: Int,                  // 0-100
    val level: ThreatLevel,
    val reasons: List<String>,       // الأسباب المقروءة
    val facesDetected: Int = 0,
    val unknownFace: Boolean = false,
    val capturedAt: Long = System.currentTimeMillis()
) {
    companion object {
        val EMPTY = ThreatAssessment(
            score = 0,
            level = ThreatLevel.LOW,
            reasons = emptyList()
        )
    }
}

/**
 * عوامل التقييم — تُجمع لتكوين الدرجة النهائية.
 */
enum class ThreatFactor(val weight: Int, val labelAr: String) {
    NO_FACE(20, "لم يتم اكتشاف أي وجه (كاميرا محجوبة)"),
    MULTIPLE_FACES(30, "أكثر من شخص أمام الجهاز"),
    UNKNOWN_FACE(40, "وجه غير معروف"),
    OUTSIDE_SAFE_ZONE(20, "خارج المنطقة الآمنة"),
    UNUSUAL_TIME(15, "وقت غير معتاد"),
    HIGH_ATTEMPTS(25, "عدد محاولات مرتفع"),
    LOW_LIGHT(10, "إضاءة منخفضة"),
    TEST_MODE(0, "وضع الاختبار")
}

/**
 * سياق التحليل — معلومات إضافية تساهم في التقييم.
 */
data class ThreatContext(
    val attempts: Int = 0,
    val threshold: Int = 3,
    val isInSafeZone: Boolean? = null,   // null = غير معروف
    val currentHour: Int = -1,           // -1 = غير معروف
    val typicalHours: Set<Int> = emptySet(),
    val averageBrightness: Float = -1f,  // -1 = غير معروف
    val isTest: Boolean = false
)
