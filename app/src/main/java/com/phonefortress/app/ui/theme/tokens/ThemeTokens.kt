package com.phonefortress.app.ui.theme.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * حزمة الرموز البصرية لكل ثيم.
 * كل ثيم يعرّف قيمه الخاصة — والمكونات تقرأ من هذه الباقة.
 */
data class ThemeTokens(
    val id: String,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val primaryGlow: Color,
    val secondary: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val safe: Color,
    val warning: Color,
    val danger: Color,
    val critical: Color,
    val cardCornerRadius: Dp,
    val buttonCornerRadius: Dp,
    val heroCornerRadius: Dp,
    val cardElevation: Dp,
    val contentPadding: Dp,
    val hasGlassmorphism: Boolean = false,
    val hasGridBackground: Boolean = false,
    val hasGlow: Boolean = false,
    val hasScanLine: Boolean = false,
    val isMonospace: Boolean = false,
    val hasAmbientWaves: Boolean = false,
    val cardGradient: List<Color> = emptyList()
)
