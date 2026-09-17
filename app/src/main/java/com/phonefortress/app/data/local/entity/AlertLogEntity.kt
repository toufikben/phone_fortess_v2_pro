package com.phonefortress.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * سجل إرسال التنبيه لكل قناة.
 */
@Entity(
    tableName = "alert_logs",
    indices = [Index("eventId"), Index("channelId")]
)
data class AlertLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: String,
    val channelId: String,
    val status: String,        // SUCCESS / RETRYABLE / FATAL / NOT_CONFIGURED
    val messageId: String?,
    val reason: String?,
    val timestamp: Long
)
