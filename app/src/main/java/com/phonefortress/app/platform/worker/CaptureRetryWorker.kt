package com.phonefortress.app.platform.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phonefortress.app.data.repository.EventRepository
import com.phonefortress.app.domain.model.EventOperation
import com.phonefortress.app.domain.model.SecurityEventStatus
import com.phonefortress.app.platform.service.CameraForegroundService
import com.phonefortress.app.util.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CaptureRetryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val eventRepository: EventRepository
) : CoroutineWorker(context, params) {
    companion object {
        const val KEY_EVENT_ID = "event_id"
        const val KEY_ATTEMPT = "attempt_number"
        const val MAX_ATTEMPTS = 3
    }

    override suspend fun doWork(): Result {
        val eventId = inputData.getString(KEY_EVENT_ID)
            ?: return Result.failure().also { Logger.w("CaptureRetryWorker: missing eventId") }
        val attempt = inputData.getInt(KEY_ATTEMPT, 0)
        val event = eventRepository.getById(eventId) ?: return Result.failure()
        if (event.status == SecurityEventStatus.SENT || event.status == SecurityEventStatus.FAILED_FINAL || event.status == SecurityEventStatus.CANCELLED) return Result.success()
        if (attempt >= MAX_ATTEMPTS) {
            eventRepository.transition(eventId, SecurityEventStatus.FAILED_FINAL, "retry-budget-exhausted", event.operation)
            return Result.failure()
        }
        return try {
            when (event.operation) {
                EventOperation.CAPTURE -> {
                    eventRepository.transition(eventId, SecurityEventStatus.IN_PROGRESS, "capture-retry-$attempt", EventOperation.CAPTURE)
                    CameraForegroundService.start(applicationContext, eventId, event.isTest)
                }
                EventOperation.SEND -> WorkScheduler.dispatchEventNow(applicationContext, eventId)
            }
            Result.success()
        } catch (e: Exception) {
            Logger.e(e, "CaptureRetryWorker failed")
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }
}
