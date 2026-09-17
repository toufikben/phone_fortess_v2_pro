package com.phonefortress.app.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.phonefortress.app.data.prefs.SecurityPrefs
import com.phonefortress.app.geofence.ZoneSyncUseCase
import com.phonefortress.app.geofence.ZoneStateHolder
import com.phonefortress.app.platform.worker.WorkScheduler
import com.phonefortress.app.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var zoneSyncUseCase: ZoneSyncUseCase
    @Inject lateinit var securityPrefs: SecurityPrefs
    @Inject lateinit var zoneStateHolder: ZoneStateHolder

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED
            )) return

        Logger.i("Boot received")
        WorkScheduler.scheduleRecovery(context)
        WorkScheduler.schedulePhotoCleanup(context)
        WorkScheduler.schedulePeriodicDispatcher(context)
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                withTimeoutOrNull(8_000L) {
                    runCatching { zoneSyncUseCase() }
                        .onFailure { Logger.e(it, "Zone sync failed at boot") }
                    runCatching { zoneStateHolder.restore() }
                        .onFailure { Logger.e(it, "Zone state restore failed at boot") }
                    if (runCatching { securityPrefs.protectionEnabled.first() }.getOrDefault(false)) {
                        runCatching { WorkScheduler.scheduleEventDispatcher(context) }
                            .onFailure { Logger.e(it, "Event dispatcher resume failed at boot") }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
