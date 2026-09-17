package com.phonefortress.app.data.prefs

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/** مدير اللغة المدعوم على جميع إصدارات Android. */
object LanguageManager {
    val SUPPORTED_LANGUAGES = listOf(
        "ar" to "العربية", "en" to "English", "fr" to "Français", "es" to "Español",
        "de" to "Deutsch", "pt" to "Português", "it" to "Italiano", "ru" to "Русский",
        "zh-rCN" to "简体中文", "zh-rTW" to "繁體中文", "ja" to "日本語", "ko" to "한국어",
        "hi" to "हिन्दी", "ur" to "اردو", "tr" to "Türkçe", "fa" to "فارسی",
        "id" to "Bahasa Indonesia", "ms" to "Bahasa Melayu", "th" to "ไทย",
        "vi" to "Tiếng Việt", "he" to "עברית", "nl" to "Nederlands", "pl" to "Polski",
        "uk" to "Українська", "bn" to "বাংলা"
    )

    fun applyLanguage(code: String) {
        Locale.forLanguageTag(code)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
    }

    fun resetToSystem() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }
}
