package com.phonefortress.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.phonefortress.app.data.prefs.PinPrefs
import com.phonefortress.app.ui.navigation.AppNavHost
import com.phonefortress.app.ui.screens.pin.PinGateScreen
import com.phonefortress.app.ui.theme.AppThemeProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var pinPrefs: PinPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AppThemeProvider {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val pinEnabled by pinPrefs.isPinEnabled.collectAsState(initial = false)
                    var unlocked by remember(pinEnabled) { mutableStateOf(!pinEnabled) }
                    if (pinEnabled && !unlocked) {
                        PinGateScreen(onSuccess = { unlocked = true })
                    } else {
                        AppNavHost(rememberNavController())
                    }
                }
            }
        }
    }
}
