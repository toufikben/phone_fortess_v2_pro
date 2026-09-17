package com.phonefortress.app.platform.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.phonefortress.app.util.Constants
import com.phonefortress.app.util.Logger
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private fun enqueueCaptureRetry(context: Context, eventId: String, attempt: Int) {
        val data = Data.Builder().putString(CaptureRetryWorker.KEY_EVENT_ID, eventId)
            .putInt(CaptureRetryWorker.KEY_ATTEMPT, attempt).build()
        val request = OneTimeWorkRequestBuilder<CaptureRetryWorker>()
            .setInputData(data)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .setInitialDelay(30, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("capture_retry")
            .addTag("capture_retry_$eventId").build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "${Constants.WORK_CAPTURE_RETRY}_$eventId", ExistingWorkPolicy.KEEP, request
        )
    }

    fun scheduleCaptureRetry(context: Context, eventId: String, attempt: Int) {
        if (attempt < CaptureRetryWorker.MAX_ATTEMPTS) enqueueCaptureRetry(context, eventId, attempt)
    }

    fun schedulePhotoCleanup(context: Context) {
        val request = PeriodicWorkRequestBuilder<PhotoCleanupWorker>(24, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).setRequiresStorageNotLow(true).build())
            .setInitialDelay(2, TimeUnit.HOURS)
            .addTag("photo_cleanup").addTag(Constants.WORK_PHOTO_CLEANUP).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            Constants.WORK_PHOTO_CLEANUP, ExistingPeriodicWorkPolicy.KEEP, request
        )
    }

    fun dispatchEventNow(context: Context, eventId: String) {
        val data = Data.Builder().putString(EventDispatcherWorker.KEY_EVENT_ID, eventId).build()
        val request = OneTimeWorkRequestBuilder<EventDispatcherWorker>()
            .setInputData(data)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .addTag("event_dispatch").addTag("dispatch_$eventId").build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "${Constants.WORK_EVENT_DISPATCH}_$eventId", ExistingWorkPolicy.KEEP, request
        )
    }

    fun scheduleEventDispatcher(context: Context) {
        val request = OneTimeWorkRequestBuilder<EventDispatcherWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("event_dispatch").addTag(Constants.WORK_EVENT_DISPATCH).build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            Constants.WORK_EVENT_DISPATCH, ExistingWorkPolicy.KEEP, request
        )
    }

    fun schedulePeriodicDispatcher(context: Context) {
        val request = PeriodicWorkRequestBuilder<EventDispatcherWorker>(6, TimeUnit.HOURS)
            .addTag("event_dispatch").addTag("periodic_dispatcher").build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "periodic_dispatcher", ExistingPeriodicWorkPolicy.KEEP, request
        )
    }

    fun cancelForEvent(context: Context, eventId: String) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork("${Constants.WORK_CAPTURE_RETRY}_$eventId")
            cancelUniqueWork("${Constants.WORK_EVENT_DISPATCH}_$eventId")
            cancelAllWorkByTag("capture_retry_$eventId")
            cancelAllWorkByTag("dispatch_$eventId")
        }
        Logger.i("Cancelled work for $eventId")
    }
}
