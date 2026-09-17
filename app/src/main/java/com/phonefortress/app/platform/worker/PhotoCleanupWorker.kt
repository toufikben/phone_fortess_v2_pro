package com.phonefortress.app.platform.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phonefortress.app.data.prefs.SecurityPrefs
import com.phonefortress.app.data.repository.EventRepository
import com.phonefortress.app.util.Constants
import com.phonefortress.app.util.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.io.File

@HiltWorker
class PhotoCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val eventRepository: EventRepository,
    private val securityPrefs: SecurityPrefs
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val retentionDays = securityPrefs.retentionDays.first()
        val cutoff = System.currentTimeMillis() - retentionDays * 24L * 60 * 60 * 1000
        val terminalEvents = eventRepository.getTerminalBefore(cutoff)
        var deletedFiles = 0
        terminalEvents.forEach { event ->
            listOfNotNull(event.photoPath, event.audioPath).forEach { path ->
                val file = File(path)
                if (!file.exists() || file.delete()) deletedFiles++
            }
        }
        // Paths are cleared only after each event is terminal and its files are gone.
        eventRepository.clearEvidencePathsBefore(cutoff)
        eventRepository.deleteOlderThan(cutoff)
        Logger.i("PhotoCleanupWorker: removed $deletedFiles evidence files and ${terminalEvents.size} terminal events")
        Result.success()
    } catch (e: Exception) {
        Logger.e(e, "PhotoCleanupWorker failed")
        Result.retry()
    }
}
