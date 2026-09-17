package com.phonefortress.app.platform.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phonefortress.app.alerts.AlertDispatcher
import com.phonefortress.app.data.repository.EventRepository
import com.phonefortress.app.domain.model.AlertResult
import com.phonefortress.app.domain.model.SecurityEventStatus
import com.phonefortress.app.domain.state.SecurityEventStateMachine
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

    override suspend fun doWork(): Result = try {
        inputData.getString(KEY_EVENT_ID)?.let { dispatchSingle(it) } ?: dispatchAllPending()
        Result.success()
    } catch (e: Exception) {
        Logger.e(e, "EventDispatcherWorker failed")
        if (runAttemptCount < 5) Result.retry() else Result.failure()
    }

    private suspend fun dispatchSingle(eventId: String): Boolean {
        val event = eventRepository.getById(eventId) ?: return false
        if (SecurityEventStateMachine.isTerminal(event.status)) return true
        val results = alertDispatcher.dispatch(event)
        val finalStatus = computeFinalStatus(results)
        eventRepository.updateStatus(eventId, finalStatus)
        return finalStatus == SecurityEventStatus.SENT
    }

    private suspend fun dispatchAllPending() {
        eventRepository.getActive().forEach { event ->
            runCatching { dispatchSingle(event.id) }
                .onFailure { Logger.e(it, "Failed to dispatch ${event.id}") }
        }
    }

    private fun computeFinalStatus(results: List<AlertResult>): SecurityEventStatus = when {
        results.any { it is AlertResult.Success } -> SecurityEventStatus.SENT
        results.any { it is AlertResult.Retryable } -> SecurityEventStatus.FAILED_RETRYABLE
        else -> SecurityEventStatus.FAILED_FINAL
    }
}
