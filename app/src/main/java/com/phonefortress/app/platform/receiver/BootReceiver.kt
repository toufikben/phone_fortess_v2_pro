package com.phonefortress.app.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.phonefortress.app.data.prefs.SecurityPrefs
import com.phonefortress.app.geofence.ZoneSyncUseCase
import com.phonefortress.app.platform.worker.WorkScheduler
import com.phonefortress.app.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var zoneSyncUseCase: ZoneSyncUseCase
    @Inject lateinit var securityPrefs: SecurityPrefs

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Logger.i("Boot received: ${intent.action}")
                scope.launch {
                    runCatching { zoneSyncUseCase() }
                        .onFailure { Logger.e(it, "Zone sync failed at boot") }
                    runCatching {
                        WorkScheduler.schedulePhotoCleanup(context)
                        WorkScheduler.schedulePeriodicDispatcher(context)
                    }.onFailure { Logger.e(it, "Work scheduling failed at boot") }
                    if (runCatching { securityPrefs.protectionEnabled.first() }.getOrDefault(false)) {
                        runCatching { WorkScheduler.scheduleEventDispatcher(context) }
                            .onFailure { Logger.e(it, "Event dispatcher resume failed at boot") }
                    }
                }
            }
        }
    }
}
