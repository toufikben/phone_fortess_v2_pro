package com.phonefortress.app.platform.ai

import com.phonefortress.app.domain.model.ThreatAssessment
import com.phonefortress.app.domain.model.ThreatLevel

class ThreatAnalyzer {
    // حساب درجة الخطر محلياً دون إرسال الصورة أو البيانات إلى أي خادم.
    fun assess(faces: Int, unknownFace: Boolean, outsideSafeZone: Boolean, unusualTime: Boolean, attempts: Int): ThreatAssessment {
        var score = 0
        val reasons = mutableListOf<String>()
        if (faces == 0) { score += 20; reasons += "camera_obscured" }
        if (faces > 1) { score += 30; reasons += "multiple_faces" }
        if (unknownFace) { score += 40; reasons += "unknown_face" }
        if (outsideSafeZone) { score += 20; reasons += "outside_safe_zone" }
        if (unusualTime) { score += 15; reasons += "unusual_time" }
        if (attempts > 3) { score += 25; reasons += "repeated_attempts" }
        val bounded = score.coerceIn(0, 100)
        val level = when { bounded >= 80 -> ThreatLevel.CRITICAL; bounded >= 60 -> ThreatLevel.HIGH; bounded >= 30 -> ThreatLevel.MEDIUM; else -> ThreatLevel.LOW }
        return ThreatAssessment(bounded, level, reasons)
    }
}
