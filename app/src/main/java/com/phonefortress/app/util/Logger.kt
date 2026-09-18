package com.phonefortress.app.util

import android.util.Log
import timber.log.Timber

/**
 * غلاف حول Timber.
 * - في وضع debug: يطبع كل شيء.
 * - في وضع release: لا يطبع شيئًا حساسًا.
 * - فلترة تلقائية للكلمات الحساسة.
 */
object Logger {

    private val SENSITIVE_PATTERNS = listOf(
        Regex("(?i)(password|passwd|pwd)\\s*[:=]\\s*[^\\s,;]+"),
        Regex("(?i)(token|bearer|authorization)\\s*[:=]?\\s*[A-Za-z0-9._\\-]{8,}"),
        Regex("(?i)(api[_-]?key|apikey)\\s*[:=]\\s*[A-Za-z0-9._\\-]{8,}"),
        Regex("(?i)lat(itude)?\\s*[:=]\\s*[-\\d.]+"),
        Regex("(?i)(lng|lon|longitude)\\s*[:=]\\s*[-\\d.]+")
    )

    fun init(isDebug: Boolean) {
        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }

    fun d(message: String, vararg args: Any?) = Timber.d(sanitize(message), *sanitizeArgs(args))
    fun i(message: String, vararg args: Any?) = Timber.i(sanitize(message), *sanitizeArgs(args))
    fun w(message: String, vararg args: Any?) = Timber.w(sanitize(message), *sanitizeArgs(args))
    fun e(t: Throwable? = null, message: String, vararg args: Any?) {
        if (t != null) Timber.e(sanitize("${sanitize(message)}: ${t.message.orEmpty()}"), *sanitizeArgs(args))
        else Timber.e(sanitize(message), *sanitizeArgs(args))
    }

    private fun sanitizeArgs(args: Array<out Any?>): Array<out Any?> =
        args.map { value -> if (value is String) sanitize(value) else value }.toTypedArray()

    private fun sanitize(message: String): String {
        var sanitized = message
        SENSITIVE_PATTERNS.forEach { regex ->
            sanitized = regex.replace(sanitized, "[REDACTED]")
        }
        return sanitized
    }

    /** شجرة release — لا تطبع شيئًا. */
    private class ReleaseTree : Timber.Tree() {
        override fun isLoggable(tag: String?, priority: Int): Boolean = false
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // لا شيء في release
        }
    }
}
