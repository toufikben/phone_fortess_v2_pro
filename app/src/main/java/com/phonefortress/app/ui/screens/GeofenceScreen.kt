package com.phonefortress.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phonefortress.app.domain.model.SafeZone
import com.phonefortress.app.domain.model.ZoneType
import com.phonefortress.app.ui.viewmodel.GeofenceViewModel
import androidx.compose.ui.res.stringResource
import com.phonefortress.app.R

/**
 * شاشة إدارة المناطق الجغرافية.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofenceScreen(
    onBack: () -> Unit,
    viewModel: GeofenceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.geofence_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showAddDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.geofence_add))
                    }
                }
            )
        },
        snackbarHost = {
            state.message?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(2500)
                    viewModel.clearMessage()
                }
            }
        }
    ) { padding ->
        if (state.zones.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onAdd = { viewModel.showAddDialog() }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    InfoBanner()
                }

                items(state.zones, key = { it.id }) { zone ->
                    ZoneCard(
                        zone = zone,
                        isCurrent = state.currentZoneId == zone.id,
                        onToggle = { viewModel.toggleZone(zone.id, it) },
                        onDelete = { viewModel.deleteZone(zone.id) }
                    )
                }
            }
        }

        if (state.showAddDialog) {
            AddZoneDialog(
                onDismiss = { viewModel.dismissAddDialog() },
                onAdd = { name, lat, lng, radius, type ->
                    viewModel.addZone(name, lat, lng, radius, type)
                },
                useCurrentLocation = { viewModel.getCurrentLocationForDialog() },
                currentLocation = state.dialogLocation
            )
        }
    }
}

@Composable
private fun InfoBanner() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "💡 كيف تعمل المناطق؟",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "🟢 آمنة: عتبة 5 محاولات (المنزل)\n" +
                       "🟡 محايدة: عتبة 3 محاولات (افتراضي)\n" +
                       "🔴 خطر: عتبة محاولة واحدة (مطار، محطة)",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ZoneCard(
    zone: SafeZone,
    isCurrent: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        border = if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = zone.type.emoji, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = zone.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isCurrent) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "أنت هنا",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${zone.type.labelAr} • عتبة ${zone.effectiveThreshold()} • ${zone.radiusMeters.toInt()}م",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete))
                }
            }
            Switch(
                checked = zone.enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier, onAdd: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "📍", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.geofence_empty),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.geofence_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.geofence_add))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.geofence_add))
        }
    }
}

@Composable
private fun AddZoneDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, Double, Float, ZoneType) -> Unit,
    useCurrentLocation: () -> Unit,
    currentLocation: Pair<Double, Double>?
) {
    var name by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf(200f) }
    var type by remember { mutableStateOf(ZoneType.SAFE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.geofence_add)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.geofence_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.geofence_type), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ZoneType.entries.forEach { zt ->
                        FilterChip(
                            selected = type == zt,
                            onClick = { type = zt },
                            label = { Text("${zt.emoji} ${zt.labelAr}") }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.geofence_radius, radius.toInt()))
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 100f..2000f,
                    steps = 19
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = useCurrentLocation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (currentLocation == null) "استخدم موقعي الحالي" else "✓ تم تحديد الموقع")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && currentLocation != null) {
                        onAdd(name, currentLocation.first, currentLocation.second, radius, type)
                    }
                },
                enabled = name.isNotBlank() && currentLocation != null
            ) { Text(stringResource(R.string.geofence_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
