package com.mrbastim.standbymode

import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.view.KeyEvent
import android.view.MotionEvent
import com.mrbastim.standbymode.display.DisplayProtectionHost
import com.mrbastim.standbymode.display.DisplayPhase
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.mrbastim.standbymode.data.settings.StandByWidgetType
import com.mrbastim.standbymode.data.settings.WidgetColumn
import com.mrbastim.standbymode.data.settings.SettingsRepository
import com.mrbastim.standbymode.data.settings.StandBySettings
import com.mrbastim.standbymode.data.weather.WeatherRepository
import com.mrbastim.standbymode.data.weather.WeatherState
import com.mrbastim.standbymode.system.ChargingObserver
import com.mrbastim.standbymode.system.ChargingState
import com.mrbastim.standbymode.system.NextAlarmObserver
import com.mrbastim.standbymode.system.NextAlarmState
import com.mrbastim.standbymode.ui.standby.StandByExitButton
import com.mrbastim.standbymode.ui.standby.StandByActions
import com.mrbastim.standbymode.ui.standby.StandByDisplay
import com.mrbastim.standbymode.ui.theme.StandByTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class StandByDreamService : DreamService(), LifecycleOwner, ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val exitHandler = Handler(Looper.getMainLooper())
    private val hideExitButton = Runnable { exitButtonVisible = false }
    private var exitButtonVisible by mutableStateOf(false)
    private var endingDream = false

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    override fun onCreate() {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        super.onCreate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = true
        isFullscreen = true
        isScreenBright = false

        val settingsRepository = SettingsRepository(applicationContext)
        val chargingObserver = ChargingObserver(applicationContext)
        val nextAlarmObserver = NextAlarmObserver(applicationContext)
        val weatherRepository = WeatherRepository()
        val content = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@StandByDreamService)
            setViewTreeViewModelStoreOwner(this@StandByDreamService)
            setViewTreeSavedStateRegistryOwner(this@StandByDreamService)
            setContent {
                val settings by settingsRepository.settings.collectAsState(StandBySettings())
                val charging by chargingObserver.state.collectAsState(ChargingState())
                val nextAlarm by nextAlarmObserver.state.collectAsState(NextAlarmState())
                val weatherFlow = remember(settings.weatherEnabled, settings.worldClockZone) {
                    weatherRepository.observe(settings.weatherEnabled, settings.worldClockZone)
                }
                val weather by weatherFlow.collectAsState(WeatherState())
                val isLandscape = LocalConfiguration.current.orientation ==
                    Configuration.ORIENTATION_LANDSCAPE

                StandByTheme {
                    DisplayProtectionHost(
                        window = window,
                        active = true,
                        settings = settings,
                        onHoldChanged = { isScreenBright = it },
                        onRelease = {
                            if (beginEndingDream()) finish()
                        },
                    ) { phase, automaticNightMode ->
                        if (isLandscape) {
                            StandByDisplay(
                                settings = settings,
                                automaticNightMode = automaticNightMode,
                                charging = charging,
                                ambient = settings.displayProtection.enabled &&
                                    (phase == DisplayPhase.AMBIENT || phase == DisplayPhase.RELEASED),
                                nextAlarm = nextAlarm,
                                weather = weather,
                                actions = StandByActions(
                                    onDismissDream = if (exitButtonVisible) ::wakeUp else null,
                                    onToggleWidget = { column: WidgetColumn, widget: StandByWidgetType ->
                                        serviceScope.launch { settingsRepository.toggleWidget(column, widget) }
                                    },
                                    onMoveWidget = { column, from, to ->
                                        serviceScope.launch { settingsRepository.moveWidget(column, from, to) }
                                    },
                                    onClockFaceChanged = { face ->
                                        serviceScope.launch { settingsRepository.setClockFace(face) }
                                    },
                                    onClockColorChanged = { color ->
                                        serviceScope.launch { settingsRepository.setClockColor(color) }
                                    },
                                    onShowSecondsChanged = { value ->
                                        serviceScope.launch { settingsRepository.setShowSeconds(value) }
                                    },
                                    on24HourChanged = { value ->
                                        serviceScope.launch { settingsRepository.setUse24HourClock(value) }
                                    },
                                    onShowDateChanged = { value ->
                                        serviceScope.launch { settingsRepository.setShowDate(value) }
                                    },
                                    onDayThemeChanged = { value ->
                                        serviceScope.launch { settingsRepository.setDayTheme(value) }
                                    },
                                    onNightThemeChanged = { value ->
                                        serviceScope.launch { settingsRepository.setNightTheme(value) }
                                    },
                                    onBackgroundStyleChanged = { value ->
                                        serviceScope.launch { settingsRepository.setBackgroundStyle(value) }
                                    },
                                    onTypographyStyleChanged = { value ->
                                        serviceScope.launch { settingsRepository.setTypographyStyle(value) }
                                    },
                                    onCustomColorsChanged = { background, primary, secondary, accent ->
                                        serviceScope.launch {
                                            settingsRepository.setCustomColors(background, primary, secondary, accent)
                                        }
                                    },
                                    onClockWidgetStyleChanged = { value ->
                                        serviceScope.launch { settingsRepository.setClockWidgetStyle(value) }
                                    },
                                    onDateWidgetStyleChanged = { value ->
                                        serviceScope.launch { settingsRepository.setDateWidgetStyle(value) }
                                    },
                                    onBatteryWidgetStyleChanged = { value ->
                                        serviceScope.launch { settingsRepository.setBatteryWidgetStyle(value) }
                                    },
                                ),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                                    .windowInsetsPadding(WindowInsets.safeDrawing),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.rotate_to_landscape),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (exitButtonVisible) {
                                    StandByExitButton(
                                        onClick = ::wakeUp,
                                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        setContentView(content)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (!endingDream && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
            event.actionMasked != MotionEvent.ACTION_CANCEL
        ) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) exitButtonVisible = true
            if (exitButtonVisible) extendExitButtonTimeout()
        }
        return super.dispatchTouchEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (exitButtonVisible && event.keyCode != KeyEvent.KEYCODE_BACK) extendExitButtonTimeout()
        return super.dispatchKeyEvent(event)
    }

    private fun extendExitButtonTimeout() {
        exitHandler.removeCallbacks(hideExitButton)
        exitHandler.postDelayed(hideExitButton, 5_000L)
    }

    private fun clearExitButton() {
        exitHandler.removeCallbacks(hideExitButton)
        exitButtonVisible = false
    }

    private fun beginEndingDream(): Boolean {
        if (endingDream) return false
        endingDream = true
        clearExitButton()
        return true
    }

    override fun onWakeUp() {
        if (beginEndingDream()) super.onWakeUp()
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        endingDream = false
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDreamingStopped() {
        clearExitButton()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDreamingStopped()
    }

    override fun onDestroy() {
        clearExitButton()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
        super.onDestroy()
    }
}
