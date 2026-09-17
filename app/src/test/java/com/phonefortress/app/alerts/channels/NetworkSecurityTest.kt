package com.phonefortress.app.alerts.channels

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class NetworkSecurityTest {
    @Test
    fun `ntfy rejects cleartext endpoints`() {
        assertThat(NtfyChannel.isSecureEndpoint("http://example.test/topic")).isFalse()
        assertThat(NtfyChannel.isSecureEndpoint("https://example.test/topic")).isTrue()
    }

    @Test
    fun `webhook rejects cleartext endpoints`() {
        assertThat(WebhookChannel.isSecureEndpoint("http://example.test/hook")).isFalse()
        assertThat(WebhookChannel.isSecureEndpoint("https://example.test/hook")).isTrue()
    }

    @Test
    fun `smtp requires TLS`() {
        assertThat(GenericSmtpChannel.isSecureTransport(false)).isFalse()
        assertThat(GenericSmtpChannel.isSecureTransport(true)).isTrue()
    }
}
