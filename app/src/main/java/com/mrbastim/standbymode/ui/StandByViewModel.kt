package com.mrbastim.standbymode.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrbastim.standbymode.data.settings.ScreenTimeout
import com.mrbastim.standbymode.data.settings.ClockAccessory
import com.mrbastim.standbymode.data.settings.ClockColor
import com.mrbastim.standbymode.data.settings.ClockFace
import com.mrbastim.standbymode.data.settings.BackgroundStyle
import com.mrbastim.standbymode.data.settings.BatteryWidgetStyle
import com.mrbastim.standbymode.data.settings.ClockWidgetStyle
import com.mrbastim.standbymode.data.settings.DateWidgetStyle
import com.mrbastim.standbymode.data.settings.StandByThemeId
import com.mrbastim.standbymode.data.settings.TypographyStyle
import com.mrbastim.standbymode.data.settings.SettingsRepository
import com.mrbastim.standbymode.data.settings.StandBySettings
import com.mrbastim.standbymode.data.settings.StandByWidgetType
import com.mrbastim.standbymode.data.settings.WidgetColumn
import com.mrbastim.standbymode.data.settings.WorldClockZone
import com.mrbastim.standbymode.data.weather.WeatherRepository
import com.mrbastim.standbymode.data.weather.WeatherState
import com.mrbastim.standbymode.system.ChargingState
import com.mrbastim.standbymode.system.SystemStateRepository
import com.mrbastim.standbymode.system.NextAlarmState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StandByUiState(
    val settings: StandBySettings = StandBySettings(),
    val charging: ChargingState = ChargingState(),
    val isLandscape: Boolean = false,
    val nextAlarm: NextAlarmState = NextAlarmState(),
    val weather: WeatherState = WeatherState(),
    val isStandByActive: Boolean = false,
    val isPreviewMode: Boolean = false,
)

internal fun shouldActivateStandBy(
    automaticModeEnabled: Boolean,
    isCharging: Boolean,
    isLandscape: Boolean,
    isPreviewMode: Boolean,
    automaticModeDismissed: Boolean = false,
): Boolean = isPreviewMode ||
    (automaticModeEnabled && isCharging && isLandscape && !automaticModeDismissed)

class StandByViewModel(
    private val settingsRepository: SettingsRepository,
    systemStateRepository: SystemStateRepository,
    weatherRepository: WeatherRepository,
) : ViewModel() {
    private val previewMode = MutableStateFlow(false)
    private val automaticModeDismissed = MutableStateFlow(false)
    private val systemSources = MutableStateFlow(systemStateRepository)

    // Activity-owned observers must be replaced when the retained ViewModel gets a new Activity.
    fun bindSystemState(repository: SystemStateRepository) { systemSources.value = repository }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val systemState = systemSources.flatMapLatest { repository ->
        combine(repository.charging, repository.isLandscape, repository.nextAlarm) { charging, landscape, alarm ->
            Triple(charging, landscape, alarm)
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val weatherState = settingsRepository.settings
        .map { it.weatherEnabled to it.worldClockZone }
        .distinctUntilChanged()
        .flatMapLatest { (enabled, location) -> weatherRepository.observe(enabled, location) }

    private val baseState = combine(
        settingsRepository.settings,
        systemState,
        weatherState,
    ) { settings, system, weather ->
        val (charging, isLandscape, nextAlarm) = system
        StandByUiState(
            settings = settings,
            charging = charging,
            isLandscape = isLandscape,
            nextAlarm = nextAlarm,
            weather = weather,
        )
    }

    // A new charging/landscape session can activate again after the user leaves StandBy.
    private val activationState = baseState.onEach { state ->
        val automaticActivationAllowed = shouldActivateStandBy(
            automaticModeEnabled = state.settings.automaticModeEnabled,
            isCharging = state.charging.isCharging,
            isLandscape = state.isLandscape,
            isPreviewMode = false,
        )
        if (!automaticActivationAllowed) {
            automaticModeDismissed.value = false
        }
    }

    val uiState: StateFlow<StandByUiState> = combine(
        activationState, previewMode, automaticModeDismissed,
    ) { state, isPreviewMode, dismissed ->
        state.copy(
            isStandByActive = shouldActivateStandBy(
                automaticModeEnabled = state.settings.automaticModeEnabled,
                isCharging = state.charging.isCharging,
                isLandscape = state.isLandscape,
                isPreviewMode = isPreviewMode,
                automaticModeDismissed = dismissed,
            ),
            isPreviewMode = isPreviewMode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StandByUiState())

    fun setDisplayProtection(value: com.mrbastim.standbymode.data.settings.DisplayProtectionSettings) =
        update { setDisplayProtection(value) }
    fun setAutomaticModeEnabled(value: Boolean) = update { setAutomaticModeEnabled(value) }
    fun setUse24HourClock(value: Boolean) = update { setUse24HourClock(value) }
    fun setShowSeconds(value: Boolean) = update { setShowSeconds(value) }
    fun setKeepScreenAwake(value: Boolean) = update { setKeepScreenAwake(value) }
    fun setClockFace(value: ClockFace) = update { setClockFace(value) }
    fun setClockColor(value: ClockColor) = update { setClockColor(value) }
    fun setShowDate(value: Boolean) = update { setShowDate(value) }
    fun setShowNextAlarm(value: Boolean) = update { setShowNextAlarm(value) }
    fun setClockAccessory(value: ClockAccessory) = update { setClockAccessory(value) }
    fun setNightMode(value: Boolean) = update { setNightMode(value) }
    fun setBurnInProtection(value: Boolean) = update { setBurnInProtection(value) }
    fun setDayTheme(value: StandByThemeId) = update { setDayTheme(value) }
    fun setNightTheme(value: StandByThemeId) = update { setNightTheme(value) }
    fun setBackgroundStyle(value: BackgroundStyle) = update { setBackgroundStyle(value) }
    fun setTypographyStyle(value: TypographyStyle) = update { setTypographyStyle(value) }
    fun setClockWidgetStyle(value: ClockWidgetStyle) = update { setClockWidgetStyle(value) }
    fun setDateWidgetStyle(value: DateWidgetStyle) = update { setDateWidgetStyle(value) }
    fun setBatteryWidgetStyle(value: BatteryWidgetStyle) = update { setBatteryWidgetStyle(value) }
    fun setWorldClockZone(value: WorldClockZone) = update { setWorldClockZone(value) }
    fun setWeatherEnabled(value: Boolean) = update { setWeatherEnabled(value) }
    fun setCustomColors(background: Long, primary: Long, secondary: Long, accent: Long) =
        update { setCustomColors(background, primary, secondary, accent) }
    fun setScreenTimeout(value: ScreenTimeout) = update { setScreenTimeout(value) }
    fun startPreview() { previewMode.value = true }
    fun stopPreview() { previewMode.value = false }
    fun dismissStandBy() { automaticModeDismissed.value = true }
    fun toggleWidget(column: WidgetColumn, widget: StandByWidgetType) =
        update { toggleWidget(column, widget) }
    fun rememberActiveWidget(column: WidgetColumn, widget: StandByWidgetType) =
        settingsRepository.rememberActiveWidget(column, widget)
    fun moveWidget(column: WidgetColumn, fromIndex: Int, toIndex: Int) =
        update { moveWidget(column, fromIndex, toIndex) }

    private fun update(block: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { settingsRepository.block() }
    }
}
