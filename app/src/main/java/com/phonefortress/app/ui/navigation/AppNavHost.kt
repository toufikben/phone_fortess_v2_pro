package com.phonefortress.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.phonefortress.app.ui.screens.AlertSettingsScreen
import com.phonefortress.app.ui.screens.GeofenceScreen
import com.phonefortress.app.ui.screens.HomeScreen
import com.phonefortress.app.ui.screens.LogsScreen
import com.phonefortress.app.ui.screens.PinSetupScreen
import com.phonefortress.app.ui.screens.SettingsScreen
import com.phonefortress.app.ui.screens.ThemePickerScreen

object Routes {
    const val HOME = "home"
    const val LOGS = "logs"
    const val ZONES = "zones"
    const val CHANNELS = "channels"
    const val SETTINGS = "settings"
    const val THEME_PICKER = "theme_picker"
    const val PIN_SETUP = "pin_setup"
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToLogs = { navController.navigate(Routes.LOGS) },
                onNavigateToZones = { navController.navigate(Routes.ZONES) },
                onNavigateToChannels = { navController.navigate(Routes.CHANNELS) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.LOGS) { LogsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.ZONES) { GeofenceScreen() }
        composable(Routes.CHANNELS) { AlertSettingsScreen() }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onThemePicker = { navController.navigate(Routes.THEME_PICKER) },
                onPinSetup = { navController.navigate(Routes.PIN_SETUP) }
            )
        }
        composable(Routes.THEME_PICKER) { ThemePickerScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.PIN_SETUP) { PinSetupScreen(onBack = { navController.popBackStack() }) }
    }
}
