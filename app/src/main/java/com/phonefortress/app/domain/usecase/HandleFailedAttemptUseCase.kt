package com.phonefortress.app.domain.usecase

import com.phonefortress.app.data.prefs.SecurityPrefs
import com.phonefortress.app.util.Logger
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * حالة استخدام: معالجة محاولة فاشلة (للاختبار اليدوي أو من مصادر أخرى).
 */
@Singleton
class HandleFailedAttemptUseCase @Inject constructor(
    private val captureUseCase: CaptureEvidenceUseCase,
    private val securityPrefs: SecurityPrefs
) {

    suspend operator fun invoke(isTest: Boolean = false): Boolean {
        return try {
            val attempts = securityPrefs.incrementAttempts()
            val threshold = securityPrefs.threshold.first()

            Logger.i("Attempt $attempts / $threshold (test=$isTest)")

            if (attempts >= threshold) {
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
