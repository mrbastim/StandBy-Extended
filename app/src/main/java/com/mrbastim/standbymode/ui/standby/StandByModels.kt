package com.mrbastim.standbymode.ui.standby

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mrbastim.standbymode.data.settings.ClockAccessory
import com.mrbastim.standbymode.data.settings.ClockAccessorySide
import com.mrbastim.standbymode.data.settings.ClockFace
import com.mrbastim.standbymode.data.settings.BackgroundStyle
import com.mrbastim.standbymode.data.settings.BatteryWidgetStyle
import com.mrbastim.standbymode.data.settings.ClockColor
import com.mrbastim.standbymode.data.settings.ClockWidgetStyle
import com.mrbastim.standbymode.data.settings.DateWidgetStyle
import com.mrbastim.standbymode.data.settings.StandBySettings
import com.mrbastim.standbymode.data.settings.StandByThemeId
import com.mrbastim.standbymode.data.settings.StandByWidgetType
import com.mrbastim.standbymode.data.settings.TypographyStyle
import com.mrbastim.standbymode.data.settings.WidgetColumn
import java.time.LocalDateTime
import kotlinx.coroutines.delay

val LocalAmbientMode = androidx.compose.runtime.staticCompositionLocalOf { false }

data class StandByActions(
    val onExitPreview: (() -> Unit)? = null,
    val onToggleWidget: ((WidgetColumn, StandByWidgetType) -> Unit)? = null,
    val onMoveWidget: ((WidgetColumn, Int, Int) -> Unit)? = null,
    val onClockFaceChanged: ((ClockFace) -> Unit)? = null,
    val onClockColorChanged: ((ClockColor) -> Unit)? = null,
    val onShowSecondsChanged: ((Boolean) -> Unit)? = null,
    val on24HourChanged: ((Boolean) -> Unit)? = null,
    val onShowDateChanged: ((Boolean) -> Unit)? = null,
    val onClockAccessoryChanged: ((ClockAccessory) -> Unit)? = null,
    val onClockAccessorySideChanged: ((ClockAccessorySide) -> Unit)? = null,
    val onDayThemeChanged: ((StandByThemeId) -> Unit)? = null,
    val onNightThemeChanged: ((StandByThemeId) -> Unit)? = null,
    val onBackgroundStyleChanged: ((BackgroundStyle) -> Unit)? = null,
    val onTypographyStyleChanged: ((TypographyStyle) -> Unit)? = null,
    val onCustomColorsChanged: ((Long, Long, Long, Long) -> Unit)? = null,
    val onClockWidgetStyleChanged: ((ClockWidgetStyle) -> Unit)? = null,
    val onDateWidgetStyleChanged: ((DateWidgetStyle) -> Unit)? = null,
    val onBatteryWidgetStyleChanged: ((BatteryWidgetStyle) -> Unit)? = null,
    val onDismissDream: (() -> Unit)? = null,
    val onExitStandBy: (() -> Unit)? = null,
)

@Composable
fun rememberStandByTime(showSeconds: Boolean, smooth: Boolean = false): LocalDateTime {
    var time by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(showSeconds, smooth) {
        while (true) {
            val nowMillis = System.currentTimeMillis()
            val wait = when {
                smooth && showSeconds -> 200L
                showSeconds -> 1_000L - nowMillis % 1_000L
                else -> 60_000L - nowMillis % 60_000L
            }
            delay(wait)
            time = LocalDateTime.now()
        }
    }
    return time
}

@Composable
fun BurnInContainer(enabled: Boolean, content: @Composable () -> Unit) {
    var position by remember { mutableIntStateOf(0) }
    val pixels = with(LocalDensity.current) { 3.dp.roundToPx() }
    val offsets = remember(pixels) {
        listOf(
            IntOffset(0, 0),
            IntOffset(pixels, -pixels),
            IntOffset(-pixels, pixels),
            IntOffset(pixels, pixels),
            IntOffset(-pixels, -pixels),
        )
    }
    LaunchedEffect(enabled) {
        position = 0
        while (enabled) {
            delay(60_000L)
            position = (position + 1) % offsets.size
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (enabled) 3.dp else 0.dp)
            .offset { if (enabled) offsets[position] else IntOffset.Zero },
    ) {
        content()
    }
}
