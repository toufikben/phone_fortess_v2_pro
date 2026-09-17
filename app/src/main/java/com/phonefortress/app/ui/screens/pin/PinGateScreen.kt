package com.phonefortress.app.ui.screens.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.phonefortress.app.ui.theme.LocalThemeTokens
import com.phonefortress.app.ui.viewmodel.PinViewModel

@Composable
fun PinGateScreen(onSuccess: () -> Unit, viewModel: PinViewModel = hiltViewModel()) {
    val state by viewModel.gateState.collectAsState()
    val tokens = LocalThemeTokens.current
    LaunchedEffect(state.isUnlocked) { if (state.isUnlocked) onSuccess() }
    Column(
        modifier = Modifier.fillMaxSize().background(tokens.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔐", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text("أدخل رمز الحماية", style = MaterialTheme.typography.titleLarge, color = tokens.textPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(8) { index ->
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (index < state.pin.length) tokens.primary else tokens.textMuted.copy(alpha = 0.3f))
                        .semantics { contentDescription = "خانة PIN ${index + 1}" }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        when {
            state.lockoutRemaining > 0 -> Text("مقفل مؤقتاً · ${state.lockoutRemaining} ثانية", color = tokens.danger)
            state.errorMessage != null -> Text(state.errorMessage.orEmpty(), color = tokens.danger)
            else -> Spacer(Modifier.height(20.dp))
        }
        Spacer(Modifier.height(32.dp))
        PinPad(
            enabled = !state.isLockedOut,
            onDigit = viewModel::appendDigit,
            onDelete = viewModel::deleteDigit,
            onBiometric = if (state.biometricAvailable) viewModel::requestBiometric else null,
            primaryColor = tokens.primary,
            textColor = tokens.textPrimary,
            surfaceColor = tokens.surface
        )
    }
}

@Composable
private fun PinPad(enabled: Boolean, onDigit: (Char) -> Unit, onDelete: () -> Unit, onBiometric: (() -> Unit)?, primaryColor: androidx.compose.ui.graphics.Color, textColor: androidx.compose.ui.graphics.Color, surfaceColor: androidx.compose.ui.graphics.Color) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("BIO", "0", "DEL"))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { key ->
                    when (key) {
                        "BIO" -> if (onBiometric != null) PinButton(null, Icons.Default.Fingerprint, onBiometric, enabled, primaryColor, textColor, surfaceColor) else Spacer(Modifier.size(72.dp))
                        "DEL" -> PinButton(null, Icons.Default.Backspace, onDelete, enabled, primaryColor, textColor, surfaceColor)
                        else -> PinButton(key, null, { onDigit(key.single()) }, enabled, primaryColor, textColor, surfaceColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun PinButton(label: String?, icon: androidx.compose.ui.graphics.vector.ImageVector?, onClick: () -> Unit, enabled: Boolean, primaryColor: androidx.compose.ui.graphics.Color, textColor: androidx.compose.ui.graphics.Color, surfaceColor: androidx.compose.ui.graphics.Color) {
    val description = when (label) { null -> if (icon == Icons.Default.Backspace) "حذف آخر رقم" else "المصادقة بالبصمة"; else -> "رقم $label" }
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(72.dp)) {
        if (icon != null) Icon(icon, description, tint = if (enabled) primaryColor else textColor.copy(alpha = 0.4f))
        else Text(label.orEmpty(), style = MaterialTheme.typography.headlineMedium, color = if (enabled) textColor else textColor.copy(alpha = 0.4f))
    }
}
