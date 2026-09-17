package com.phonefortress.app.ui.components.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phonefortress.app.ui.theme.LocalThemeTokens

@Composable
fun ProtectionHero(isActive: Boolean, uptimeText: String, modifier: Modifier = Modifier) {
    val tokens = LocalThemeTokens.current
    val transition = rememberInfiniteTransition(label = "hero")
    val pulse by transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val color = if (isActive) tokens.primary else tokens.textMuted
    val glowColor = if (isActive) tokens.primaryGlow else tokens.textMuted
    Box(modifier = modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(220.dp).scale(if (isActive) pulse else 1f)) {
            val radius = size.minDimension / 2f
            if (tokens.hasGlow && isActive) {
                drawCircle(
                    brush = Brush.radialGradient(listOf(glowColor.copy(alpha = 0.25f), Color.Transparent), center, radius),
                    radius = radius,
                    center = center
                )
            }
            drawCircle(color = color.copy(alpha = 0.35f), radius = radius - 4f, center = center, style = Stroke(2f))
            drawCircle(color = color.copy(alpha = 0.6f), radius = radius - 20f, center = center, style = Stroke(1f))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = if (isActive) "🛡️" else "🔒", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isActive) "PROTECTED" else "INACTIVE",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                color = color,
                textAlign = TextAlign.Center
            )
            if (isActive && uptimeText.isNotEmpty()) {
                Text(uptimeText, style = MaterialTheme.typography.labelSmall, color = tokens.textSecondary)
            }
        }
    }
}
