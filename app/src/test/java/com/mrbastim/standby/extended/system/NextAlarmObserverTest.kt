package com.mrbastim.standby.extended.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Looper
import android.os.SystemClock
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35])
class NextAlarmObserverTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val manager get() = requireNotNull(context.getSystemService(AlarmManager::class.java))
    private val looper get() = shadowOf(Looper.getMainLooper())

    @Test
    fun cancellationBroadcastPromotesTheNextAlarmAndThenClearsIt() {
        val now = nowMillis()
        val first = scheduleAlarm(now + 120_000, 1)
        val second = scheduleAlarm(now + 240_000, 2)

        observe { states ->
            assertEquals(listOf(now + 120_000), states.map { it.triggerAtMillis })
            manager.cancel(first)
            notifyAlarmChanged()
            assertEquals(listOf(now + 120_000, now + 240_000), states.map { it.triggerAtMillis })
            manager.cancel(second)
            notifyAlarmChanged()
            assertEquals(listOf(now + 120_000, now + 240_000, null), states.map { it.triggerAtMillis })
        }
    }

    @Test
    fun refreshClearsCancellationWithoutBroadcast() {
        val trigger = nowMillis() + 120_000
        val operation = scheduleAlarm(trigger, 1)

        observe { states ->
            assertEquals(listOf(trigger), states.map { it.triggerAtMillis })
            manager.cancel(operation)
            looper.idleFor(Duration.ofMinutes(1))
            assertEquals(listOf(trigger, null), states.map { it.triggerAtMillis })
        }
    }

    @Test
    fun expiryClearsAnAlarmEvenWhenAlarmManagerStillReturnsIt() {
        val trigger = nowMillis() + 10_000
        scheduleAlarm(trigger, 1)

        observe { states ->
            looper.idleFor(Duration.ofMillis(9_999))
            assertEquals(listOf(trigger), states.map { it.triggerAtMillis })
            looper.idleFor(Duration.ofMillis(1))
            assertEquals(trigger, manager.nextAlarmClock?.triggerTime)
            assertEquals(listOf(trigger, null), states.map { it.triggerAtMillis })
        }

        observe { states ->
            assertEquals(listOf(NextAlarmState()), states)
        }
    }

    @Test
    fun calendarReminderHidesItsTimeUntilTheSystemReportsTheClockAlarm() {
        val now = nowMillis()
        val reminder = scheduleAlarm(now + 10_000, 1, "com.android.providers.calendar")
        val alarm = scheduleAlarm(now + 120_000, 2, "com.sec.android.app.clockpackage")

        observe { states ->
            assertEquals(NextAlarmState(isCalendarReminder = true), states.single())

            looper.idleFor(Duration.ofMillis(9_999))
            manager.cancel(reminder)
            looper.idleFor(Duration.ofMillis(1))
            assertEquals(NextAlarmState(triggerAtMillis = now + 120_000), states.last())

            manager.cancel(alarm)
            notifyAlarmChanged()
            assertEquals(NextAlarmState(), states.last())
        }
    }

    private fun scheduleAlarm(
        trigger: Long,
        requestCode: Int,
        creatorPackage: String = context.packageName,
    ): PendingIntent {
        val operation = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent("test.NEXT_ALARM").setPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE,
        )
        shadowOf(operation).setCreatorPackage(creatorPackage)
        manager.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, operation), operation)
        return operation
    }

    private fun notifyAlarmChanged() {
        context.sendBroadcast(Intent(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED))
        looper.idle()
    }

    private fun observe(assertUpdates: (List<NextAlarmState>) -> Unit) {
        val scope = CoroutineScope(Dispatchers.Main.immediate)
        val states = mutableListOf<NextAlarmState>()
        scope.launch {
            NextAlarmObserver(context, ::nowMillis).state.collect { states += it }
        }
        try {
            looper.idle()
            assertUpdates(states)
        } finally {
            scope.cancel()
            looper.idle()
        }
    }

    // Advance the observer's wall clock together with the Robolectric looper.
    private fun nowMillis(): Long = 1_700_000_000_000L + SystemClock.uptimeMillis()
}
