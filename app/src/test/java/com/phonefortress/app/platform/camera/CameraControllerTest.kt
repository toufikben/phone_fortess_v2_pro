package com.phonefortress.app.platform.camera

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CameraControllerTest {
    @Test
    fun cancellationAfterRenameDeletesFinalEvidenceFile() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val root = File(context.cacheDir, "batch10-camera-${System.nanoTime()}").apply { mkdirs() }
        val temporary = File(root, "capture.jpg.tmp").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val finalFile = File(root, "capture.jpg")
        val active = AtomicBoolean(true)

        try {
            val result = CameraController(ApplicationProvider.getApplicationContext()).finalizePhotoFile(
                temporaryFile = temporary,
                finalFile = finalFile,
                continuationActive = { active.get() },
                afterRename = { active.set(false) }
            )

            assertThat(result).isNull()
            assertThat(temporary.exists()).isFalse()
            assertThat(finalFile.exists()).isFalse()
        } finally {
            root.deleteRecursively()
        }
    }
}
