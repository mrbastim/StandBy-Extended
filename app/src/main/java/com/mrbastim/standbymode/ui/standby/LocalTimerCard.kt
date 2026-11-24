package com.mrbastim.standbymode.ui.standby

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
) {
    val context = LocalContext.current
    val store = remember(context.applicationContext) { LocalTimerStore.get(context) }
    val sessions by store.state.collectAsState()
    val session = sessions.getValue(mode)
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(mode, session) {
        now = SystemClock.elapsedRealtime()
        while (session.running(mode, now)) {
            delay(250L)
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

    val timeAndLabel: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = theme.secondary, fontSize = if (compact) 11.sp else 14.sp, textAlign = TextAlign.Center)
            Text(
                value,
                color = if (finished) theme.accent else theme.clockPrimary,
                fontSize = if (compact) 24.sp else 46.sp,
                fontWeight = FontWeight.Light,
                style = TextStyle(fontFeatureSettings = "tnum"),
                maxLines = 1,
            )
            if (finished) {
                Text(stringResource(R.string.timer_finished), color = theme.accent, fontSize = 11.sp)
            }
        }
    }
    val controls: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = { store.update(mode) { current, tick -> current.primaryAction(mode, tick) } }) {
                Text(primaryLabel, color = theme.accent, fontSize = if (compact) 12.sp else 14.sp)
            }
            TextButton(onClick = { store.update(mode) { current, _ -> current.reset(mode) } }) {
                Text(stringResource(R.string.timer_reset), color = theme.secondary, fontSize = if (compact) 12.sp else 14.sp)
            }
        }
    }
    val adjustment: @Composable () -> Unit = {
        if (canAdjust) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { store.update(mode) { current, _ -> current.adjustTimer(-1) } }) {
                    Text(stringResource(R.string.timer_minus_minute), color = theme.secondary)
                }
                TextButton(onClick = { store.update(mode) { current, _ -> current.adjustTimer(1) } }) {
                    Text(stringResource(R.string.timer_plus_minute), color = theme.secondary)
                }
            }
        }
    }
    if (compact) {
        Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                timeAndLabel()
                controls()
            }
            adjustment()
        }
    } else {
        BoxWithConstraints(modifier = modifier.padding(8.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                timeAndLabel()
                controls()
                adjustment()
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
