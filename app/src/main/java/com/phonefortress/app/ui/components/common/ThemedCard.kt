package com.phonefortress.app.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.phonefortress.app.ui.theme.LocalThemeTokens

/**
 * بطاقة موحّدة تتكيف مع كل الثيمات.
 * - تعرض glassmorphism إذا مُفعّل.
 * - تُطبّق الحدود والزوايا من الثيم.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ThemedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalThemeTokens.current
    val shape = RoundedCornerShape(tokens.cardCornerRadius)
    val backgroundBrush = tokens.cardGradient.takeIf { it.isNotEmpty() }?.let {
        Brush.linearGradient(it)
    }

    val baseModifier = modifier
        .fillMaxWidth()
        .clip(shape)
        .then(
            if (tokens.hasGlassmorphism) {
                Modifier.border(
                    width = 1.dp,
                    color = tokens.textPrimary.copy(alpha = 0.08f),
                    shape = shape
                )
            } else Modifier
        )

    Card(
        modifier = baseModifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = tokens.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.cardElevation),
        border = if (tokens.hasGridBackground) {
            BorderStroke(1.dp, tokens.primary.copy(alpha = 0.15f))
        } else null
    ) {
        Column(
            modifier = Modifier
                .then(backgroundBrush?.let { Modifier.background(it) } ?: Modifier)
                .padding(tokens.contentPadding),
            content = content
        )
    }
}
