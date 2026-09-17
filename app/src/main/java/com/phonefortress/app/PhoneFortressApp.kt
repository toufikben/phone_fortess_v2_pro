package com.phonefortress.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.phonefortress.app.platform.worker.WorkScheduler
import com.phonefortress.app.util.Constants
import com.phonefortress.app.util.Logger
import com.phonefortress.app.geofence.ZoneStateHolder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * نقطة تشغيل التطبيق الرئيسية.
 * - تهيئة Hilt
 * - تهيئة WorkManager
 * - إنشاء قنوات الإشعارات
 * - تهيئة Logger
 */
@HiltAndroidApp
class PhoneFortressApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var zoneStateHolder: ZoneStateHolder
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Logger.init(isDebug = BuildConfig.DEBUG)
        createNotificationChannels()
        scheduleInitialWork()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java)

        val channels = listOf(
            NotificationChannel(
                Constants.CHANNEL_PROTECTION,
                getString(R.string.channel_protection_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_protection_desc)
                setShowBadge(false)
            },
            NotificationChannel(
                Constants.CHANNEL_ALERTS,
                getString(R.string.channel_alerts_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_alerts_desc)
                enableVibration(true)
            },
            NotificationChannel(
                Constants.CHANNEL_SILENT,
                getString(R.string.channel_silent_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.channel_silent_desc)
                setShowBadge(false)
            }
        )

        manager.createNotificationChannels(channels)
    }

    private fun scheduleInitialWork() {
        appScope.launch {
            zoneStateHolder.restore()
            WorkScheduler.scheduleRecovery(this@PhoneFortressApp)
            WorkScheduler.schedulePhotoCleanup(this@PhoneFortressApp)
            WorkScheduler.schedulePeriodicDispatcher(this@PhoneFortressApp)
            WorkScheduler.scheduleEventDispatcher(this@PhoneFortressApp)
        }
    }
}
