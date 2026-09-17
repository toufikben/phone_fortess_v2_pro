package com.phonefortress.app

import com.phonefortress.app.platform.ai.ThreatAnalyzer
import com.phonefortress.app.domain.model.ThreatLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreatAnalyzerTest {
    @Test fun `no face and repeated attempts becomes critical`() {
        val result = ThreatAnalyzer().assess(0, true, true, true, 4)
        assertEquals(100, result.score)
        assertEquals(ThreatLevel.CRITICAL, result.level)
    }

    @Test fun `normal known face in safe zone is low`() {
        val result = ThreatAnalyzer().assess(1, false, false, false, 1)
        assertTrue(result.score < 30)
        assertEquals(ThreatLevel.LOW, result.level)
    }
}
