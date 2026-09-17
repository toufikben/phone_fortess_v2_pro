package com.phonefortress.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.phonefortress.app.data.session.SessionManager
import com.phonefortress.app.ui.navigation.AppNavHost
import com.phonefortress.app.ui.screens.pin.PinGateScreen
import com.phonefortress.app.ui.theme.AppThemeProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AppThemeProvider {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var pinRequired by remember { mutableStateOf<Boolean?>(null) }
                    var unlocked by remember { mutableStateOf(false) }
                    val navController = rememberNavController()
                    LaunchedEffect(Unit) {
                        pinRequired = sessionManager.shouldRequireUnlock()
                        delay(400)
                        splashScreen.setKeepOnScreenCondition { false }
                    }
                    when (pinRequired) {
                        null -> Unit
                        true -> if (!unlocked) PinGateScreen { unlocked = true; sessionManager.markUnlocked() } else AppNavHost(navController)
                        false -> AppNavHost(navController)
                    }
                }
            }
        }
    }
}
