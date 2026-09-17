package com.phonefortress.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phonefortress.app.data.prefs.LanguageManager
import com.phonefortress.app.ui.theme.LocalThemeTokens
import androidx.compose.ui.res.stringResource
import com.phonefortress.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerScreen(onBack: () -> Unit) {
    val tokens = LocalThemeTokens.current
    var currentLang by remember { mutableStateOf(LanguageManager.SUPPORTED_LANGUAGES.first().first) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.language_picker_title)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back)) } }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(LanguageManager.SUPPORTED_LANGUAGES, key = { it.first }) { (code, name) ->
                Card(
                    onClick = { currentLang = code; LanguageManager.applyLanguage(code) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = tokens.surface)
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = tokens.textPrimary)
                        if (code == currentLang) Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.common_selected), tint = tokens.primary)
                    }
                }
            }
        }
    }
}
