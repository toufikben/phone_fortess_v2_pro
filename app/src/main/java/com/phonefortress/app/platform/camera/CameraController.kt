package com.phonefortress.app.platform.camera

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.phonefortress.app.util.Constants
import com.phonefortress.app.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * تحكم بالكاميرا الأمامية باستخدام CameraX.
 * - يلتقط صورة واحدة بأعلى دقة ممكنة.
 * - يُدير دورة حياة الكاميرا بأمان.
 */
@Singleton
class CameraController @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var executor: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * يلتقط صورة من الكاميرا الأمامية.
     * @param outputDir مجلد الحفظ.
     * @return مسار الملف أو null عند الفشل.
     */
    @SuppressLint("RestrictedApi")
    suspend fun captureFrontPhoto(
        lifecycleOwner: LifecycleOwner,
        outputDir: File
    ): File? = withContext(Dispatchers.IO) {
        var outputFile: File? = null
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Logger.w("Camera permission not granted")
                return@withContext null
            }
            if (executor.isShutdown) executor = Executors.newSingleThreadExecutor()
            if (!outputDir.exists()) outputDir.mkdirs()

            val provider = getCameraProvider() ?: return@withContext null
            val capture = bindCamera(provider, lifecycleOwner) ?: return@withContext null

            val file = File(
                outputDir,
                "intruder_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}_${UUID.randomUUID()}.jpg"
            )
            val temporaryFile = File(file.parentFile, "${file.name}.tmp")
            outputFile = temporaryFile

            val options = ImageCapture.OutputFileOptions.Builder(temporaryFile).build()

            return@withContext suspendCancellableCoroutine { cont ->
                capture.takePicture(
                    options,
                    executor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val saved = finalizePhotoFile(temporaryFile, file) { cont.isActive }
                            if (saved != null) {
                                Logger.i("Photo saved: ${file.name}")
                            }
                            if (cont.isActive) cont.resume(saved)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Logger.e(exception, "Photo capture failed")
                            temporaryFile.delete()
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                )
            }
        } catch (e: CancellationException) {
            outputFile?.delete()
            throw e
        } catch (e: Exception) {
            Logger.e(e, "Camera capture exception")
            outputFile?.delete()
            null
        }
    }

    private suspend fun getCameraProvider(): ProcessCameraProvider? =
        suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            cont.invokeOnCancellation { future.cancel(true) }
            future.addListener({
                try {
                    val provider = future.get()
                    cameraProvider = provider
                    if (cont.isActive) cont.resume(provider)
                } catch (e: Exception) {
                    Logger.e(e, "Camera provider failed")
                    if (cont.isActive) cont.resume(null)
                }
            }, ContextCompat.getMainExecutor(context))
        }

    private suspend fun bindCamera(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner
    ): ImageCapture? = withContext(Dispatchers.Main) {
        try {
            provider.unbindAll()

            val preview = Preview.Builder().build().also {
                // لا نعرض شيئاً — التقاط صامت
            }

            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetResolution(Size(1280, 720))
                .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                .build()

            val selector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            imageCapture = capture
            capture
        } catch (e: Exception) {
            Logger.e(e, "Camera bind failed")
            null
        }
    }

    /**
     * يُحرّر الكاميرا.
     */
    fun release() {
        try {
            cameraProvider?.unbindAll()
            cameraProvider = null
            imageCapture = null
            executor.shutdown()
        } catch (e: Exception) {
            Logger.e(e, "Camera release failed")
        }
    }

    internal fun finalizePhotoFile(
        temporaryFile: File,
        finalFile: File,
        continuationActive: () -> Boolean,
        afterRename: () -> Unit = {}
    ): File? {
        if (!continuationActive()) {
            temporaryFile.delete()
            return null
        }
        val moved = temporaryFile.renameTo(finalFile)
        afterRename()
        if (!continuationActive()) {
            if (moved) finalFile.delete() else temporaryFile.delete()
            return null
        }
        return finalFile.takeIf { moved }
    }
}
