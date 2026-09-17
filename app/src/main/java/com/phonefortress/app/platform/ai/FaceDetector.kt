package com.phonefortress.app.platform.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mediapipe.tasks.components.containers.Detection
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector as MpFaceDetector
import com.phonefortress.app.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * كاشف الوجوه — غلاف حول MediaPipe.
 * يعمل محلياً 100% بدون إنترنت.
 */
@Singleton
class FaceDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class FaceResult(
        val count: Int,
        val confidence: Float,
        val boundingBoxes: List<Pair<Float, Float>> // (w, h) نسبي
    )

    private var detector: MpFaceDetector? = null

    init {
        initialize()
    }

    private fun initialize() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("blaze_face_short_range.tflite")
                .build()

            val options = MpFaceDetector.FaceDetectorOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinDetectionConfidence(0.5f)
                .setRunningMode(RunningMode.IMAGE)
                .build()

            detector = MpFaceDetector.createFromOptions(context, options)
            Logger.i("FaceDetector initialized")
        } catch (e: Exception) {
            Logger.e(e, "FaceDetector init failed — will use fallback")
            detector = null
        }
    }

    /**
     * يحلّل صورة ويعيد نتائج اكتشاف الوجوه.
     */
    suspend fun detect(imagePath: String): FaceResult? = withContext(Dispatchers.Default) {
        try {
            val file = File(imagePath)
            if (!file.exists()) return@withContext null

            val bitmap = BitmapFactory.decodeFile(imagePath)
                ?: return@withContext null

            try {
                detectBitmap(bitmap)
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        } catch (e: Exception) {
            Logger.e(e, "Face detection failed")
            null
        }
    }

    private fun detectBitmap(bitmap: Bitmap): FaceResult? {
        val det = detector ?: return fallbackDetect(bitmap)

        return try {
            val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(bitmap).build()
            val mpResult = det.detect(mpImage)
            val detections: List<Detection> = mpResult.detections()

            val boxes = detections.map { d ->
                val bb = d.boundingBox()
                bb.width().toFloat() / bitmap.width to bb.height().toFloat() / bitmap.height
            }

            val maxConfidence = detections.maxOfOrNull { it.categories().firstOrNull()?.score() ?: 0f } ?: 0f

            FaceResult(
                count = detections.size,
                confidence = maxConfidence,
                boundingBoxes = boxes
            )
        } catch (e: Exception) {
            Logger.e(e, "MediaPipe detect failed — fallback")
            fallbackDetect(bitmap)
        }
    }

    /**
     * كشف احتياطي بسيط جداً (لون البشرة) عند فشل MediaPipe.
     */
    private fun fallbackDetect(bitmap: Bitmap): FaceResult {
        return try {
            val sample = sampleSkinTone(bitmap)
            val estimated = if (sample > 0.15f) 1 else 0
            FaceResult(count = estimated, confidence = sample, boundingBoxes = emptyList())
        } catch (e: Exception) {
            FaceResult(0, 0f, emptyList())
        }
    }

    private fun sampleSkinTone(bitmap: Bitmap): Float {
        val w = bitmap.width
        val h = bitmap.height
        val stepX = (w / 40).coerceAtLeast(1)
        val stepY = (h / 40).coerceAtLeast(1)
        var skinPixels = 0
        var totalPixels = 0

        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                if (isSkinTone(r, g, b)) skinPixels++
                totalPixels++
                x += stepX
            }
            y += stepY
        }
        return if (totalPixels > 0) skinPixels.toFloat() / totalPixels else 0f
    }

    private fun isSkinTone(r: Int, g: Int, b: Int): Boolean =
        r > 95 && g > 40 && b > 20 &&
        r > g && r > b &&
        (r - g) > 15 &&
        kotlin.math.abs(r - g) > 15

    /**
     * يحسب متوسط سطوع الصورة.
     */
    suspend fun averageBrightness(imagePath: String): Float = withContext(Dispatchers.Default) {
        try {
            val bitmap = BitmapFactory.decodeFile(imagePath) ?: return@withContext -1f
            val w = bitmap.width
            val h = bitmap.height
            val stepX = (w / 30).coerceAtLeast(1)
            val stepY = (h / 30).coerceAtLeast(1)
            try {
                var total = 0L
                var count = 0

                var y = 0
                while (y < h) {
                    var x = 0
                    while (x < w) {
                        val p = bitmap.getPixel(x, y)
                        val r = (p shr 16) and 0xFF
                        val g = (p shr 8) and 0xFF
                        val b = p and 0xFF
                        total += (r + g + b) / 3
                        count++
                        x += stepX
                    }
                    y += stepY
                }
                if (count > 0) (total.toFloat() / count) / 255f else -1f
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        } catch (_: Exception) {
            Logger.w("Brightness check failed")
            -1f
        }
    }

    fun release() {
        try {
            detector?.close()
            detector = null
        } catch (_: Exception) {
            Logger.w("Face detector release failed")
        }
    }
}
