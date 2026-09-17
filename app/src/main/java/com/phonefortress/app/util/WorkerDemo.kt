package com.phonefortress.app.util

import android.content.Context
import com.phonefortress.app.platform.worker.WorkScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** مساعد اختبار العمال، يُستدعى يدوياً من وضع Debug. */
@Singleton
class WorkerDemo @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun triggerAll() {
        WorkScheduler.schedulePhotoCleanup(context)
        WorkScheduler.schedulePeriodicDispatcher(context)
        WorkScheduler.scheduleEventDispatcher(context)
    }

    fun triggerRetryFor(eventId: String) {
        WorkScheduler.scheduleCaptureRetry(context, eventId)
    }
}
