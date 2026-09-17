package com.phonefortress.app.data.repository

import com.phonefortress.app.data.local.dao.AlertLogDao
import com.phonefortress.app.data.local.entity.AlertLogEntity
import com.phonefortress.app.domain.model.AlertResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مستودع سجل التنبيهات.
 */
@Singleton
class AlertRepository @Inject constructor(
    private val dao: AlertLogDao
) {
    suspend fun logResult(eventId: String, result: AlertResult) {
        val entity = when (result) {
            is AlertResult.Success -> AlertLogEntity(
                eventId = eventId, channelId = result.channelId,
                status = "SUCCESS", messageId = result.messageId, reason = null,
                timestamp = System.currentTimeMillis()
            )
            is AlertResult.Retryable -> AlertLogEntity(
                eventId = eventId, channelId = result.channelId,
                status = "RETRYABLE", messageId = null, reason = result.reason,
                timestamp = System.currentTimeMillis()
            )
            is AlertResult.Fatal -> AlertLogEntity(
                eventId = eventId, channelId = result.channelId,
                status = "FATAL", messageId = null, reason = result.reason,
                timestamp = System.currentTimeMillis()
            )
            is AlertResult.NotConfigured -> AlertLogEntity(
                eventId = eventId, channelId = result.channelId,
                status = "NOT_CONFIGURED", messageId = null, reason = null,
                timestamp = System.currentTimeMillis()
            )
        }
        dao.insert(entity)
    }

    fun observeRecent(limit: Int = 100) = dao.observeRecent(limit)

    suspend fun getByEvent(eventId: String) = dao.getByEvent(eventId)

    suspend fun cleanup(beforeTimestamp: Long) = dao.deleteOlderThan(beforeTimestamp)
}
