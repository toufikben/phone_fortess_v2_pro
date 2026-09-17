package com.phonefortress.app.alerts.template

import com.google.common.truth.Truth.assertThat
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.domain.model.ThreatLevel
import org.junit.jupiter.api.Test

class AlertTemplateTest {
    @Test fun `builds payload with all fields`() {
        val p = AlertTemplate.build(SecurityEvent("evt-123", 1700000000000L, 5, 3, latitude = 24.7136, longitude = 46.6753, threatScore = 85, threatLevel = ThreatLevel.CRITICAL, threatReasons = listOf("وجه غير معروف", "خارج المنطقة")))
        assertThat(p.eventId).isEqualTo("evt-123"); assertThat(p.title).contains("غير مصرح"); assertThat(p.body).contains("5"); assertThat(p.body).contains("24.7136"); assertThat(p.body).contains("46.6753"); assertThat(p.body).contains("حرج"); assertThat(p.body).contains("85"); assertThat(p.body).contains("وجه غير معروف")
    }
    @Test fun `test mode is labeled`() { assertThat(AlertTemplate.build(SecurityEvent("test", 1L, 3, 3, isTest = true)).title).contains("اختبار") }
    @Test fun `missing location is handled`() { assertThat(AlertTemplate.build(SecurityEvent("x", 1L, 3, 3)).body).contains("غير متاح") }
}
