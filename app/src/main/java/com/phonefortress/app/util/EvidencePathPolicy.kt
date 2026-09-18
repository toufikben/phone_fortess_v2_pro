package com.phonefortress.app.util

import java.io.File

/** Rejects arbitrary database/imported paths before media is opened or transmitted. */
object EvidencePathPolicy {
    fun safeFile(path: String?, filesDir: File, extension: String): File? {
        if (path.isNullOrBlank()) return null
        val evidenceRoot = File(filesDir, Constants.DIR_EVIDENCE).canonicalFile
        val candidate = runCatching { File(path).canonicalFile }.getOrNull() ?: return null
        val rootPath = evidenceRoot.path + File.separator
        if (!candidate.path.startsWith(rootPath)) return null
        if (!candidate.isFile || !candidate.name.endsWith(extension, ignoreCase = true)) return null
        return candidate
    }
}
