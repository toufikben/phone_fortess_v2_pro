package com.phonefortress.app.domain.usecase

import com.phonefortress.app.data.prefs.SecurityPrefs
import com.phonefortress.app.domain.model.ThreatAssessment
import com.phonefortress.app.domain.model.ThreatContext
import com.phonefortress.app.domain.model.ThreatFactor
import com.phonefortress.app.domain.model.ThreatLevel
import com.phonefortress.app.platform.ai.FaceDetector
import com.phonefortress.app.util.Logger
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * حالة استخدام: تقييم التهديد من صورة + سياق.
 * تُجمع عوامل متعددة لتكوين درجة 0-100.
 */
@Singleton
class EvaluateThreatUseCase @Inject constructor(
    private val faceDetector: FaceDetector,
    private val securityPrefs: SecurityPrefs
) {

    /**
     * التحليل الرئيسي.
     */
    suspend operator fun invoke(
        photoPath: String?,
        attempts: Int,
        isInSafeZone: Boolean? = null,
        isTest: Boolean = false
    ): ThreatAssessment {
        val threshold = runCatching { securityPrefs.threshold.first() }.getOrDefault(3)

        val context = ThreatContext(
            attempts = attempts,
            threshold = threshold,
            isInSafeZone = isInSafeZone,
            currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            isTest = isTest
        )

        val factors = mutableListOf<ThreatFactor>()
        var facesDetected = 0
        var unknownFace = false
        var brightness = -1f

        // 1. تحليل الصورة
        if (photoPath != null) {
            val faceResult = faceDetector.detect(photoPath)
            if (faceResult != null) {
                facesDetected = faceResult.count

                when {
                    faceResult.count == 0 -> factors.add(ThreatFactor.NO_FACE)
                    faceResult.count > 1 -> factors.add(ThreatFactor.MULTIPLE_FACES)
                    faceResult.confidence < 0.6f -> {
                        factors.add(ThreatFactor.UNKNOWN_FACE)
                        unknownFace = true
                    }
                }
            }

            brightness = faceDetector.averageBrightness(photoPath)
            if (brightness in 0f..0.25f) {
                factors.add(ThreatFactor.LOW_LIGHT)
            }
        } else {
            factors.add(ThreatFactor.NO_FACE)
        }

        // 2. عوامل السياق
        if (isInSafeZone == false) factors.add(ThreatFactor.OUTSIDE_SAFE_ZONE)
        if (attempts >= threshold * 2) factors.add(ThreatFactor.HIGH_ATTEMPTS)
        if (isUnusualTime(context.currentHour)) factors.add(ThreatFactor.UNUSUAL_TIME)
        if (isTest) factors.add(ThreatFactor.TEST_MODE)

        // 3. حساب الدرجة
        val rawScore = factors.sumOf { it.weight }.coerceIn(0, 100)
        val level = scoreToLevel(rawScore)

        Logger.i("Threat evaluated: $rawScore ($level) — ${factors.size} factors")

        return ThreatAssessment(
            score = rawScore,
            level = level,
            reasons = factors.map { it.labelAr },
            facesDetected = facesDetected,
            unknownFace = unknownFace
        )
    }

    private fun isUnusualTime(hour: Int): Boolean {
        if (hour < 0) return false
        return hour in 2..5  // ساعات الفجر
    }

    private fun scoreToLevel(score: Int): ThreatLevel = when {
        score >= 80 -> ThreatLevel.CRITICAL
        score >= 60 -> ThreatLevel.HIGH
        score >= 30 -> ThreatLevel.MEDIUM
        else -> ThreatLevel.LOW
    }
}
