package com.phonefortress.app.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.domain.model.ThreatLevel
import com.phonefortress.app.ui.components.common.ProtectionHero
import com.phonefortress.app.ui.components.common.ThemedBackground
import com.phonefortress.app.ui.components.common.ThemedCard
import com.phonefortress.app.ui.theme.LocalThemeTokens
import com.phonefortress.app.ui.theme.tokens.ThemeTokens
import com.phonefortress.app.ui.viewmodel.HomeViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.phonefortress.app.platform.admin.MyDeviceAdminReceiver

@Composable
fun HomeScreen(
    onNavigateToLogs: () -> Unit,
    onNavigateToZones: () -> Unit,
    onNavigateToChannels: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tokens = LocalThemeTokens.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) viewModel.enableProtectionIfReady() else viewModel.refreshProtectionState()
    }
    val adminLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (viewModel.isDeviceAdminActiveNow()) {
            if (state.requiredProtectionPermissions.isEmpty()) viewModel.enableProtectionIfReady()
            else permissionLauncher.launch(state.requiredProtectionPermissions.toTypedArray())
        } else viewModel.refreshProtectionState()
    }
    LaunchedEffect(Unit) { viewModel.refreshProtectionState() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshProtectionState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ThemedBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = tokens.contentPadding)
                .padding(top = 40.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PHONE FORTRESS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                ),
                color = tokens.primary
            )
            Spacer(Modifier.height(24.dp))
            ProtectionHero(state.isProtectionActive, state.uptimeText)
            Text(
                text = protectionStatusText(state),
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.isProtectionActive) tokens.safe else tokens.warning,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(
                    onClick = {
                        if (!state.isProtectionActive && !state.isDeviceAdminActive) {
                            val component = ComponentName(context, MyDeviceAdminReceiver::class.java)
                            adminLauncher.launch(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "يحتاج Phone Fortress إلى Device Admin لرصد محاولات فتح القفل الفاشلة.")
                            })
                        } else if (!state.isProtectionActive) {
                            if (state.requiredProtectionPermissions.isEmpty()) viewModel.enableProtectionIfReady()
                            else permissionLauncher.launch(state.requiredProtectionPermissions.toTypedArray())
                        } else viewModel.toggleProtection()
                    },
                modifier = Modifier.fillMaxWidth(0.7f).height(52.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (state.isProtectionActive) tokens.danger.copy(alpha = 0.15f) else tokens.primary,
                    contentColor = if (state.isProtectionActive) tokens.danger else tokens.background
                )
            ) {
                Icon(
                    imageVector = if (state.isProtectionActive) Icons.Default.Shield else Icons.Default.PlayArrow,
                    contentDescription = if (state.isProtectionActive) "حالة الحماية مفعلة" else "حالة الحماية متوقفة"
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (state.isProtectionActive) "إيقاف الحماية" else "تفعيل الحماية",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(24.dp))
            ThemedCard {
                Text("✨ نظرة سريعة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatCell("فتح", state.stats.attempts.toString())
                    StatCell("قفل", state.stats.locks.toString())
                    StatCell("صورة", state.stats.photos.toString())
                    StatCell("قناة", "${state.stats.channelsActive}/${state.stats.channelsTotal}")
                }
            }
            Spacer(Modifier.height(12.dp))
            state.lastEvent?.let { event ->
                ThemedCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("آخر حدث", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(state.lastEventTimeAgo, style = MaterialTheme.typography.labelSmall, color = tokens.textSecondary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(event.threatLevel.emoji, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${event.threatLevel.name} · ${event.threatScore}/100",
                            style = MaterialTheme.typography.bodyLarge,
                            color = event.color(tokens),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onNavigateToLogs) { Text("عرض التفاصيل →") }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAction("📍", "المناطق", onNavigateToZones, Modifier.weight(1f))
                QuickAction("📡", "القنوات", onNavigateToChannels, Modifier.weight(1f))
                QuickAction("⚙️", "الإعدادات", onNavigateToSettings, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    val tokens = LocalThemeTokens.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = tokens.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = tokens.textSecondary)
    }
}

@Composable
private fun QuickAction(emoji: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = LocalThemeTokens.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(tokens.buttonCornerRadius),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = tokens.textPrimary),
        border = BorderStroke(1.dp, tokens.primary.copy(alpha = 0.3f))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall, color = tokens.textSecondary)
        }
    }
}

private fun SecurityEvent.color(tokens: ThemeTokens) = when (threatLevel) {
    ThreatLevel.LOW -> tokens.safe
    ThreatLevel.MEDIUM -> tokens.warning
    ThreatLevel.HIGH -> tokens.danger
    ThreatLevel.CRITICAL -> tokens.critical
}

private fun protectionStatusText(state: com.phonefortress.app.ui.viewmodel.HomeState): String = when {
    state.isProtectionActive -> "الحماية مفعلة وتعمل الآن"
    !state.isDeviceAdminActive -> "يلزم تفعيل مسؤول الجهاز أولاً"
    !state.requiredProtectionPermissionsGranted -> "يلزم منح أذونات الالتقاط المطلوبة"
    else -> "الحماية متوقفة — اضغط لتفعيلها"
}
