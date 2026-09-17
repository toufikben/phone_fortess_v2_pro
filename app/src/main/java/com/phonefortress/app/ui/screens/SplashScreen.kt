package com.phonefortress.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phonefortress.app.ui.theme.LocalThemeTokens

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val tokens = LocalThemeTokens.current
    val transition = rememberInfiniteTransition(label = "splash")
    val scale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(1500); onFinished() }
    Box(Modifier.fillMaxSize().background(tokens.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🛡️", style = MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp), modifier = Modifier.scale(scale))
            Spacer(Modifier.height(24.dp))
            Text("PHONE FORTRESS", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 6.sp), color = tokens.primary)
            Spacer(Modifier.height(8.dp))
            Text("v2.0.0", style = MaterialTheme.typography.labelMedium, color = tokens.textSecondary)
        }
    }
}
