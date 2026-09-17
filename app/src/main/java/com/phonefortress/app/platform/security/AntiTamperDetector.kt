package com.phonefortress.app.platform.security

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.phonefortress.app.platform.admin.MyDeviceAdminReceiver
import com.phonefortress.app.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AntiTamperDetector @Inject constructor(@ApplicationContext private val context: Context) {
    data class SecurityReport(
        val isRooted: Boolean,
        val isDebuggerAttached: Boolean,
        val isDeviceAdminActive: Boolean,
        val isEmulator: Boolean,
        val isDeveloperMode: Boolean,
        val riskLevel: RiskLevel
    )
    enum class RiskLevel { SAFE, LOW, MEDIUM, HIGH }

    fun analyze(): SecurityReport {
        val rooted = checkRoot()
        val debugger = android.os.Debug.isDebuggerConnected() || android.os.Debug.waitingForDebugger()
        val admin = checkDeviceAdmin()
        val emulator = checkEmulator()
        val developer = checkDeveloperMode()
        val score = listOf(rooted to 40, debugger to 50, emulator to 30, developer to 10).filter { it.first }.sumOf { it.second }
        val level = when { score >= 60 -> RiskLevel.HIGH; score >= 30 -> RiskLevel.MEDIUM; score > 0 -> RiskLevel.LOW; else -> RiskLevel.SAFE }
        Logger.i("Anti-tamper risk: $level")
        return SecurityReport(rooted, debugger, admin, emulator, developer, level)
    }

    private fun checkRoot(): Boolean {
        val paths = listOf("/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/su/bin/su", "/magisk/.core/bin/su")
        if (paths.any { java.io.File(it).exists() }) return true
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            try {
                process.inputStream.bufferedReader().readText().isNotEmpty()
            } finally {
                process.destroy()
            }
        } catch (_: Exception) { false }
    }

    private fun checkDeviceAdmin(): Boolean = try {
        val manager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        manager.isAdminActive(ComponentName(context, MyDeviceAdminReceiver::class.java))
    } catch (_: Exception) { false }

    private fun checkEmulator(): Boolean = Build.FINGERPRINT.startsWith("generic") || Build.FINGERPRINT.contains("emulator") || Build.MODEL.contains("Emulator") || Build.MODEL.contains("Android SDK built for") || Build.MANUFACTURER.contains("Genymotion") || Build.HARDWARE.contains("goldfish") || Build.HARDWARE.contains("ranchu")

    private fun checkDeveloperMode(): Boolean = try {
        android.provider.Settings.Global.getInt(context.contentResolver, android.provider.Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
    } catch (_: Exception) { false }
}
