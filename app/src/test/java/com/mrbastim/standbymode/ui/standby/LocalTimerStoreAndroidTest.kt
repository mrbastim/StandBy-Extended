package com.mrbastim.standbymode.ui.standby

import android.content.Context
import android.content.ContextWrapper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35])
class LocalTimerStoreAndroidTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val prefs get() = context.getSharedPreferences("timer_store_test", Context.MODE_PRIVATE)
    private var now = 1_000L

    @Before fun resetPreferences() {
        prefs.edit().clear().commit()
        now = 1_000L
    }

    private fun reopen(boot: Int = 7) = LocalTimerStore(prefs, boot) { now }

    @Test fun sessionStateRoundTripsAfterStoreRecreation() {
        val store = reopen()
        store.update(TimerMode.TIMER) { session, _ -> session.adjustTimer(2) }
        store.update(TimerMode.TIMER) { session, tick -> session.primaryAction(TimerMode.TIMER, tick) }
        store.update(TimerMode.STOPWATCH) { session, tick -> session.primaryAction(TimerMode.STOPWATCH, tick) }
        now += 10_000L
        store.update(TimerMode.STOPWATCH) { session, tick -> session.primaryAction(TimerMode.STOPWATCH, tick) }
        store.update(TimerMode.POMODORO) { _, tick -> TimerSession(300_000L, startedAtMillis = tick, breakPhase = true) }
        now += 20_000L

        val restored = reopen().state.value
        assertEquals(390_000L, restored.getValue(TimerMode.TIMER).displayedMillis(TimerMode.TIMER, now))
        assertEquals(10_000L, restored.getValue(TimerMode.STOPWATCH).displayedMillis(TimerMode.STOPWATCH, now))
        assertTrue(restored.getValue(TimerMode.POMODORO).breakPhase)
        assertEquals(280_000L, restored.getValue(TimerMode.POMODORO).displayedMillis(TimerMode.POMODORO, now))
    }

    @Test fun newBootResetsSessionsButRetainsConfiguredTimerDuration() {
        val store = reopen()
        store.update(TimerMode.TIMER) { session, _ -> session.adjustTimer(3) }
        store.update(TimerMode.TIMER) { session, tick -> session.primaryAction(TimerMode.TIMER, tick) }
        store.update(TimerMode.STOPWATCH) { session, tick -> session.primaryAction(TimerMode.STOPWATCH, tick) }
        now = 50_000L // May exceed the previous boot's saved start timestamp.
        val restored = reopen(8).state.value
        assertEquals(TimerSession(480_000L), restored.getValue(TimerMode.TIMER))
        assertEquals(TimerSession.initial(TimerMode.STOPWATCH), restored.getValue(TimerMode.STOPWATCH))
    }

    @Test fun unknownBootDoesNotRestorePotentiallyInvalidElapsedTime() {
        val store = reopen(-1)
        store.update(TimerMode.TIMER) { session, tick -> session.primaryAction(TimerMode.TIMER, tick) }
        now += 20_000L
        assertEquals(TimerSession.initial(TimerMode.TIMER), reopen(-1).state.value.getValue(TimerMode.TIMER))
    }

    @Test fun invalidFutureStartIsNotRestoredEvenWithSameBootCount() {
        val store = reopen()
        store.update(TimerMode.TIMER) { session, tick -> session.primaryAction(TimerMode.TIMER, tick) }
        now = 100L
        assertEquals(TimerSession.initial(TimerMode.TIMER), reopen().state.value.getValue(TimerMode.TIMER))
    }

    @Test fun differentContextWrappersShareTheApplicationStore() {
        assertSame(LocalTimerStore.get(context), LocalTimerStore.get(ContextWrapper(context)))
    }
}
