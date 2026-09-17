package com.phonefortress.app.platform.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phonefortress.app.alerts.AlertDispatcher
import com.phonefortress.app.data.repository.EventRepository
import com.phonefortress.app.domain.model.AlertResult
import com.phonefortress.app.domain.model.EventOperation
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.domain.model.SecurityEventStatus
import com.phonefortress.app.util.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class EventDispatcherWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val eventRepository: EventRepository,
    private val alertDispatcher: AlertDispatcher
) : CoroutineWorker(context, params) {
    companion object { const val KEY_EVENT_ID = "specific_event_id" }

    override suspend fun doWork(): Result {
        return try {
            val ids = inputData.getString(KEY_EVENT_ID)?.let { listOf(it) }
            val events = ids?.mapNotNull { eventRepository.getById(it) } ?: eventRepository.getDispatchable()
            var retryable = false
            events.forEach { event ->
                when (dispatchSingle(event)) {
                    DispatchOutcome.RETRY -> retryable = true
                    DispatchOutcome.FAILED -> Logger.w("Event ${event.id} reached final failure")
                    DispatchOutcome.DONE, DispatchOutcome.SKIPPED -> Unit
                }
            }
            if (retryable) Result.retry() else Result.success()
        } catch (e: Exception) {
            Logger.e(e, "EventDispatcherWorker infrastructure failure")
            Result.retry()
        }
    }

    private suspend fun dispatchSingle(initial: SecurityEvent): DispatchOutcome {
        val event = when {
            initial.status == SecurityEventStatus.CAPTURED ->
                eventRepository.transition(initial.id, SecurityEventStatus.SEND_PENDING, "capture-complete", EventOperation.SEND)
            initial.status == SecurityEventStatus.SEND_PENDING -> initial
            initial.status == SecurityEventStatus.FAILED_RETRYABLE && initial.operation == EventOperation.SEND ->
                eventRepository.transition(initial.id, SecurityEventStatus.SEND_PENDING, "send-retry", EventOperation.SEND)
            else -> return DispatchOutcome.SKIPPED
        } ?: return DispatchOutcome.SKIPPED

        val results = alertDispatcher.dispatch(event)
        return when {
            results.any { it is AlertResult.Success } -> {
                eventRepository.transition(event.id, SecurityEventStatus.SENT, "dispatch-success", EventOperation.SEND)
                DispatchOutcome.DONE
            }
            results.any { it is AlertResult.Retryable } -> {
                eventRepository.transition(event.id, SecurityEventStatus.FAILED_RETRYABLE, "dispatch-retryable-failure", EventOperation.SEND)
                DispatchOutcome.RETRY
            }
            else -> {
                eventRepository.transition(event.id, SecurityEventStatus.FAILED_FINAL, "dispatch-final-failure", EventOperation.SEND)
                DispatchOutcome.FAILED
            }
        }
    }

    private enum class DispatchOutcome { DONE, RETRY, FAILED, SKIPPED }
}
