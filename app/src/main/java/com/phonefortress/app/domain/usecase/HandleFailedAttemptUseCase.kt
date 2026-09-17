package com.phonefortress.app.domain.usecase

import com.phonefortress.app.data.prefs.SecurityPrefs
import com.phonefortress.app.util.Logger
import com.phonefortress.app.geofence.ZoneStateHolder
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * حالة استخدام: معالجة محاولة فاشلة (للاختبار اليدوي أو من مصادر أخرى).
 */
@Singleton
class HandleFailedAttemptUseCase @Inject constructor(
    private val captureUseCase: CaptureEvidenceUseCase,
    private val securityPrefs: SecurityPrefs,
    private val zoneState: ZoneStateHolder
) {

    suspend operator fun invoke(isTest: Boolean = false): Boolean {
        return try {
            val configuredThreshold = securityPrefs.threshold.first()
            val threshold = zoneState.effectiveThreshold(configuredThreshold)
            val triggeringAttempts = securityPrefs.incrementAttemptsAndCheckThreshold(threshold)
            val attempts = securityPrefs.consecutiveAttempts.first()

            Logger.i("Attempt $attempts / $threshold (test=$isTest)")

            if (triggeringAttempts != null) {
                captureUseCase.start(attempts, isTest)
                securityPrefs.resetAttempts()
                true
            } else false
        } catch (e: Exception) {
            Logger.e(e, "Failed attempt handling error")
            false
        }
    }
}
