package com.mrbastim.standbymode.ui.standby

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal enum class TimerMode { TIMER, POMODORO, STOPWATCH }

private const val MINUTE = 60_000L

/** All elapsed values use elapsedRealtime, including time spent outside the StandBy UI. */
internal data class TimerSession(
    val durationMillis: Long,
    val accumulatedMillis: Long = 0L,
    val startedAtMillis: Long = -1L,
    val breakPhase: Boolean = false,
) {
    fun elapsed(now: Long): Long =
        (accumulatedMillis + if (startedAtMillis >= 0L) (now - startedAtMillis).coerceAtLeast(0L) else 0L)
            .coerceAtLeast(0L)

    fun finished(mode: TimerMode, now: Long): Boolean =
        mode != TimerMode.STOPWATCH && elapsed(now) >= durationMillis

    fun running(mode: TimerMode, now: Long): Boolean = startedAtMillis >= 0L && !finished(mode, now)

    fun displayedMillis(mode: TimerMode, now: Long): Long =
        if (mode == TimerMode.STOPWATCH) elapsed(now) else (durationMillis - elapsed(now)).coerceAtLeast(0L)

    fun primaryAction(mode: TimerMode, now: Long): TimerSession = when {
        finished(mode, now) && mode == TimerMode.POMODORO -> {
            val nextBreak = !breakPhase
            TimerSession(
                durationMillis = if (nextBreak) 5L * MINUTE else 25L * MINUTE,
                startedAtMillis = now,
                breakPhase = nextBreak,
            )
        }
        finished(mode, now) -> copy(accumulatedMillis = 0L, startedAtMillis = now)
        running(mode, now) -> copy(accumulatedMillis = elapsed(now), startedAtMillis = -1L)
        else -> copy(startedAtMillis = now)
    }

    fun reset(mode: TimerMode): TimerSession =
        if (mode == TimerMode.POMODORO) initial(mode) else copy(accumulatedMillis = 0L, startedAtMillis = -1L)

    fun adjustTimer(minutes: Int): TimerSession =
        if (startedAtMillis >= 0L || accumulatedMillis != 0L) this
        else copy(durationMillis = (durationMillis + minutes * MINUTE).coerceIn(MINUTE, 180L * MINUTE))

    companion object {
        fun initial(mode: TimerMode): TimerSession = TimerSession(
            durationMillis = when (mode) {
                TimerMode.TIMER -> 5L * MINUTE
                TimerMode.POMODORO -> 25L * MINUTE
                TimerMode.STOPWATCH -> 0L
            },
        )
    }
}

/** One process-wide store, shared by Activity and DreamService, persisted locally.
 * A device reboot clears running sessions because elapsedRealtime restarts.
 */
internal class LocalTimerStore private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("standby_local_timers", Context.MODE_PRIVATE)
    private val bootCount = runCatching {
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
    }.getOrDefault(-1)
    private val sessions = MutableStateFlow(TimerMode.entries.associateWith { mode ->
        val initial = TimerSession.initial(mode)
        val prefix = mode.name
        val duration = when (mode) {
            TimerMode.TIMER -> prefs.getLong("${prefix}_duration", initial.durationMillis).coerceIn(MINUTE, 180L * MINUTE)
            else -> initial.durationMillis
        }
        val now = SystemClock.elapsedRealtime()
        val startedAt = prefs.getLong("${prefix}_start", -1L)
        val sameBoot = bootCount < 0 || prefs.getInt("boot_count", -2) == bootCount
        if (!sameBoot || startedAt > now) initial.copy(durationMillis = duration)
        else {
            val isBreak = mode == TimerMode.POMODORO && prefs.getBoolean("${prefix}_break", false)
            TimerSession(
                durationMillis = if (isBreak) 5L * MINUTE else duration,
                accumulatedMillis = prefs.getLong("${prefix}_elapsed", 0L).coerceAtLeast(0L),
                startedAtMillis = startedAt,
                breakPhase = isBreak,
            )
        }
    })
    val state: StateFlow<Map<TimerMode, TimerSession>> = sessions

    init {
        persist(sessions.value)
    }

    @Synchronized
    fun update(mode: TimerMode, change: (TimerSession, Long) -> TimerSession) {
        val old = sessions.value
        val next = old + (mode to change(old.getValue(mode), SystemClock.elapsedRealtime()))
        sessions.value = next
        persist(next)
    }

    private fun persist(values: Map<TimerMode, TimerSession>) {
        val edit = prefs.edit().putInt("boot_count", bootCount)
        values.forEach { (mode, value) ->
            val prefix = mode.name
            edit.putLong("${prefix}_duration", value.durationMillis)
                .putLong("${prefix}_elapsed", value.accumulatedMillis)
                .putLong("${prefix}_start", value.startedAtMillis)
                .putBoolean("${prefix}_break", value.breakPhase)
        }
        edit.apply()
    }

    companion object {
        @Volatile private var instance: LocalTimerStore? = null
        fun get(context: Context): LocalTimerStore = instance ?: synchronized(this) {
            instance ?: LocalTimerStore(context.applicationContext).also { instance = it }
        }
    }
}
