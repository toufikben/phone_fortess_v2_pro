package com.phonefortress.app.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.phonefortress.app.ui.theme.LocalThemeTokens

@Composable
fun ThemedBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val tokens = LocalThemeTokens.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
    ) {
        content()
    }
}
