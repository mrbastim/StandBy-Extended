package com.mrbastim.standbymode.ui.standby

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalTimerTest {
    @Test fun countdownContinuesOutsideUiAndStopsAtZero() {
        val mode = TimerMode.TIMER
        val running = TimerSession.initial(mode).primaryAction(mode, 1_000L)
        assertEquals(240_000L, running.displayedMillis(mode, 61_000L))
        assertTrue(running.finished(mode, 301_000L))
        assertFalse(running.running(mode, 301_000L))
        assertEquals(0L, running.displayedMillis(mode, 999_000L))
    }

    @Test fun pauseResumeDoesNotCountPausedTime() {
        val mode = TimerMode.STOPWATCH
        val paused = TimerSession.initial(mode).primaryAction(mode, 1_000L).primaryAction(mode, 11_000L)
        assertEquals(10_000L, paused.displayedMillis(mode, 100_000L))
        val resumed = paused.primaryAction(mode, 100_000L)
        assertEquals(15_000L, resumed.displayedMillis(mode, 105_000L))
        assertEquals(0L, resumed.reset(mode).displayedMillis(mode, 105_000L))
    }

    @Test fun pomodoroTransitionsBetweenFocusAndBreak() {
        val mode = TimerMode.POMODORO
        val focus = TimerSession.initial(mode).primaryAction(mode, 100L)
        assertTrue(focus.finished(mode, 1_500_100L))
        val rest = focus.primaryAction(mode, 1_500_100L)
        assertTrue(rest.breakPhase)
        assertEquals(300_000L, rest.displayedMillis(mode, 1_500_100L))
        val next = rest.primaryAction(mode, 1_800_100L)
        assertFalse(next.breakPhase)
        assertEquals(1_500_000L, next.displayedMillis(mode, 1_800_100L))
    }

    @Test fun durationIsBoundedAndCannotChangeDuringActiveCountdown() {
        val mode = TimerMode.TIMER
        val initial = TimerSession.initial(mode)
        assertEquals(60_000L, initial.adjustTimer(-100).durationMillis)
        assertEquals(10_800_000L, initial.adjustTimer(1000).durationMillis)
        val active = initial.primaryAction(mode, 123L)
        assertEquals(active, active.adjustTimer(1))
    }

    @Test fun displayRoundingIsAppropriateForCountdownAndStopwatch() {
        assertEquals("01:00", timerText(59_001L, true))
        assertEquals("00:59", timerText(59_999L, false))
        assertEquals("01:00:00", timerText(3_600_000L, false))
    }
}
