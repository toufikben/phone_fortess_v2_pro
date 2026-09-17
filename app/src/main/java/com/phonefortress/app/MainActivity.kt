package com.phonefortress.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.phonefortress.app.ui.theme.PhoneFortressTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * النشاط الرئيسي — يستضيف واجهة Compose.
 * يحتوي على شاشة Home الأولية فقط في هذه المرحلة.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PhoneFortressTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Placeholder — سيُستبدل بـ NavHost لاحقًا
                    com.phonefortress.app.ui.screens.HomePlaceholder()
                }
            }
        }
    }
}
