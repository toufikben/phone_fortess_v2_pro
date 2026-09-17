package com.phonefortress.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phonefortress.app.ui.theme.tokens.ThemeTokens
import com.phonefortress.app.ui.theme.tokens.Themes
import com.phonefortress.app.ui.viewmodel.ThemeViewModel

/**
 * مزود الثيم الرئيسي — يقرأ الاختيار ويُطبّق الثيم.
 */
@Composable
fun AppThemeProvider(
    viewModel: ThemeViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val theme by viewModel.selectedTheme.collectAsStateWithLifecycle()
    val tokens = Themes.get(theme.id)
    val colorScheme = createColorScheme(tokens)

    CompositionLocalProvider(LocalThemeTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typographyFor(tokens.isMonospace),
            content = content
        )
    }
}

@Composable
private fun createColorScheme(tokens: ThemeTokens) = darkColorScheme(
    primary = tokens.primary,
    onPrimary = tokens.background,
    primaryContainer = tokens.primaryGlow.copy(alpha = 0.2f),
    onPrimaryContainer = tokens.textPrimary,
    secondary = tokens.secondary,
    onSecondary = tokens.background,
    tertiary = tokens.accent,
    onTertiary = tokens.background,
    background = tokens.background,
    onBackground = tokens.textPrimary,
    surface = tokens.surface,
    onSurface = tokens.textPrimary,
    surfaceVariant = tokens.surfaceVariant,
    onSurfaceVariant = tokens.textSecondary,
    error = tokens.danger,
    onError = tokens.textPrimary,
    outline = tokens.textMuted
)

private fun typographyFor(monospace: Boolean) =
    com.phonefortress.app.ui.theme.tokens.TypographyFactory.build(monospace)
