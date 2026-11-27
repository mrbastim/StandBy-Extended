package com.mrbastim.standbymode.ui.standby

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrbastim.standbymode.R
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
internal fun LocalTimerCard(
    mode: TimerMode,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    hideControls: Boolean = false,
) {
    val context = LocalContext.current
    val store = remember(context.applicationContext) { LocalTimerStore.get(context) }
    val sessions by store.state.collectAsState()
    val session = sessions.getValue(mode)
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(mode, session) {
        now = SystemClock.elapsedRealtime()
        while (session.running(mode, now)) {
            delay(1_000L - now % 1_000L)
            now = SystemClock.elapsedRealtime()
        }
    }
    val theme = LocalStandByTheme.current
    val finished = session.finished(mode, now)
    val running = session.running(mode, now)
    val title = stringResource(when (mode) {
        TimerMode.TIMER -> R.string.widget_timer
        TimerMode.POMODORO -> if (session.breakPhase) R.string.pomodoro_break else R.string.pomodoro_focus
        TimerMode.STOPWATCH -> R.string.widget_stopwatch
    })
    val value = timerText(session.displayedMillis(mode, now), mode != TimerMode.STOPWATCH)
    val primaryLabel = stringResource(when {
        finished && mode == TimerMode.POMODORO -> R.string.timer_next_phase
        running -> R.string.timer_pause
        else -> R.string.timer_start
    })
    val canAdjust = mode == TimerMode.TIMER && session.startedAtMillis < 0L && session.accumulatedMillis == 0L

    val adjustControls: @Composable () -> Unit = {
        TimerControl(TimerIcon.MINUS, stringResource(R.string.timer_minus_minute),
            enabled = canAdjust && session.durationMillis > 60_000L) {
            store.update(mode) { current, _ -> current.adjustTimer(-1) }
        }
        TimerControl(TimerIcon.PLUS, stringResource(R.string.timer_plus_minute),
            enabled = canAdjust && session.durationMillis < 10_800_000L) {
            store.update(mode) { current, _ -> current.adjustTimer(1) }
        }
    }
    val sessionControls: @Composable () -> Unit = {
        TimerControl(
            icon = when {
                finished && mode == TimerMode.POMODORO -> TimerIcon.NEXT
                running -> TimerIcon.PAUSE
                else -> TimerIcon.PLAY
            },
            label = primaryLabel,
            primary = true,
        ) { store.update(mode) { current, tick -> current.primaryAction(mode, tick) } }
        TimerControl(TimerIcon.RESET, stringResource(R.string.timer_reset),
            enabled = session.startedAtMillis >= 0L || session.accumulatedMillis > 0L || session.breakPhase) {
            store.update(mode) { current, _ -> current.reset(mode) }
        }
    }

    val label = if (finished) stringResource(R.string.timer_finished) else title
    BoxWithConstraints(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Preserve the usual centered single row and 48dp touch targets. Only a
        // narrow window falls back to two rows rather than clipping the buttons.
        val twoRows = mode == TimerMode.TIMER && maxWidth < 198.dp
        val fontScale = LocalDensity.current.fontScale
        val desiredSize = if (compact) 24f else 46f
        val fittedSize = minOf(desiredSize, maxWidth.value / (value.length * 0.64f * fontScale))
            .coerceAtLeast(1f)
        val arrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally)
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Text(label, color = theme.secondary, fontSize = 14.sp, textAlign = TextAlign.Center)
            Text(
                value, color = if (finished) theme.accent else theme.clockPrimary,
                fontSize = fittedSize.sp, fontWeight = FontWeight.Light,
                style = TextStyle(fontFeatureSettings = "tnum"), maxLines = 1,
            )
            if (hideControls) {
                Spacer(Modifier.height(if (twoRows) 98.dp else 48.dp))
            } else if (twoRows) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = arrangement) { adjustControls() }
                    Row(horizontalArrangement = arrangement) { sessionControls() }
                }
            } else {
                Row(horizontalArrangement = arrangement, verticalAlignment = Alignment.CenterVertically) {
                    if (mode == TimerMode.TIMER) adjustControls()
                    sessionControls()
                }
            }
        }
    }
}

private enum class TimerIcon { MINUS, PLUS, PLAY, PAUSE, RESET, NEXT }

@Composable
private fun TimerControl(
    icon: TimerIcon,
    label: String,
    enabled: Boolean = true,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    val theme = LocalStandByTheme.current
    val foreground = (if (primary) theme.background else theme.primary).copy(alpha = if (enabled) 1f else 0.32f)
    Box(
        modifier = Modifier.size(48.dp)
            .clip(CircleShape)
            .semantics { contentDescription = label }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(36.dp).background(
                if (primary) theme.primary else theme.separator.copy(alpha = if (enabled) 0.9f else 0.45f),
                CircleShape,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(18.dp)) {
                val w = size.width
                val h = size.height
                val stroke = 1.7.dp.toPx()
                fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
                    drawLine(foreground, Offset(x1*w,y1*h), Offset(x2*w,y2*h), stroke, StrokeCap.Round)
                when (icon) {
                    TimerIcon.MINUS -> line(.2f,.5f,.8f,.5f)
                    TimerIcon.PLUS -> { line(.2f,.5f,.8f,.5f); line(.5f,.2f,.5f,.8f) }
                    TimerIcon.PAUSE -> { line(.35f,.22f,.35f,.78f); line(.65f,.22f,.65f,.78f) }
                    TimerIcon.PLAY, TimerIcon.NEXT -> {
                        val triangle = Path().apply {
                            moveTo(w*.28f,h*.18f); lineTo(w*.76f,h*.5f); lineTo(w*.28f,h*.82f); close()
                        }
                        drawPath(triangle, foreground)
                        if (icon == TimerIcon.NEXT) line(.82f,.22f,.82f,.78f)
                    }
                    TimerIcon.RESET -> {
                        drawArc(foreground, -105f, 285f, false,
                            topLeft = Offset(w*.16f,h*.16f), size = Size(w*.68f,h*.68f),
                            style = Stroke(stroke, cap = StrokeCap.Round))
                        line(.43f,.04f,.68f,.17f)
                        line(.68f,.17f,.47f,.33f)
                    }
                }
            }
        }
    }
}

internal fun timerText(millis: Long, countdown: Boolean): String {
    val seconds = if (countdown) (millis.coerceAtLeast(0L) + 999L) / 1_000L else millis.coerceAtLeast(0L) / 1_000L
    val hours = seconds / 3600L
    return if (hours > 0L) String.format(Locale.ROOT, "%02d:%02d:%02d", hours, seconds / 60L % 60L, seconds % 60L)
    else String.format(Locale.ROOT, "%02d:%02d", seconds / 60L, seconds % 60L)
}
