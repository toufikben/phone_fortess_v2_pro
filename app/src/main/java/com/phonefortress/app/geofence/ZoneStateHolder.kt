package com.phonefortress.app.geofence

import com.phonefortress.app.domain.model.SafeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * حافظ حالة المناطق الحالية — Shared singleton.
 * يسمح للخدمة والواجهة بمعرفة المنطقة الحالية بسرعة.
 */
@Singleton
class ZoneStateHolder @Inject constructor() {

    private val _currentZone = MutableStateFlow<SafeZone?>(null)
    val currentZone: StateFlow<SafeZone?> = _currentZone.asStateFlow()

    private val _isInSafeZone = MutableStateFlow<Boolean?>(null)
    val isInSafeZone: StateFlow<Boolean?> = _isInSafeZone.asStateFlow()

    fun setCurrentZone(zone: SafeZone?) {
        _currentZone.value = zone
    }

    fun getCurrentZone(): SafeZone? = _currentZone.value

    fun setInSafeZone(value: Boolean?) {
        _isInSafeZone.value = value
    }

    /**
     * العتبة الفعلية بناءً على المنطقة الحالية.
     * - داخل منطقة → عتبتها.
     * - لا منطقة → عتبة افتراضية من الإعدادات.
     */
    fun effectiveThreshold(defaultThreshold: Int): Int =
        _currentZone.value?.effectiveThreshold() ?: defaultThreshold
}
