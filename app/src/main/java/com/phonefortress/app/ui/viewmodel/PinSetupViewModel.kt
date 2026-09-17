package com.phonefortress.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonefortress.app.data.prefs.PinPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PinSetupViewModel @Inject constructor(private val pinPrefs: PinPrefs) : ViewModel() {
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun save(pin: String, confirmation: String, biometric: Boolean) {
        when {
            pin.length !in 4..8 -> _error.value = "يجب أن يتكون PIN من 4 إلى 8 أرقام"
            pin != confirmation -> _error.value = "رموز PIN غير متطابقة"
            else -> viewModelScope.launch {
                pinPrefs.setPin(pin)
                pinPrefs.setBiometricEnabled(biometric)
                _error.value = null
                _saved.value = true
            }
        }
    }
}
