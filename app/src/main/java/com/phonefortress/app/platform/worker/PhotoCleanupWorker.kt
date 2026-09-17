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
        eventRepository.clearEvidencePathsBefore(cutoff)
        eventRepository.deleteOlderThan(cutoff)
        val evidenceDir = File(applicationContext.filesDir, Constants.DIR_EVIDENCE)
        val photos = cleanupDir(File(evidenceDir, "photos"), cutoff)
        val audio = cleanupDir(File(evidenceDir, "audio"), cutoff)
        Logger.i("PhotoCleanupWorker: deleted $photos photos and $audio audio files")
        Result.success()
    } catch (e: Exception) {
        Logger.e(e, "PhotoCleanupWorker failed")
        Result.retry()
    }

    private fun cleanupDir(dir: File, cutoff: Long): Int {
        if (!dir.exists()) return 0
        return dir.listFiles()?.count { it.isFile && it.lastModified() < cutoff && it.delete() } ?: 0
    }
}
