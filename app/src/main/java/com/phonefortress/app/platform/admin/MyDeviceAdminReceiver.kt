package com.phonefortress.app.platform.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.phonefortress.app.geofence.ZoneStateHolder
import com.phonefortress.app.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * مستقبل Device Admin — يستقبل أحداث محاولات القفل الفاشلة/الناجحة.
 * يُطلق مسار التقاط الأدلة عند بلوغ العتبة.
 */
@AndroidEntryPoint
class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    @Inject lateinit var captureUseCase: com.phonefortress.app.domain.usecase.CaptureEvidenceUseCase
    @Inject lateinit var securityPrefs: com.phonefortress.app.data.prefs.SecurityPrefs
    @Inject lateinit var zoneState: ZoneStateHolder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        Logger.i("Password failed event received")

        scope.launch {
            try {
                val attempts = securityPrefs.incrementAttempts()
                val defaultThreshold = securityPrefs.threshold.first()
                val effectiveThreshold = zoneState.effectiveThreshold(defaultThreshold)

                Logger.d("Attempt $attempts / threshold $effectiveThreshold (default=$defaultThreshold)")

                if (attempts >= effectiveThreshold) {
                    captureUseCase.start(attempts, isTest = false)
                    securityPrefs.resetAttempts()
                }
            } catch (e: Exception) {
                Logger.e(e, "Password failed handling error")
            }
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        Logger.i("Password succeeded — resetting counter")
        scope.launch {
            runCatching { securityPrefs.resetAttempts() }
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Logger.i("Device Admin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Logger.i("Device Admin disabled")
    }
}
