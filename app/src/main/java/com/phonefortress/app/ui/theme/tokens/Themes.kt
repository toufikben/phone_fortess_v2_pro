package com.phonefortress.app.ui.theme.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object Themes {
    val FortressNoir = ThemeTokens(
        id = "fortress_noir",
        background = Color(0xFF050505),
        surface = Color(0xFF0F0F12),
        surfaceVariant = Color(0xFF1A1A20),
        primary = Color(0xFFC9A227),
        primaryGlow = Color(0xFFF5D76E),
        secondary = Color(0xFF8B8B93),
        accent = Color(0xFFD4AF37),
        textPrimary = Color(0xFFE8E8E8),
        textSecondary = Color(0xFF8B8B93),
        textMuted = Color(0xFF55555A),
        safe = Color(0xFF2A9D8F),
        warning = Color(0xFFE9C46A),
        danger = Color(0xFFE63946),
        critical = Color(0xFFBB0A1E),
        cardCornerRadius = 12.dp,
        buttonCornerRadius = 8.dp,
        heroCornerRadius = 24.dp,
        cardElevation = 2.dp,
        contentPadding = 16.dp,
        hasGridBackground = true,
        hasGlow = true
    )

    val LiquidShield = ThemeTokens(
        id = "liquid_shield",
        background = Color(0xFF0A0E1A),
        surface = Color(0xFF1A1F35).copy(alpha = 0.65f),
        surfaceVariant = Color(0xFF232A47).copy(alpha = 0.5f),
        primary = Color(0xFF00D4FF),
        primaryGlow = Color(0xFF7B5CFF),
        secondary = Color(0xFF7B5CFF),
        accent = Color(0xFF39FF14),
        textPrimary = Color(0xFFF0F4F8),
        textSecondary = Color(0xFFB8C2D4),
        textMuted = Color(0xFF6B7599),
        safe = Color(0xFF39FF14),
        warning = Color(0xFFFF6B35),
        danger = Color(0xFFFF3B5C),
        critical = Color(0xFFC1121F),
        cardCornerRadius = 28.dp,
        buttonCornerRadius = 24.dp,
        heroCornerRadius = 32.dp,
        cardElevation = 0.dp,
        contentPadding = 20.dp,
        hasGlassmorphism = true,
        hasAmbientWaves = true,
        cardGradient = listOf(
            Color(0xFF00D4FF).copy(alpha = 0.15f),
            Color(0xFF7B5CFF).copy(alpha = 0.08f)
        )
    )

    val CyberTerminal = ThemeTokens(
        id = "cyber_terminal",
        background = Color(0xFF000000),
        surface = Color(0xFF0A0F0A),
        surfaceVariant = Color(0xFF0F1A0F),
        primary = Color(0xFF00FF41),
        primaryGlow = Color(0xFF39FF88),
        secondary = Color(0xFF00D9FF),
        accent = Color(0xFFFFD400),
        textPrimary = Color(0xFFB3FFB3),
        textSecondary = Color(0xFF66CC66),
        textMuted = Color(0xFF334433),
        safe = Color(0xFF00FF41),
        warning = Color(0xFFFFD400),
        danger = Color(0xFFFF0040),
        critical = Color(0xFFFF0040),
        cardCornerRadius = 4.dp,
        buttonCornerRadius = 2.dp,
        heroCornerRadius = 8.dp,
        cardElevation = 0.dp,
        contentPadding = 12.dp,
        hasScanLine = true,
        isMonospace = true
    )

    val GuardianMinimal = ThemeTokens(
        id = "guardian_minimal",
        background = Color(0xFF0B0D10),
        surface = Color(0xFF151821),
        surfaceVariant = Color(0xFF1E2230),
        primary = Color(0xFFE8B04B),
        primaryGlow = Color(0xFFF5C56E),
        secondary = Color(0xFFA78BFA),
        accent = Color(0xFFE8B04B),
        textPrimary = Color(0xFFF1F5F9),
        textSecondary = Color(0xFF94A3B8),
        textMuted = Color(0xFF4B5563),
        safe = Color(0xFF34D399),
        warning = Color(0xFFFBBF24),
        danger = Color(0xFFF87171),
        critical = Color(0xFFDC2626),
        cardCornerRadius = 16.dp,
        buttonCornerRadius = 12.dp,
        heroCornerRadius = 24.dp,
        cardElevation = 3.dp,
        contentPadding = 16.dp,
        hasGlow = true,
        cardGradient = listOf(
            Color(0xFFE8B04B).copy(alpha = 0.12f),
            Color(0xFFA78BFA).copy(alpha = 0.05f)
        )
    )

    fun get(themeId: String): ThemeTokens = when (themeId) {
        "fortress_noir" -> FortressNoir
        "liquid_shield" -> LiquidShield
        "cyber_terminal" -> CyberTerminal
        else -> GuardianMinimal
    }
}
