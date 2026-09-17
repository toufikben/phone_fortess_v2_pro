package com.phonefortress.app.ui.theme.tokens

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * بناء نظام الحروف حسب نوع الثيم.
 */
object TypographyFactory {
    fun build(monospace: Boolean): Typography {
        val family = if (monospace) FontFamily.Monospace else FontFamily.Default
        val tracking = if (monospace) 1.5.sp else 0.sp
        fun style(size: androidx.compose.ui.unit.TextUnit, weight: FontWeight, spaced: Boolean = false) =
            TextStyle(
                fontFamily = family,
                fontWeight = weight,
                fontSize = size,
                letterSpacing = if (spaced) tracking else 0.sp
            )

        return Typography(
            displayLarge = style(32.sp, FontWeight.Black, true),
            headlineMedium = style(24.sp, FontWeight.Bold, true),
            titleLarge = style(20.sp, FontWeight.SemiBold, true),
            titleMedium = style(16.sp, FontWeight.SemiBold, true),
            bodyLarge = style(16.sp, FontWeight.Normal),
            bodyMedium = style(14.sp, FontWeight.Normal),
            bodySmall = style(12.sp, FontWeight.Normal),
            labelLarge = style(14.sp, FontWeight.Medium, true),
            labelMedium = style(12.sp, FontWeight.Medium, true),
            labelSmall = style(10.sp, FontWeight.Medium, true)
        )
    }
}
