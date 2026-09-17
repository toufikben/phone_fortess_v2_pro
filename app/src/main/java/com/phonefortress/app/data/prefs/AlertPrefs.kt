package com.phonefortress.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.phonefortress.app.data.crypto.CryptoHelper
import com.phonefortress.app.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.alertDataStore by preferencesDataStore(name = "alert_prefs")

/**
 * تخزين إعدادات قنوات التنبيه.
 * كل مفاتيح API تُشفَّر قبل الحفظ.
 */
@Singleton
class AlertPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crypto: CryptoHelper
) {

    private object Keys {
        // Telegram
        val TG_ENABLED = booleanPreferencesKey("tg_enabled")
        val TG_TOKEN_ENC = stringPreferencesKey("tg_token_enc")
        val TG_CHAT_ID = stringPreferencesKey("tg_chat_id")

        // ntfy
        val NTFY_ENABLED = booleanPreferencesKey("ntfy_enabled")
        val NTFY_TOPIC_URL = stringPreferencesKey("ntfy_topic_url")
        val NTFY_AUTH_ENC = stringPreferencesKey("ntfy_auth_enc")

        // SMTP
        val SMTP_ENABLED = booleanPreferencesKey("smtp_enabled")
        val SMTP_HOST = stringPreferencesKey("smtp_host")
        val SMTP_PORT = stringPreferencesKey("smtp_port")
        val SMTP_USER = stringPreferencesKey("smtp_user")
        val SMTP_PASS_ENC = stringPreferencesKey("smtp_pass_enc")
        val SMTP_FROM = stringPreferencesKey("smtp_from")
        val SMTP_TO = stringPreferencesKey("smtp_to")
        val SMTP_TLS = booleanPreferencesKey("smtp_tls")

        // Webhook
        val WH_ENABLED = booleanPreferencesKey("wh_enabled")
        val WH_URL = stringPreferencesKey("wh_url")
        val WH_METHOD = stringPreferencesKey("wh_method")
        val WH_SECRET_ENC = stringPreferencesKey("wh_secret_enc")

        // SMS
        val SMS_ENABLED = booleanPreferencesKey("sms_enabled")
        val SMS_NUMBERS = stringPreferencesKey("sms_numbers")

        // Local
        val LOCAL_ENABLED = booleanPreferencesKey("local_enabled")
    }

    // ---------- Telegram ----------
    val telegramEnabled: Flow<Boolean> = context.alertDataStore.data.map { it[Keys.TG_ENABLED] ?: false }
    val telegramChatId: Flow<String> = context.alertDataStore.data.map { it[Keys.TG_CHAT_ID] ?: "" }

    suspend fun getTelegramToken(): String? =
        context.alertDataStore.data.first()[Keys.TG_TOKEN_ENC]?.let {
            runCatching { crypto.decrypt(it) }.getOrNull()
        }

    suspend fun saveTelegram(token: String, chatId: String) {
        context.alertDataStore.edit { prefs ->
            prefs[Keys.TG_TOKEN_ENC] = crypto.encrypt(token)
            prefs[Keys.TG_CHAT_ID] = chatId
            prefs[Keys.TG_ENABLED] = true
        }
        Logger.i("Telegram settings saved")
    }

    suspend fun setTelegramEnabled(enabled: Boolean) {
        context.alertDataStore.edit { it[Keys.TG_ENABLED] = enabled }
    }

    // ---------- ntfy ----------
    val ntfyEnabled: Flow<Boolean> = context.alertDataStore.data.map { it[Keys.NTFY_ENABLED] ?: false }
    val ntfyUrl: Flow<String> = context.alertDataStore.data.map { it[Keys.NTFY_TOPIC_URL] ?: "" }

    suspend fun getNtfyAuth(): String? =
        context.alertDataStore.data.first()[Keys.NTFY_AUTH_ENC]?.let {
            runCatching { crypto.decrypt(it) }.getOrNull()
        }

    suspend fun saveNtfy(url: String, authHeader: String?) {
        context.alertDataStore.edit { prefs ->
            prefs[Keys.NTFY_TOPIC_URL] = url
            if (authHeader.isNullOrBlank()) {
                prefs.remove(Keys.NTFY_AUTH_ENC)
            } else {
                prefs[Keys.NTFY_AUTH_ENC] = crypto.encrypt(authHeader)
            }
            prefs[Keys.NTFY_ENABLED] = true
        }
    }

    suspend fun setNtfyEnabled(enabled: Boolean) {
        context.alertDataStore.edit { it[Keys.NTFY_ENABLED] = enabled }
    }

    // ---------- SMTP ----------
    val smtpEnabled: Flow<Boolean> = context.alertDataStore.data.map { it[Keys.SMTP_ENABLED] ?: false }

    suspend fun getSmtpConfig(): SmtpConfig? {
        val prefs = context.alertDataStore.data.first()
        val host = prefs[Keys.SMTP_HOST] ?: return null
        val port = prefs[Keys.SMTP_PORT]?.toIntOrNull() ?: return null
        val user = prefs[Keys.SMTP_USER] ?: return null
        val passEnc = prefs[Keys.SMTP_PASS_ENC] ?: return null
        val from = prefs[Keys.SMTP_FROM] ?: user
        val to = prefs[Keys.SMTP_TO] ?: return null
        val tls = prefs[Keys.SMTP_TLS] ?: true
        val pass = runCatching { crypto.decrypt(passEnc) }.getOrNull() ?: return null
        return SmtpConfig(host, port, user, pass, from, to, tls)
    }

    suspend fun saveSmtp(config: SmtpConfig) {
        context.alertDataStore.edit { prefs ->
            prefs[Keys.SMTP_HOST] = config.host
            prefs[Keys.SMTP_PORT] = config.port.toString()
            prefs[Keys.SMTP_USER] = config.user
            prefs[Keys.SMTP_PASS_ENC] = crypto.encrypt(config.password)
            prefs[Keys.SMTP_FROM] = config.from
            prefs[Keys.SMTP_TO] = config.to
            prefs[Keys.SMTP_TLS] = config.useTls
            prefs[Keys.SMTP_ENABLED] = true
        }
    }

    suspend fun setSmtpEnabled(enabled: Boolean) {
        context.alertDataStore.edit { it[Keys.SMTP_ENABLED] = enabled }
    }

    // ---------- Webhook ----------
    val webhookEnabled: Flow<Boolean> = context.alertDataStore.data.map { it[Keys.WH_ENABLED] ?: false }
    val webhookUrl: Flow<String> = context.alertDataStore.data.map { it[Keys.WH_URL] ?: "" }
    val webhookMethod: Flow<String> = context.alertDataStore.data.map { it[Keys.WH_METHOD] ?: "POST" }

    suspend fun getWebhookSecret(): String? =
        context.alertDataStore.data.first()[Keys.WH_SECRET_ENC]?.let {
            runCatching { crypto.decrypt(it) }.getOrNull()
        }

    suspend fun saveWebhook(url: String, method: String, secret: String?) {
        context.alertDataStore.edit { prefs ->
            prefs[Keys.WH_URL] = url
            prefs[Keys.WH_METHOD] = method
            if (secret.isNullOrBlank()) prefs.remove(Keys.WH_SECRET_ENC)
            else prefs[Keys.WH_SECRET_ENC] = crypto.encrypt(secret)
            prefs[Keys.WH_ENABLED] = true
        }
    }

    suspend fun setWebhookEnabled(enabled: Boolean) {
        context.alertDataStore.edit { it[Keys.WH_ENABLED] = enabled }
    }

    // ---------- SMS ----------
    val smsEnabled: Flow<Boolean> = context.alertDataStore.data.map { it[Keys.SMS_ENABLED] ?: false }
    val smsNumbers: Flow<String> = context.alertDataStore.data.map { it[Keys.SMS_NUMBERS] ?: "" }

    suspend fun saveSms(numbers: String) {
        context.alertDataStore.edit { prefs ->
            prefs[Keys.SMS_NUMBERS] = numbers
            prefs[Keys.SMS_ENABLED] = true
        }
    }

    suspend fun setSmsEnabled(enabled: Boolean) {
        context.alertDataStore.edit { it[Keys.SMS_ENABLED] = enabled }
    }

    // ---------- Local ----------
    val localEnabled: Flow<Boolean> = context.alertDataStore.data.map { it[Keys.LOCAL_ENABLED] ?: true }

    suspend fun setLocalEnabled(enabled: Boolean) {
        context.alertDataStore.edit { it[Keys.LOCAL_ENABLED] = enabled }
    }
}

data class SmtpConfig(
    val host: String,
    val port: Int,
    val user: String,
    val password: String,
    val from: String,
    val to: String,
    val useTls: Boolean
)
