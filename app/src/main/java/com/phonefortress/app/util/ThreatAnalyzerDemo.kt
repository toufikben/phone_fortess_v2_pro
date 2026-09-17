package com.phonefortress.app.util

import com.phonefortress.app.domain.usecase.EvaluateThreatUseCase
import javax.inject.Inject

/**
 * اختبار يدوي — لا يُستدعى تلقائياً.
 * استخدمه من Android Studio Debugger أو وحدة اختبار.
 */
class ThreatAnalyzerDemo @Inject constructor(
    private val evaluateThreatUseCase: EvaluateThreatUseCase
) {
    suspend fun runDemo(photoPath: String?) {
        val result = evaluateThreatUseCase(
            photoPath = photoPath,
            attempts = 5,
            isInSafeZone = false,
            isTest = true
        )
        Logger.i("DEMO: threat evaluation completed")
        return result
    }
}
