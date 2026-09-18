package com.phonefortress.app.alerts

import com.phonefortress.app.alerts.template.AlertTemplate
import com.phonefortress.app.data.repository.AlertRepository
import com.phonefortress.app.domain.model.AlertChannel
import com.phonefortress.app.domain.model.AlertResult
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.util.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * موزع التنبيهات — يرسل بالتوازي عبر كل القنوات المفعّلة.
 * فشل قناة لا يُعطّل الأخرى.
 */
@Singleton
class AlertDispatcher @Inject constructor(
    private val channels: Set<@JvmSuppressWildcards AlertChannel>,
    private val repository: AlertRepository
) {

    /**
     * يرسل التنبيه عبر كل القنوات المتاحة.
     * @return قائمة نتائج كل قناة.
     */
    suspend fun dispatch(event: SecurityEvent): List<AlertResult> = coroutineScope {
        val payload = AlertTemplate.build(event)
        val successful = repository.getSuccessfulChannels(event.id)
        val configured = channels.filter { it.id !in successful }.filter {
            runCatching { it.isEnabled() && it.isConfigured() }.getOrDefault(false)
        }

        if (configured.isEmpty()) {
            if (successful.isNotEmpty()) {
                // The process may have died after every channel was delivered but
                // before the event transition to SENT. Replay durable successes so
                // the worker can finish that transition instead of finalizing the
                // event as a failure.
                return@coroutineScope successful.map { channelId ->
                    AlertResult.Success("already-delivered", channelId)
                }
            }
            Logger.w("No alert channel configured")
            return@coroutineScope emptyList()
        }

        Logger.i("Dispatching to ${configured.size} channels")

        val results = configured.map { channel ->
            async {
                val result = runCatching {
                    channel.send(event, payload)
                }.getOrElse { e ->
                    Logger.e(e, "Channel ${channel.id} crashed")
                    AlertResult.Retryable("Channel crashed: ${e.message}", channel.id)
                }
                repository.logResult(event.id, result)
                result
            }
        }.awaitAll()

        results
    }

    /** يعيد حالة كل قناة (مفعّلة/مهيأة). */
    suspend fun channelStatuses(): List<ChannelStatus> = coroutineScope {
        channels.map { channel ->
            async {
                ChannelStatus(
                    id = channel.id,
                    displayName = channel.displayName,
                    enabled = runCatching { channel.isEnabled() }.getOrDefault(false),
                    configured = runCatching { channel.isConfigured() }.getOrDefault(false)
                )
            }
        }.awaitAll()
    }
}

data class ChannelStatus(
    val id: String,
    val displayName: String,
    val enabled: Boolean,
    val configured: Boolean
)
