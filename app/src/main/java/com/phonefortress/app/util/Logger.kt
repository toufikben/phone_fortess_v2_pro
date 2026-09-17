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
        Regex("(?i)password\\s*[:=]\\s*\\S+"),
        Regex("(?i)token\\s*[:=]\\s*\\S+"),
        Regex("(?i)api[_-]?key\\s*[:=]\\s*\\S+"),
        Regex("(?i)lat\\s*[:=]\\s*[-\\d.]+"),
        Regex("(?i)lng\\s*[:=]\\s*[-\\d.]+"),
        Regex("(?i)authorization\\s*[:=]\\s*\\S+")
    )

    fun init(isDebug: Boolean) {
        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }

    fun d(message: String, vararg args: Any?) = Timber.d(sanitize(message), *args)
    fun i(message: String, vararg args: Any?) = Timber.i(sanitize(message), *args)
    fun w(message: String, vararg args: Any?) = Timber.w(sanitize(message), *args)
    fun e(t: Throwable? = null, message: String, vararg args: Any?) {
        if (t != null) Timber.e(t, sanitize(message), *args)
        else Timber.e(sanitize(message), *args)
    }

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
