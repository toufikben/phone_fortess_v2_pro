package com.phonefortress.app.ui.theme

import androidx.compose.runtime.compositionLocalOf
import com.phonefortress.app.ui.theme.tokens.ThemeTokens
import com.phonefortress.app.ui.theme.tokens.Themes

/**
 * CompositionLocal للوصول إلى الثيم الحالي من أي مكوّن.
 */
val LocalThemeTokens = compositionLocalOf<ThemeTokens> { Themes.GuardianMinimal }
