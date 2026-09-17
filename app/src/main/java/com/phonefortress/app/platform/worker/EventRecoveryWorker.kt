package com.phonefortress.app.platform.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.phonefortress.app.data.repository.EventRepository
import com.phonefortress.app.domain.model.EventOperation
import com.phonefortress.app.domain.model.SecurityEventStatus
import com.phonefortress.app.platform.service.CameraForegroundService
import com.phonefortress.app.util.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Reconciles persisted non-terminal events after process death or reboot. */
@HiltWorker
class EventRecoveryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val eventRepository: EventRepository
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = try {
        eventRepository.getActive().forEach { event ->
            when (event.status) {
                SecurityEventStatus.PENDING, SecurityEventStatus.DEFERRED -> {
                    try {
                        CameraForegroundService.start(applicationContext, event.id, event.isTest)
                    } catch (error: Exception) {
                        val claimed = eventRepository.transition(
                            event.id,
                            SecurityEventStatus.IN_PROGRESS,
                            "startup-recovery-start-attempt",
                            EventOperation.CAPTURE
                        )
                        if (claimed != null) {
                            eventRepository.transition(
                                event.id,
                                SecurityEventStatus.FAILED_RETRYABLE,
                                "startup-recovery-start-failed",
                                EventOperation.CAPTURE
                            )
                            WorkScheduler.scheduleCaptureRetry(applicationContext, event.id)
                        }
                        Logger.e(error, "Recovery could not start capture service")
                    }
                }
                SecurityEventStatus.IN_PROGRESS -> {
                    if (isStale(event.lastTransitionAt, System.currentTimeMillis())) {
                        eventRepository.transition(
                            event.id,
                            SecurityEventStatus.FAILED_RETRYABLE,
                            "startup-recovery-stale-capture",
                            EventOperation.CAPTURE
                        )
                        WorkScheduler.scheduleCaptureRetry(applicationContext, event.id)
                    }
                }
                SecurityEventStatus.CAPTURED -> WorkScheduler.dispatchEventNow(applicationContext, event.id)
                SecurityEventStatus.SEND_PENDING -> WorkScheduler.dispatchEventNow(applicationContext, event.id)
                SecurityEventStatus.FAILED_RETRYABLE -> when (event.operation) {
                    EventOperation.CAPTURE -> WorkScheduler.scheduleCaptureRetry(applicationContext, event.id)
                    EventOperation.SEND -> WorkScheduler.dispatchEventNow(applicationContext, event.id)
                }
                SecurityEventStatus.SENT,
                SecurityEventStatus.FAILED_FINAL,
                SecurityEventStatus.CANCELLED -> Unit
            }
        }
        Result.success()
    } catch (error: Exception) {
        Logger.e(error, "Event recovery failed")
        Result.retry()
    }

    companion object {
        private const val UNIQUE_NAME = "event-recovery"
        private const val IN_PROGRESS_TIMEOUT_MS = 2 * 60_000L

        internal fun isStale(lastTransitionAt: Long, now: Long): Boolean =
            lastTransitionAt <= now - IN_PROGRESS_TIMEOUT_MS

        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<EventRecoveryWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
