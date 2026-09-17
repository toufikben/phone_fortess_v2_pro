package com.phonefortress.app.ui.screens.pin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phonefortress.app.ui.viewmodel.PinSetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupScreen(onBack: () -> Unit, viewModel: PinSetupViewModel = hiltViewModel()) {
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var biometric by remember { mutableStateOf(true) }
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    LaunchedEffect(saved) { if (saved) onBack() }
    Scaffold(topBar = { TopAppBar(title = { Text("إعداد قفل التطبيق") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("أنشئ رمز PIN لحماية التطبيق والسجلات.")
            OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(8) }, label = { Text("PIN جديد") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(confirmation, { confirmation = it.filter(Char::isDigit).take(8) }, label = { Text("تأكيد PIN") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("السماح بفتح القفل بالبصمة")
                Switch(checked = biometric, onCheckedChange = { biometric = it })
            }
            error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            Button(onClick = { viewModel.save(pin, confirmation, biometric) }, modifier = Modifier.fillMaxWidth()) { Text("حفظ PIN") }
        }
    }
}
