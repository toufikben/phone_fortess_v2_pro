package com.phonefortress.app.platform.admin

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
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

@AndroidEntryPoint
class MyDeviceAdminReceiver : DeviceAdminReceiver() {
    @Inject lateinit var captureUseCase: com.phonefortress.app.domain.usecase.CaptureEvidenceUseCase
    @Inject lateinit var securityPrefs: com.phonefortress.app.data.prefs.SecurityPrefs
    @Inject lateinit var zoneState: ZoneStateHolder

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        runAsync {
            try {
                if (!securityPrefs.protectionEnabled.first() || !isAdminActive(context)) return@runAsync
                val configuredThreshold = securityPrefs.threshold.first()
                val effectiveThreshold = zoneState.effectiveThreshold(configuredThreshold)
                val evaluation = securityPrefs.incrementAttemptsAndCheckThreshold(effectiveThreshold)
                Logger.d("Password attempt evaluated")
                if (evaluation.thresholdReached) {
                    captureUseCase.start(evaluation.newAttemptCount, isTest = false)
                }
            } catch (e: Exception) {
                Logger.e(e, "Password failed handling error")
            }
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        Logger.i("Password succeeded — resetting counter")
        runAsync { runCatching { securityPrefs.resetAttempts() } }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Logger.i("Device Admin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Logger.i("Device Admin disabled")
        runAsync { runCatching { securityPrefs.setProtectionEnabled(false) } }
    }

    private fun runAsync(block: suspend () -> Unit) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                block()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun isAdminActive(context: Context): Boolean {
        val manager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        return manager.isAdminActive(ComponentName(context, MyDeviceAdminReceiver::class.java))
    }
}
