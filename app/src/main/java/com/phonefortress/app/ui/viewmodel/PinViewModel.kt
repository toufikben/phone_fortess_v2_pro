package com.phonefortress.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonefortress.app.data.prefs.PinPrefs
import com.phonefortress.app.platform.security.BiometricAuthenticator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

 data class PinGateState(
    val pin: String = "",
    val isUnlocked: Boolean = false,
    val isLockedOut: Boolean = false,
    val lockoutRemaining: Long = 0,
    val errorMessage: String? = null,
    val biometricAvailable: Boolean = false
)

@HiltViewModel
class PinViewModel @Inject constructor(
    private val pinPrefs: PinPrefs,
    private val biometric: BiometricAuthenticator,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _gateState = MutableStateFlow(PinGateState())
    val gateState: StateFlow<PinGateState> = _gateState.asStateFlow()
    private var lockoutJob: Job? = null

    init {
        viewModelScope.launch {
            val enabled = pinPrefs.isPinEnabled.first()
            val biometricEnabled = pinPrefs.isBiometricEnabled.first()
            _gateState.update { it.copy(biometricAvailable = enabled && biometricEnabled && biometric.isAvailable()) }
            refreshLockout()
        }
    }

    fun appendDigit(digit: Char) {
        if (_gateState.value.isLockedOut || _gateState.value.pin.length >= 8) return
        val pin = _gateState.value.pin + digit
        _gateState.update { it.copy(pin = pin, errorMessage = null) }
        if (pin.length >= 4) verify(pin)
    }

    fun deleteDigit() { _gateState.update { it.copy(pin = it.pin.dropLast(1), errorMessage = null) } }

    fun requestBiometric() {
        val activity = context as? androidx.fragment.app.FragmentActivity ?: return
        viewModelScope.launch {
            if (pinPrefs.isPinEnabled.first() && pinPrefs.isBiometricEnabled.first() && biometric.authenticate(activity)) {
                _gateState.update { it.copy(isUnlocked = true) }
            }
        }
    }

    private fun verify(pin: String) {
        viewModelScope.launch {
            when (val result = pinPrefs.verifyPin(pin)) {
                PinPrefs.VerifyResult.Success -> _gateState.update { it.copy(isUnlocked = true, pin = "") }
                PinPrefs.VerifyResult.NotSet -> _gateState.update { it.copy(errorMessage = "لم يتم إعداد PIN", pin = "") }
                PinPrefs.VerifyResult.Corrupted -> _gateState.update { it.copy(errorMessage = "بيانات PIN تالفة؛ الحماية مقفلة", pin = "") }
                is PinPrefs.VerifyResult.WrongPin -> _gateState.update { it.copy(errorMessage = "رمز غير صحيح · المتبقي ${result.remainingAttempts}", pin = "") }
                is PinPrefs.VerifyResult.LockedOut -> { _gateState.update { it.copy(pin = "") }; startLockout(result.until) }
            }
        }
    }

    private fun startLockout(until: Long) {
        lockoutJob?.cancel()
        lockoutJob = viewModelScope.launch {
            while (System.currentTimeMillis() < until) {
                val remaining = ((until - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                _gateState.update { it.copy(isLockedOut = true, lockoutRemaining = remaining) }
                delay(1000)
            }
            _gateState.update { it.copy(isLockedOut = false, lockoutRemaining = 0) }
        }
    }

    private suspend fun refreshLockout() {
        val until = pinPrefs.lockoutUntil.first()
        if (until > System.currentTimeMillis()) startLockout(until)
    }
}
