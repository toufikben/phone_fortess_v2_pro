package com.phonefortress.app.platform.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.phonefortress.app.R
import com.phonefortress.app.alerts.AlertDispatcher
import com.phonefortress.app.data.prefs.SecurityPrefs
import com.phonefortress.app.data.repository.EventRepository
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.domain.model.SecurityEventStatus
import com.phonefortress.app.domain.state.SecurityEventStateMachine
import com.phonefortress.app.domain.usecase.EvaluateThreatUseCase
import com.phonefortress.app.geofence.ZoneStateHolder
import com.phonefortress.app.platform.audio.AudioRecorder
import com.phonefortress.app.platform.camera.CameraController
import com.phonefortress.app.platform.location.LocationProvider
import com.phonefortress.app.util.Constants
import com.phonefortress.app.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import javax.inject.Inject

/**
 * خدمة أمامية للتقاط الأدلة.
 * - تلتقط صورة.
 * - تسجل صوتاً (اختياري).
 * - تحدد الموقع.
 * - ترسل التنبيه.
 * - تُحدّث حالة الحدث في Room.
 */
@AndroidEntryPoint
class CameraForegroundService : Service(), LifecycleOwner {

    @Inject lateinit var cameraController: CameraController
    @Inject lateinit var audioRecorder: AudioRecorder
    @Inject lateinit var locationProvider: LocationProvider
    @Inject lateinit var eventRepository: EventRepository
    @Inject lateinit var alertDispatcher: AlertDispatcher
    @Inject lateinit var securityPrefs: SecurityPrefs
    @Inject lateinit var evaluateThreatUseCase: EvaluateThreatUseCase
    @Inject lateinit var zoneState: ZoneStateHolder

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null
    private var isProcessing = false

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_IS_TEST = "is_test"

        fun start(context: Context, eventId: String, isTest: Boolean = false) {
            val intent = Intent(context, CameraForegroundService::class.java).apply {
                putExtra(EXTRA_EVENT_ID, eventId)
                putExtra(EXTRA_IS_TEST, isTest)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val eventId = intent?.getStringExtra(EXTRA_EVENT_ID)
        val isTest = intent?.getBooleanExtra(EXTRA_IS_TEST, false) ?: false

        if (eventId.isNullOrBlank()) {
            Logger.w("Service started without eventId")
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundWithNotification()

        if (isProcessing) {
            Logger.w("Already processing — ignoring $eventId")
            return START_NOT_STICKY
        }
        isProcessing = true

        acquireWakeLock()

        scope.launch {
            try {
                processEvent(eventId, isTest)
            } catch (e: Exception) {
                Logger.e(e, "Event processing failed")
            } finally {
                releaseWakeLock()
                isProcessing = false
                lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun processEvent(eventId: String, isTest: Boolean) {
        val existing = eventRepository.getById(eventId) ?: run {
            Logger.w("Event not found: $eventId")
            return
        }
        if (SecurityEventStateMachine.isTerminal(existing.status)) {
            Logger.i("Event already terminal: $eventId")
            return
        }

        // 1. IN_PROGRESS
        var event = SecurityEventStateMachine.transition(existing, SecurityEventStatus.IN_PROGRESS)
        eventRepository.save(event)

        // 2. مجلد الأدلة
        val evidenceDir = File(filesDir, Constants.DIR_EVIDENCE).apply { mkdirs() }
        val photosDir = File(evidenceDir, "photos").apply { mkdirs() }
        val audioDir = File(evidenceDir, "audio").apply { mkdirs() }

        // 3. صورة
        val capturePhoto = securityPrefs.capturePhoto.first()
        if (capturePhoto) {
            val photo = withTimeoutOrNull(Constants.CAMERA_TIMEOUT_MS) {
                cameraController.captureFrontPhoto(this@CameraForegroundService, photosDir)
            }
            if (photo != null) {
                event = event.copy(photoPath = photo.absolutePath)
            }
        }

        // 4. صوت
        val captureAudio = securityPrefs.captureAudio.first()
        if (captureAudio) {
            val audio = withTimeoutOrNull(Constants.AUDIO_DURATION_MS + 5_000L) {
                audioRecorder.recordShort(audioDir)
            }
            if (audio != null) {
                event = event.copy(audioPath = audio.absolutePath)
            }
        }

        // 5. موقع
        val captureLocation = securityPrefs.captureLocation.first()
        if (captureLocation) {
            val loc = withTimeoutOrNull(Constants.LOCATION_TIMEOUT_MS + 2_000L) {
                locationProvider.getCurrentLocation()
            }
            if (loc != null) {
                event = event.copy(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    locationAccuracy = loc.accuracy
                )
            }
        }

        // 6. CAPTURED
        event = SecurityEventStateMachine.transition(event, SecurityEventStatus.CAPTURED)
        eventRepository.save(event)

        // تقييم التهديد
        val isInSafeZone = zoneState.isInSafeZone.value
        val assessment = evaluateThreatUseCase(
            photoPath = event.photoPath,
            attempts = event.failedAttempts,
            isInSafeZone = isInSafeZone,
            isTest = event.isTest
        )
        event = event.copy(
            threatScore = assessment.score,
            threatLevel = assessment.level,
            threatReasons = assessment.reasons
        )
        eventRepository.save(event)
        Logger.i("Event $eventId threat=${assessment.score}/100 (${assessment.level})")

        // 7. SEND_PENDING
        event = SecurityEventStateMachine.transition(event, SecurityEventStatus.SEND_PENDING)
        eventRepository.save(event)

        // 8. إرسال
        cameraController.release()
        val results = alertDispatcher.dispatch(event)

        // 9. تحديد الحالة النهائية
        val anySuccess = results.any { it is com.phonefortress.app.domain.model.AlertResult.Success }
        val anyRetryable = results.any { it is com.phonefortress.app.domain.model.AlertResult.Retryable }

        val finalStatus = when {
            anySuccess -> SecurityEventStatus.SENT
            anyRetryable -> SecurityEventStatus.FAILED_RETRYABLE
            else -> SecurityEventStatus.FAILED_FINAL
        }

        event = SecurityEventStateMachine.transition(event, finalStatus)
        eventRepository.save(event)
        Logger.i("Event $eventId finished with status $finalStatus")
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            ServiceCompat.startForeground(this, Constants.NOTIF_ID_CAPTURE, notification, type)
        } else {
            startForeground(Constants.NOTIF_ID_CAPTURE, notification)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, Constants.CHANNEL_PROTECTION)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(getString(R.string.capturing_evidence_title))
            .setContentText(getString(R.string.capturing_evidence_body))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PhoneFortress::CaptureWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(60_000L)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.takeIf { it.isHeld }?.release()
        } catch (e: Exception) {
            Logger.w("WakeLock release: ${e.message}")
        }
        wakeLock = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        cameraController.release()
        releaseWakeLock()
    }
}
