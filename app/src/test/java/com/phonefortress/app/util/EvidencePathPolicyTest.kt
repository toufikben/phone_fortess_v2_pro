package com.phonefortress.app.util

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.nio.file.Files
import org.junit.Test

class EvidencePathPolicyTest {
    @Test fun `rejects paths outside evidence root`() {
        val root = Files.createTempDirectory("evidence-policy").toFile()
        val evidence = File(root, Constants.DIR_EVIDENCE).apply { mkdirs() }
        val outside = File(root, "secret.jpg").apply { writeText("x") }
        assertThat(EvidencePathPolicy.safeFile(outside.path, root, ".jpg")).isNull()
    }

    @Test fun `accepts existing expected media under evidence root`() {
        val root = Files.createTempDirectory("evidence-policy").toFile()
        val evidence = File(root, Constants.DIR_PHOTOS).apply { mkdirs() }
        val photo = File(evidence, "capture.jpg").apply { writeText("x") }
        assertThat(EvidencePathPolicy.safeFile(photo.path, root, ".jpg")).isEqualTo(photo.canonicalFile)
    }
}
