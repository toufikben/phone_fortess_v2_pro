package com.phonefortress.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.phonefortress.app.ui.screens.AlertSettingsScreen
import com.phonefortress.app.ui.screens.DiagnosticsScreen
import com.phonefortress.app.ui.screens.EventDetailsScreen
import com.phonefortress.app.ui.screens.GeofenceScreen
import com.phonefortress.app.ui.screens.HomeScreen
import com.phonefortress.app.ui.screens.LogsScreen
import com.phonefortress.app.ui.screens.LanguagePickerScreen
import com.phonefortress.app.ui.screens.SettingsScreen
import com.phonefortress.app.ui.screens.ThemePickerScreen
import com.phonefortress.app.ui.screens.pin.PinSetupScreen

object Routes {
    const val HOME = "home"
    const val LOGS = "logs"
    const val ZONES = "zones"
    const val CHANNELS = "channels"
    const val SETTINGS = "settings"
    const val THEME_PICKER = "theme_picker"
    const val PIN_SETUP = "pin_setup"
    const val DIAGNOSTICS = "diagnostics"
    const val LANGUAGE_PICKER = "language_picker"
    const val EVENT_DETAILS = "event/{eventId}"
    fun eventDetails(eventId: String) = "event/${android.net.Uri.encode(eventId)}"
}

@Composable
fun AppNavHost(navController: NavHostController, startDestination: String = Routes.HOME) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToLogs = { navController.navigate(Routes.LOGS) { launchSingleTop = true } },
                onNavigateToZones = { navController.navigate(Routes.ZONES) { launchSingleTop = true } },
                onNavigateToChannels = { navController.navigate(Routes.CHANNELS) { launchSingleTop = true } },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } }
            )
        }
        composable(Routes.LOGS) { LogsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.EVENT_DETAILS) { EventDetailsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.ZONES) { GeofenceScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.CHANNELS) { AlertSettingsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onThemePicker = { navController.navigate(Routes.THEME_PICKER) { launchSingleTop = true } },
                onPinSetup = { navController.navigate(Routes.PIN_SETUP) { launchSingleTop = true } },
                onNavigateToDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) { launchSingleTop = true } },
                onNavigateToLanguage = { navController.navigate(Routes.LANGUAGE_PICKER) { launchSingleTop = true } }
            )
        }
        composable(Routes.THEME_PICKER) { ThemePickerScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.PIN_SETUP) { PinSetupScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.DIAGNOSTICS) { DiagnosticsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.LANGUAGE_PICKER) { LanguagePickerScreen(onBack = { navController.popBackStack() }) }
    }
}
