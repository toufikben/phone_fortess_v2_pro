package com.phonefortress.app.platform.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.phonefortress.app.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BiometricAuthenticator @Inject constructor(@ApplicationContext private val context: Context) {
    fun isAvailable(): Boolean = BiometricManager.from(context).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
    ) == BiometricManager.BIOMETRIC_SUCCESS

    suspend fun authenticate(activity: FragmentActivity, title: String = "التحقق من الهوية", subtitle: String = "استخدم البصمة أو الوجه"): Boolean = suspendCancellableCoroutine { continuation ->
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(context), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { Logger.i("Biometric success"); if (continuation.isActive) continuation.resume(true) }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { Logger.w("Biometric authentication ended"); if (continuation.isActive) continuation.resume(false) }
            override fun onAuthenticationFailed() { Logger.w("Biometric authentication failed") }
        })
        val info = BiometricPrompt.PromptInfo.Builder().setTitle(title).setSubtitle(subtitle).setNegativeButtonText("إلغاء").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK).build()
        try { prompt.authenticate(info) } catch (e: Exception) { Logger.e(e, "Biometric prompt failed"); if (continuation.isActive) continuation.resume(false) }
    }
}
