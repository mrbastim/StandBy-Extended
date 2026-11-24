package com.mrbastim.standbymode.ui.standby

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import android.os.SystemClock
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import com.mrbastim.standbymode.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.mrbastim.standbymode.data.settings.StandBySettings
import com.mrbastim.standbymode.data.weather.WeatherKind
import com.mrbastim.standbymode.data.settings.WidgetColumn
import com.mrbastim.standbymode.system.ChargingState
import com.mrbastim.standbymode.system.NextAlarmState
import com.mrbastim.standbymode.data.weather.WeatherState
import com.mrbastim.standbymode.ui.StandByUiState
import kotlinx.coroutines.delay
import java.time.LocalDateTime

@Composable
fun StandByRoot(
    state: StandByUiState,
    actions: StandByActions,
    ambient: Boolean = false,
    automaticNightMode: Boolean = false,
    settingsContent: @Composable () -> Unit,
) {
    AnimatedContent(
        targetState = state.isStandByActive,
        transitionSpec = { fadeIn(tween(420)) togetherWith fadeOut(tween(260)) },
        label = "standby-mode",
    ) { active ->
        if (active) {
            StandByDisplay(state.settings, state.charging, state.nextAlarm, state.weather, actions, ambient, automaticNightMode)
        } else {
            settingsContent()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StandByDisplay(
    settings: StandBySettings,
    charging: ChargingState,
    nextAlarm: NextAlarmState = NextAlarmState(),
    weather: WeatherState = WeatherState(),
    actions: StandByActions = StandByActions(),
    ambient: Boolean = false,
    automaticNightMode: Boolean = false,
) {
    val displaySettings = standByPresentationSettings(settings, ambient, automaticNightMode)
    androidx.compose.runtime.CompositionLocalProvider(LocalAmbientMode provides ambient) {
        StandByThemeProvider(displaySettings) {
            ThemedBackground(
                settings = displaySettings,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                StandByDisplayContent(displaySettings, charging, nextAlarm, weather, actions)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StandByDisplayContent(
    settings: StandBySettings,
    charging: ChargingState,
    nextAlarm: NextAlarmState,
    weather: WeatherState,
    actions: StandByActions,
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    var editingStack by remember { mutableStateOf<WidgetColumn?>(null) }
    var editingClock by remember { mutableStateOf(false) }
    var indicatorVisible by remember { mutableStateOf(false) }
    val theme = LocalStandByTheme.current
    var lastInteraction by remember { mutableLongStateOf(0L) }
    var activityExitVisible by remember { mutableStateOf(false) }
    val canExitStandBy = actions.onExitStandBy != null

    LaunchedEffect(lastInteraction, canExitStandBy) {
        if (canExitStandBy && lastInteraction != 0L) {
            activityExitVisible = true
            delay(5_000L)
        }
        activityExitVisible = false
    }

    LaunchedEffect(pagerState.isScrollInProgress, pagerState.currentPage) {
        indicatorVisible = true
        if (!pagerState.isScrollInProgress) {
            delay(1_100L)
            indicatorVisible = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().pointerInput(canExitStandBy) {
            if (canExitStandBy) awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.any { it.pressed || it.previousPressed }) {
                        lastInteraction = SystemClock.uptimeMillis()
                    }
                }
            }
        },
    ) {
        BurnInContainer(settings.burnInProtection && editingStack == null && !editingClock) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = editingStack == null && !editingClock,
            ) { page ->
                when (page) {
                    0 -> DualWidgetPage(settings, charging, nextAlarm, weather) {
                        editingStack = it
                    }
                    1 -> FullClockPage(settings, nextAlarm) { editingClock = true }
                    2 -> CalendarPage(nextAlarm)
                    else -> BatteryPage(settings, charging)
                }
            }
        }

        AnimatedVisibility(
            visible = indicatorVisible && editingStack == null && !editingClock,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(380)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(4) { index ->
                    Box(
                        Modifier
                            .size(if (index == pagerState.currentPage) 6.dp else 4.dp)
                            .alpha(if (index == pagerState.currentPage) 0.7f else 0.24f)
                            .background(theme.accent, CircleShape)
                    )
                }
            }
        }

        actions.onExitPreview?.let { exit ->
            val exitPreviewLabel = stringResource(R.string.exit_preview)
            Text(
                text = "×",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .minimumInteractiveComponentSize()
                    .semantics { contentDescription = exitPreviewLabel }
                    .clickable(role = Role.Button, onClick = exit)
                    .padding(14.dp),
                color = theme.secondary.copy(alpha = 0.55f),
                fontSize = 26.sp,
            )
        }

        val exit = actions.onDismissDream ?: actions.onExitStandBy?.takeIf { activityExitVisible }
        exit?.let { dismiss ->
            StandByExitButton(
                onClick = dismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                background = theme.background,
                foreground = theme.primary,
                label = stringResource(
                    if (actions.onDismissDream != null) R.string.exit_dream_label
                    else R.string.exit_standby_label,
                ),
            )
        }

        AnimatedVisibility(
            visible = editingStack != null,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180)),
        ) {
            editingStack?.let { column ->
                StackEditor(
                    selectedColumn = column,
                    settings = settings,
                    onToggleWidget = actions.onToggleWidget ?: { _, _ -> },
                    onMoveWidget = actions.onMoveWidget ?: { _, _, _ -> },
                    onClockStyleChanged = actions.onClockWidgetStyleChanged ?: {},
                    onDateStyleChanged = actions.onDateWidgetStyleChanged ?: {},
                    onBatteryStyleChanged = actions.onBatteryWidgetStyleChanged ?: {},
                    onSelectColumn = { editingStack = it },
                    onDone = { editingStack = null },
                )
            }
        }

        AnimatedVisibility(
            visible = editingClock,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180)),
        ) {
            ClockCustomizer(
                settings = settings,
                actions = actions,
                onDone = { editingClock = false },
            )
        }
    }
}

@Composable
internal fun StandByExitButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    foreground: Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
    label: String = stringResource(R.string.exit_dream_label),
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            // Leave space for system indicators drawn over the dream window.
            .windowInsetsPadding(WindowInsets(top = 36.dp))
            .semantics { contentDescription = label },
        colors = ButtonDefaults.textButtonColors(
            containerColor = background.copy(alpha = 0.82f),
            contentColor = foreground,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.size(12.dp)) {
                val inset = 2.dp.toPx()
                val stroke = 1.5.dp.toPx()
                drawLine(foreground, Offset(inset, inset), Offset(size.width - inset, size.height - inset),
                    stroke, StrokeCap.Round)
                drawLine(foreground, Offset(size.width - inset, inset), Offset(inset, size.height - inset),
                    stroke, StrokeCap.Round)
            }
            Text(stringResource(R.string.exit_dream), modifier = Modifier.clearAndSetSemantics {})
        }
    }
}

@Preview(
    name = "Экран ожидания",
    locale = "ru",
    device = "spec:width=915dp,height=412dp,dpi=240",
)
@Composable
private fun StandByDisplayPreview() {
    StandByDisplay(
        settings = StandBySettings(weatherEnabled = true),
        charging = ChargingState(
            isCharging = true,
            batteryPercent = 82,
            source = com.mrbastim.standbymode.system.ChargingSource.AC,
        ),
        nextAlarm = NextAlarmState(
            triggerAtMillis = System.currentTimeMillis() + 3_600_000L,
        ),
        weather = WeatherState(
            enabled = true,
            temperatureCelsius = 18.0,
            apparentTemperatureCelsius = 17.0,
            windSpeedKmh = 12.0,
            kind = WeatherKind.PARTLY_CLOUDY,
            isDay = true,
        ),
    )
}
