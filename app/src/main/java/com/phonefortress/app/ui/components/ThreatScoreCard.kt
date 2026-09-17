package com.phonefortress.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.domain.model.ThreatLevel
import com.phonefortress.app.domain.model.SecurityEventStatus

/**
 * بطاقة عرض درجة التهديد — تُستخدم في سجل الأحداث.
 */
@Composable
fun ThreatScoreCard(
    event: SecurityEvent,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "درجة الخطر",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${event.threatScore}/100",
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                        color = levelColor(event.threatLevel)
                    )
                }
                LevelBadge(event.threatLevel)
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { event.threatScore / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = levelColor(event.threatLevel)
            )

            if (event.threatReasons.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "الأسباب:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                event.threatReasons.forEach { reason ->
                    Text(
                        text = "• $reason",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "الحالة: ${statusLabel(event.status)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LevelBadge(level: ThreatLevel) {
    Surface(
        color = levelColor(level).copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = level.emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(6.dp))
            Text(
                text = levelName(level),
                style = MaterialTheme.typography.labelLarge,
                color = levelColor(level),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun levelColor(level: ThreatLevel): Color = when (level) {
    ThreatLevel.LOW -> Color(0xFF4CAF50)
    ThreatLevel.MEDIUM -> Color(0xFFFFC107)
    ThreatLevel.HIGH -> Color(0xFFFF9800)
    ThreatLevel.CRITICAL -> Color(0xFFF44336)
}

private fun levelName(level: ThreatLevel) = when (level) {
    ThreatLevel.LOW -> "منخفض"
    ThreatLevel.MEDIUM -> "متوسط"
    ThreatLevel.HIGH -> "مرتفع"
    ThreatLevel.CRITICAL -> "حرج"
}

private fun statusLabel(status: SecurityEventStatus) = when (status) {
    SecurityEventStatus.PENDING -> "في الانتظار"
    SecurityEventStatus.DEFERRED -> "مؤجل"
    SecurityEventStatus.IN_PROGRESS -> "قيد المعالجة"
    SecurityEventStatus.CAPTURED -> "تم الالتقاط"
    SecurityEventStatus.SEND_PENDING -> "بانتظار الإرسال"
    SecurityEventStatus.SENT -> "تم الإرسال"
    SecurityEventStatus.FAILED_RETRYABLE -> "فشل مؤقت — ستتم المحاولة مجدداً"
    SecurityEventStatus.FAILED_FINAL -> "فشل نهائي"
    SecurityEventStatus.CANCELLED -> "أُلغي"
}
