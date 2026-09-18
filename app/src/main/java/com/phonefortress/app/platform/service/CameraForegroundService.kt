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
import com.phonefortress.app.data.prefs.SecurityPrefs
import com.phonefortress.app.data.repository.EventRepository
import com.phonefortress.app.domain.model.EventOperation
import com.phonefortress.app.domain.model.SecurityEventStatus
import com.phonefortress.app.platform.audio.AudioRecorder
import com.phonefortress.app.platform.camera.CameraController
import com.phonefortress.app.platform.location.LocationProvider
import com.phonefortress.app.platform.worker.WorkScheduler
import com.phonefortress.app.util.Constants
import com.phonefortress.app.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class CameraForegroundService : Service(), LifecycleOwner {
    @Inject lateinit var cameraController: CameraController
    @Inject lateinit var audioRecorder: AudioRecorder
    @Inject lateinit var locationProvider: LocationProvider
    @Inject lateinit var eventRepository: EventRepository
    @Inject lateinit var securityPrefs: SecurityPrefs

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val captureMutex = Mutex()
    private val activeEvents = ConcurrentHashMap.newKeySet<String>()
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_IS_TEST = "is_test"
        fun start(context: Context, eventId: String, isTest: Boolean = false) {
            val intent = Intent(context, CameraForegroundService::class.java).apply {
                putExtra(EXTRA_EVENT_ID, eventId)
                putExtra(EXTRA_IS_TEST, isTest)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val eventId = intent?.getStringExtra(EXTRA_EVENT_ID)
        if (eventId.isNullOrBlank()) return START_NOT_STICKY
        if (!activeEvents.add(eventId)) return START_NOT_STICKY
        acquireWakeLock()
        scope.launch {
            try {
                val photo = securityPrefs.capturePhoto.first()
                val audio = securityPrefs.captureAudio.first()
                val location = securityPrefs.captureLocation.first()
                startForegroundWithNotification(photo, audio, location)
                captureMutex.withLock { processEvent(eventId) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e, "Event processing failed")
                runCatching { markCaptureRetryable(eventId, reason = "capture-exception") }
            } finally {
                activeEvents.remove(eventId)
                if (activeEvents.isEmpty()) {
                    releaseWakeLock()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelfResult(startId)
                }
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun processEvent(eventId: String) {
        var event = eventRepository.getById(eventId) ?: return
        if (event.status == SecurityEventStatus.SENT || event.status == SecurityEventStatus.FAILED_FINAL || event.status == SecurityEventStatus.CANCELLED) return
        if (event.status == SecurityEventStatus.IN_PROGRESS) {
            Logger.i("Event already claimed; ignoring duplicate start")
            return
        }
        if (event.status == SecurityEventStatus.FAILED_RETRYABLE && event.operation != EventOperation.CAPTURE) return
        val attemptId = eventRepository.claimCapture(eventId) ?: return
        event = eventRepository.getById(eventId) ?: return
        val evidenceDir = File(filesDir, Constants.DIR_EVIDENCE).apply { mkdirs() }
        val photosDir = File(evidenceDir, "photos").apply { mkdirs() }
        val audioDir = File(evidenceDir, "audio").apply { mkdirs() }
        val photoEnabled = securityPrefs.capturePhoto.first()
        val photo = if (photoEnabled) {
            event.photoPath?.let { File(it).takeIf(File::exists) } ?: withTimeoutOrNull(Constants.CAMERA_TIMEOUT_MS) {
                cameraController.captureFrontPhoto(this@CameraForegroundService, photosDir)
            }
        } else null
        if (photoEnabled && photo == null) {
            eventRepository.updateMetadata(event)
            markCaptureRetryable(eventId, attemptId, "photo-capture-failed")
            return
        }
        photo?.let { event = event.copy(photoPath = it.absolutePath) }
        if (event.photoPath != null) eventRepository.updateMetadata(event)

        val audioEnabled = securityPrefs.captureAudio.first()
        val audio = if (audioEnabled) {
            event.audioPath?.let { File(it).takeIf(File::exists) } ?: withTimeoutOrNull(Constants.AUDIO_DURATION_MS + 5_000L) {
                audioRecorder.recordShort(audioDir)
            }
        } else null
        if (audioEnabled && audio == null) {
            eventRepository.updateMetadata(event)
            markCaptureRetryable(eventId, attemptId, "audio-capture-failed")
            return
        }
        audio?.let { event = event.copy(audioPath = it.absolutePath) }
        if (event.audioPath != null) eventRepository.updateMetadata(event)
        if (securityPrefs.captureLocation.first()) withTimeoutOrNull(Constants.LOCATION_TIMEOUT_MS + 2_000L) { locationProvider.getCurrentLocation() }?.let { event = event.copy(latitude = it.latitude, longitude = it.longitude, locationAccuracy = it.accuracy) }
        eventRepository.updateMetadata(event)
        if (!eventRepository.transitionCaptureOwned(eventId, attemptId, SecurityEventStatus.CAPTURED, "capture-complete")) return
        event = eventRepository.getById(eventId) ?: return
        eventRepository.updateMetadata(event)
        event = eventRepository.transition(eventId, SecurityEventStatus.SEND_PENDING, "dispatch-ready", EventOperation.SEND) ?: return
        WorkScheduler.dispatchEventNow(applicationContext, eventId)
    }

    private suspend fun markCaptureRetryable(eventId: String, attemptId: String? = null, reason: String) {
        val current = eventRepository.getById(eventId) ?: return
        if (attemptId != null) {
            if (current.captureAttemptId != attemptId) return
            if (eventRepository.transitionCaptureOwned(eventId, attemptId, SecurityEventStatus.FAILED_RETRYABLE, reason)) {
                WorkScheduler.scheduleCaptureRetry(applicationContext, eventId)
            }
            return
        }
        val inProgress = when (current.status) {
            SecurityEventStatus.PENDING,
            SecurityEventStatus.DEFERRED -> eventRepository.transition(
                eventId, SecurityEventStatus.IN_PROGRESS, "capture-failure-claim", EventOperation.CAPTURE
            )
            SecurityEventStatus.IN_PROGRESS -> current
            else -> null
        }
        if (inProgress != null) {
            val failed = eventRepository.transition(
                eventId, SecurityEventStatus.FAILED_RETRYABLE, reason, EventOperation.CAPTURE
            )
            if (failed != null) WorkScheduler.scheduleCaptureRetry(applicationContext, eventId)
        }
    }

    private fun startForegroundWithNotification(photo: Boolean, audio: Boolean, location: Boolean) {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var type = 0
            if (photo) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            if (audio) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            if (location) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            ServiceCompat.startForeground(this, Constants.NOTIF_ID_CAPTURE, notification, type)
        } else startForeground(Constants.NOTIF_ID_CAPTURE, notification)
    }

    private fun buildNotification(): Notification = NotificationCompat.Builder(this, Constants.CHANNEL_PROTECTION)
        .setSmallIcon(R.drawable.ic_shield).setContentTitle(getString(R.string.capturing_evidence_title))
        .setContentText(getString(R.string.capturing_evidence_body)).setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true).setSilent(true).build()

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (wakeLock?.isHeld == true) return
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PhoneFortress::CaptureWakeLock").apply { setReferenceCounted(false); acquire(60_000L) }
    }
    private fun releaseWakeLock() { runCatching { wakeLock?.takeIf { it.isHeld }?.release() }; wakeLock = null }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        scope.cancel()
        cameraController.release()
        audioRecorder.release()
        activeEvents.clear()
        releaseWakeLock()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }
}
