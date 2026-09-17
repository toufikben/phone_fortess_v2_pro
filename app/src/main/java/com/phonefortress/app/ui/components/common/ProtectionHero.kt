package com.phonefortress.app.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.phonefortress.app.ui.theme.LocalThemeTokens

@Composable
fun ProtectionHero(
    isActive: Boolean,
    uptimeText: String,
    modifier: Modifier = Modifier
) {
    val tokens = LocalThemeTokens.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = tokens.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(tokens.heroCornerRadius)
    ) {
        Column(
            modifier = Modifier.padding(tokens.contentPadding * 1.5f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(tokens.contentPadding / 2)
        ) {
            Text(text = if (isActive) "🛡️" else "🔒", style = MaterialTheme.typography.displayLarge)
            Text(
                text = if (isActive) "الحماية نشطة" else "الحماية متوقفة",
                style = MaterialTheme.typography.headlineMedium,
                color = if (isActive) tokens.safe else tokens.textSecondary,
                fontWeight = FontWeight.Bold
            )
            Text(text = uptimeText, style = MaterialTheme.typography.bodyMedium, color = tokens.textSecondary)
        }
    }
}
