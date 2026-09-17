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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
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
        val evidenceRoot = File(applicationContext.filesDir, Constants.DIR_EVIDENCE).canonicalFile
        var deletedFiles = 0
        var deletedEvents = 0
        while (currentCoroutineContext().isActive) {
            val terminalEvents = eventRepository.getTerminalBefore(cutoff, Constants.MAX_EVENT_BATCH_SIZE)
            if (terminalEvents.isEmpty()) break
            val deletedBeforeBatch = deletedEvents
            terminalEvents.forEach { event ->
                val filesDeleted = listOfNotNull(event.photoPath, event.audioPath).all { path ->
                    val file = File(path).canonicalFile
                    if (!file.path.startsWith("${evidenceRoot.path}${File.separator}")) return@all false
                    if (!file.exists()) true else file.delete().also { if (it) deletedFiles++ }
                }
                if (filesDeleted) {
                    eventRepository.clearEvidencePaths(event.id)
                    eventRepository.delete(event.id)
                    deletedEvents++
                }
            }
            if (deletedEvents == deletedBeforeBatch) break
            if (terminalEvents.size < Constants.MAX_EVENT_BATCH_SIZE) break
        }
        Logger.i("PhotoCleanupWorker: removed $deletedFiles evidence files and $deletedEvents terminal events")
        Result.success()
    } catch (e: Exception) {
        Logger.e(e, "PhotoCleanupWorker failed")
        Result.retry()
    }
}
