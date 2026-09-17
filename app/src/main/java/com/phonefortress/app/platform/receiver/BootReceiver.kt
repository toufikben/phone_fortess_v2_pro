package com.phonefortress.app.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.phonefortress.app.geofence.ZoneSyncUseCase
import com.phonefortress.app.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var zoneSyncUseCase: ZoneSyncUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Logger.i("Boot received: ${intent.action}")
                scope.launch {
                    runCatching { zoneSyncUseCase() }
                }
            }
        }
    }
}
