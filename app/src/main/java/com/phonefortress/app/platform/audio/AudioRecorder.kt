package com.phonefortress.app.platform.audio

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import com.phonefortress.app.util.Constants
import com.phonefortress.app.util.Logger
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مسجل صوتي قصير — 10 ثوان كحد أقصى.
 * يستخدم MediaRecorder مع ترميز AAC.
 */
@Singleton
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var recorder: MediaRecorder? = null

    /**
     * يسجّل مقطعاً صوتياً لمدة محددة.
     * @return مسار الملف أو null.
     */
    suspend fun recordShort(outputDir: File, durationMs: Long = Constants.AUDIO_DURATION_MS): File? =
        withContext(Dispatchers.IO) {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    Logger.w("Microphone permission not granted")
                    return@withContext null
                }
                if (!outputDir.exists()) outputDir.mkdirs()

                val file = File(
                    outputDir,
                    "evidence_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.m4a"
                )

                val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }

                rec.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioSamplingRate(44100)
                    setAudioEncodingBitRate(96000)
                    setOutputFile(file.absolutePath)
                    prepare()
                    start()
                }
                recorder = rec

                delay(durationMs)

                stopAndRelease()
                Logger.i("Audio saved: ${file.name}")
                file
            } catch (e: Exception) {
                Logger.e(e, "Audio recording failed")
                stopAndRelease()
                null
            }
        }

    private fun stopAndRelease() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
        } catch (_: Exception) {
            Logger.w("Audio recorder stop warning")
            recorder?.runCatching { release() }
            recorder = null
        }
    }
}
