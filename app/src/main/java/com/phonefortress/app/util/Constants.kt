package com.phonefortress.app.util

/**
 * ثوابت عامة للتطبيق.
 */
object Constants {
    // Notification channels
    const val CHANNEL_PROTECTION = "channel_protection"
    const val CHANNEL_ALERTS = "channel_alerts"
    const val CHANNEL_SILENT = "channel_silent"

    // Notification IDs
    const val NOTIF_ID_PROTECTION = 1001
    const val NOTIF_ID_ALERT = 1002
    const val NOTIF_ID_CAPTURE = 1003

    // Work
    const val WORK_CAPTURE_RETRY = "work_capture_retry"
    const val WORK_PHOTO_CLEANUP = "work_photo_cleanup"
    const val WORK_EVENT_DISPATCH = "work_event_dispatch"
    const val CAPTURE_RETRY_MAX_ATTEMPTS = 3
    const val CAPTURE_RETRY_DELAY_SEC = 30L
    const val DISPATCH_MAX_ATTEMPTS = 5
    const val DISPATCH_DELAY_SEC = 15L
    const val PHOTO_CLEANUP_INTERVAL_HOURS = 24L
    const val PERIODIC_DISPATCH_INTERVAL_HOURS = 6L

    // Storage
    const val DIR_EVIDENCE = "evidence"
    const val DIR_PHOTOS = "evidence/photos"
    const val DIR_AUDIO = "evidence/audio"

    // Timeouts (ms)
    const val CAMERA_TIMEOUT_MS = 15_000L
    const val LOCATION_TIMEOUT_MS = 10_000L
    const val MAX_LOCATION_AGE_MS = 60_000L
    const val MAX_LOCATION_ACCURACY_METERS = 100f
    const val AUDIO_DURATION_MS = 10_000L

    // Thresholds
    const val MIN_THRESHOLD = 1
    const val MAX_THRESHOLD = 10
    const val DEFAULT_THRESHOLD = 3

    // Retention
    const val DEFAULT_RETENTION_DAYS = 7
}
