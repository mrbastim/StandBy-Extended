package com.mrbastim.standbymode

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mrbastim.standbymode.data.settings.SettingsRepository
import com.mrbastim.standbymode.data.weather.WeatherRepository
import com.mrbastim.standbymode.system.ChargingObserver
import com.mrbastim.standbymode.system.OrientationObserver
import com.mrbastim.standbymode.system.NextAlarmObserver
import com.mrbastim.standbymode.system.SystemStateRepository
import com.mrbastim.standbymode.ui.SettingsScreen
import com.mrbastim.standbymode.ui.StandByViewModel
import com.mrbastim.standbymode.ui.standby.StandByActions
import com.mrbastim.standbymode.ui.standby.StandByRoot
import com.mrbastim.standbymode.ui.theme.StandByTheme
import com.mrbastim.standbymode.display.DisplayProtectionHost
import com.mrbastim.standbymode.display.DisplayPhase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val applicationContext = applicationContext
        val settingsRepository = SettingsRepository(applicationContext)
        val systemStateRepository = SystemStateRepository(
            chargingObserver = ChargingObserver(applicationContext),
            orientationObserver = OrientationObserver(this),
            nextAlarmObserver = NextAlarmObserver(applicationContext),
            scope = lifecycleScope,
        )
        val viewModel = ViewModelProvider(
            this,
            StandByViewModelFactory(
                settingsRepository,
                systemStateRepository,
                WeatherRepository(),
            ),
        )[StandByViewModel::class.java]
        viewModel.bindSystemState(systemStateRepository)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            StandByTheme {
                StandByWindowBehavior(
                    active = state.isStandByActive,
                )
                DisplayProtectionHost(window, state.isStandByActive, state.settings) { phase, automaticNightMode ->
                    StandByRoot(
                        state = state,
                        automaticNightMode = automaticNightMode,
                        ambient = state.settings.displayProtection.enabled &&
                            (phase == DisplayPhase.AMBIENT || phase == DisplayPhase.RELEASED),
                        actions = StandByActions(
                            onExitPreview = if (state.isPreviewMode) viewModel::stopPreview else null,
                            onExitStandBy = if (!state.isPreviewMode) viewModel::dismissStandBy else null,
                            onToggleWidget = viewModel::toggleWidget,
                            onMoveWidget = viewModel::moveWidget,
                            onClockFaceChanged = viewModel::setClockFace,
                            onClockColorChanged = viewModel::setClockColor,
                            onShowSecondsChanged = viewModel::setShowSeconds,
                            on24HourChanged = viewModel::setUse24HourClock,
                            onShowDateChanged = viewModel::setShowDate,
                            onClockAccessoryChanged = viewModel::setClockAccessory,
                            onClockAccessorySideChanged = viewModel::setClockAccessorySide,
                            onDayThemeChanged = viewModel::setDayTheme,
                            onNightThemeChanged = viewModel::setNightTheme,
                            onBackgroundStyleChanged = viewModel::setBackgroundStyle,
                            onTypographyStyleChanged = viewModel::setTypographyStyle,
                            onCustomColorsChanged = viewModel::setCustomColors,
                            onClockWidgetStyleChanged = viewModel::setClockWidgetStyle,
                            onDateWidgetStyleChanged = viewModel::setDateWidgetStyle,
                            onBatteryWidgetStyleChanged = viewModel::setBatteryWidgetStyle,
                        ),
                    ) {
                        SettingsScreen(
                            state = state,
                            onDisplayProtectionChanged = viewModel::setDisplayProtection,
                            onAutomaticModeChanged = viewModel::setAutomaticModeEnabled,
                            on24HourChanged = viewModel::setUse24HourClock,
                            onShowSecondsChanged = viewModel::setShowSeconds,
                            onScreenTimeoutChanged = viewModel::setScreenTimeout,
                            onKeepAwakeChanged = viewModel::setKeepScreenAwake,
                            onClockFaceChanged = viewModel::setClockFace,
                            onClockColorChanged = viewModel::setClockColor,
                            onShowDateChanged = viewModel::setShowDate,
                            onShowNextAlarmChanged = viewModel::setShowNextAlarm,
                            onClockAccessoryChanged = viewModel::setClockAccessory,
                            onClockAccessorySideChanged = viewModel::setClockAccessorySide,
                            onNightModeChanged = viewModel::setNightMode,
                            onBurnInProtectionChanged = viewModel::setBurnInProtection,
                            onDayThemeChanged = viewModel::setDayTheme,
                            onNightThemeChanged = viewModel::setNightTheme,
                            onBackgroundStyleChanged = viewModel::setBackgroundStyle,
                            onTypographyStyleChanged = viewModel::setTypographyStyle,
                            onClockWidgetStyleChanged = viewModel::setClockWidgetStyle,
                            onDateWidgetStyleChanged = viewModel::setDateWidgetStyle,
                            onBatteryWidgetStyleChanged = viewModel::setBatteryWidgetStyle,
                            onWorldClockZoneChanged = viewModel::setWorldClockZone,
                            onWeatherEnabledChanged = viewModel::setWeatherEnabled,
                            onPreview = viewModel::startPreview,
                            onOpenScreenSaverSettings = ::openScreenSaverSettings,
                        )
                    }
                }
            }
        }
    }

    private fun openScreenSaverSettings() {
        try {
            startActivity(Intent(Settings.ACTION_DREAM_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                this,
                getString(R.string.screen_saver_settings_unavailable),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    @androidx.compose.runtime.Composable
    private fun StandByWindowBehavior(
        active: Boolean,
    ) {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        LaunchedEffect(active) {
            setShowWhenLocked(active)
            setTurnScreenOn(active)
            WindowCompat.setDecorFitsSystemWindows(window, !active)
            if (active) {
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                setShowWhenLocked(false)
                setTurnScreenOn(false)
                WindowCompat.setDecorFitsSystemWindows(window, true)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

private class StandByViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val systemStateRepository: SystemStateRepository,
    private val weatherRepository: WeatherRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(StandByViewModel::class.java))
        return StandByViewModel(
            settingsRepository,
            systemStateRepository,
            weatherRepository,
        ) as T
    }
}
