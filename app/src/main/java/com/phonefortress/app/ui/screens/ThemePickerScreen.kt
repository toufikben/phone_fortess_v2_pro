package com.phonefortress.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phonefortress.app.domain.model.AppTheme
import com.phonefortress.app.ui.theme.LocalThemeTokens
import com.phonefortress.app.ui.theme.tokens.Themes
import com.phonefortress.app.ui.viewmodel.ThemeViewModel

@Composable
fun ThemePickerScreen(onBack: () -> Unit, viewModel: ThemeViewModel = hiltViewModel()) {
    val current by viewModel.selectedTheme.collectAsStateWithLifecycle()
    val tokens = LocalThemeTokens.current
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("الهوية البصرية") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item { Text("اختر الهوية التي تناسبك. التغيير فوري.", style = MaterialTheme.typography.bodyMedium, color = tokens.textSecondary) }
            items(AppTheme.entries, key = { it.id }) { theme ->
                ThemePreviewCard(theme, theme == current) { viewModel.setTheme(theme) }
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(theme: AppTheme, isSelected: Boolean, onSelect: () -> Unit) {
    val themeTokens = Themes.get(theme.id)
    Card(
        onClick = onSelect,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = themeTokens.background),
        border = if (isSelected) BorderStroke(2.dp, themeTokens.primary) else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(themeTokens.heroCornerRadius)).background(Brush.linearGradient(listOf(themeTokens.surface, themeTokens.surfaceVariant))),
                    contentAlignment = Alignment.Center
                ) { Text(theme.emoji, style = MaterialTheme.typography.titleLarge) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(theme.displayNameAr, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = themeTokens.textPrimary)
                    Text(theme.displayNameEn, style = MaterialTheme.typography.labelSmall, color = themeTokens.textSecondary)
                }
                if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = themeTokens.primary)
            }
            Spacer(Modifier.height(12.dp))
            Text(theme.descriptionAr, style = MaterialTheme.typography.bodySmall, color = themeTokens.textSecondary)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(themeTokens.primary, themeTokens.secondary, themeTokens.accent, themeTokens.safe, themeTokens.warning, themeTokens.danger).forEach { color ->
                    Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
                }
            }
        }
    }
}
