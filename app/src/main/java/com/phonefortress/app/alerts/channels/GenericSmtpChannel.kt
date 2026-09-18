package com.phonefortress.app.alerts.channels

import com.phonefortress.app.alerts.template.AlertTemplate
import com.phonefortress.app.data.prefs.AlertPrefs
import com.phonefortress.app.data.prefs.SmtpConfig
import com.phonefortress.app.domain.model.AlertChannel
import com.phonefortress.app.domain.model.AlertPayload
import com.phonefortress.app.domain.model.AlertResult
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.util.Logger
import com.phonefortress.app.util.EvidencePathPolicy
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.eclipse.angus.mail.util.MailSSLSocketFactory
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton
import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.activation.DataHandler
import jakarta.activation.FileDataSource

/**
 * قناة SMTP عامة — تدعم أي مزود (Gmail, Outlook, Yahoo, Zoho, مخصص).
 * الإرسال مباشر من الجهاز بدون خدمة وسيطة.
 */
@Singleton
class GenericSmtpChannel @Inject constructor(
    private val prefs: AlertPrefs,
    @ApplicationContext private val context: Context
) : AlertChannel {

    companion object {
        internal fun isSecureTransport(useTls: Boolean): Boolean = useTls
    }

    override val id: String = "smtp"
    override val displayName: String = "بريد إلكتروني (SMTP)"
    override val requiresConfig: Boolean = true

    override suspend fun isEnabled(): Boolean = prefs.smtpEnabled.first()

    override suspend fun isConfigured(): Boolean {
        if (!isEnabled()) return false
        val cfg = prefs.getSmtpConfig() ?: return false
        return cfg.host.isNotBlank() && cfg.port > 0 && cfg.to.isNotBlank() && isSecureTransport(cfg.useTls)
    }

    override suspend fun send(event: SecurityEvent, payload: AlertPayload): AlertResult =
        withContext(Dispatchers.IO) {
            val cfg = prefs.getSmtpConfig()
                ?: return@withContext AlertResult.Fatal("SMTP not configured", id)
            if (!isSecureTransport(cfg.useTls)) {
                return@withContext AlertResult.Fatal("SMTP requires TLS", id)
            }

            try {
                val session = createSession(cfg)
                val message = buildMessage(session, cfg, payload)
                Transport.send(message)
                AlertResult.Success("smtp-${event.id}", id)
            } catch (e: jakarta.mail.AuthenticationFailedException) {
                Logger.e(e, "SMTP auth failed")
                AlertResult.Fatal("SMTP auth failed", id)
            } catch (e: jakarta.mail.MessagingException) {
                Logger.e(e, "SMTP messaging error")
                AlertResult.Retryable("SMTP error: ${e.message}", id)
            } catch (e: Exception) {
                Logger.e(e, "SMTP unexpected")
                AlertResult.Retryable("SMTP unexpected: ${e.message}", id)
            }
        }

    private fun createSession(cfg: SmtpConfig): Session {
        val props = Properties().apply {
            put("mail.smtp.host", cfg.host)
            put("mail.smtp.port", cfg.port.toString())
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", cfg.useTls.toString())
            put("mail.smtp.starttls.required", cfg.useTls.toString())
            put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")
            put("mail.smtp.ssl.checkserveridentity", "true")
            put("mail.smtp.connectiontimeout", "15000")
            put("mail.smtp.timeout", "15000")
            put("mail.smtp.writetimeout", "15000")
        }
        return Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() =
                PasswordAuthentication(cfg.user, cfg.password)
        })
    }

    private fun buildMessage(session: Session, cfg: SmtpConfig, payload: AlertPayload): MimeMessage {
        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(cfg.from))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(cfg.to))
            subject = payload.title
            setHeader("X-Security-Event-Id", payload.eventId)
        }

        val textPart = MimeBodyPart().apply {
            setText(payload.body, "UTF-8")
        }

        val multipart = MimeMultipart().apply { addBodyPart(textPart) }

        payload.photoPath?.let { path ->
            val file = EvidencePathPolicy.safeFile(path, context.filesDir, ".jpg")
            if (file != null) {
                val attach = MimeBodyPart().apply {
                    dataHandler = DataHandler(FileDataSource(file))
                    fileName = "intruder.jpg"
                }
                multipart.addBodyPart(attach)
            }
        }

        payload.audioPath?.let { path ->
            val file = EvidencePathPolicy.safeFile(path, context.filesDir, ".m4a")
            if (file != null) {
                val attach = MimeBodyPart().apply {
                    dataHandler = DataHandler(FileDataSource(file))
                    fileName = "evidence.m4a"
                }
                multipart.addBodyPart(attach)
            }
        }

        message.setContent(multipart)
        return message
    }

    override suspend fun test(): AlertResult {
        val now = System.currentTimeMillis()
        val testEvent = SecurityEvent(
            id = "test-${now}",
            timestamp = now,
            failedAttempts = 3, threshold = 3, isTest = true,
            threatScore = 50,
            threatLevel = com.phonefortress.app.domain.model.ThreatLevel.MEDIUM
        )
        return send(testEvent, AlertTemplate.build(testEvent))
    }
}
