package com.phonefortress.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phonefortress.app.ui.theme.LocalThemeTokens
import com.phonefortress.app.ui.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onThemePicker: () -> Unit, onPinSetup: () -> Unit, onNavigateToDiagnostics: () -> Unit, onNavigateToLanguage: () -> Unit, themeViewModel: ThemeViewModel = hiltViewModel()) {
    val currentTheme by themeViewModel.selectedTheme.collectAsStateWithLifecycle()
    val tokens = LocalThemeTokens.current
    Scaffold(topBar = {
        TopAppBar(title = { Text("الإعدادات") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } })
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SettingsSection("المظهر") }
            item { SettingsItem(Icons.Default.Language, "اللغة", "اختيار لغة التطبيق", onNavigateToLanguage) }
            item { SettingsItem(Icons.Default.Palette, "الهوية البصرية", "${currentTheme.emoji} ${currentTheme.displayNameAr}", onThemePicker) }
            item { Spacer(Modifier.padding(4.dp)) }
            item { SettingsSection("الأمان") }
            item { SettingsItem(Icons.Default.Lock, "قفل التطبيق (PIN)", "حماية الإعدادات والسجلات", onPinSetup) }
            item { Spacer(Modifier.padding(4.dp)) }
            item { SettingsSection("عن التطبيق") }
            item { SettingsItem(Icons.Default.Info, "الإصدار", "2.0.0", {}) }
            item { SettingsItem(Icons.Default.Shield, "الإفصاح والخصوصية", "لا سحابة · لا جمع بيانات", {}) }
            item { SettingsItem(Icons.Default.BugReport, "تشخيص النظام", "عرض حالة العمال الخلفية", onNavigateToDiagnostics) }
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = LocalThemeTokens.current.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val tokens = LocalThemeTokens.current
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = tokens.surface)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tokens.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = tokens.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = tokens.textSecondary)
            }
            Icon(Icons.Default.ChevronLeft, null, tint = tokens.textMuted)
        }
    }
}
