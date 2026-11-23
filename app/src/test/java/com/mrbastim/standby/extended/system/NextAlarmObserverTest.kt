package com.mrbastim.standby.extended.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Looper
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
        val now = System.currentTimeMillis()
        val first = scheduleAlarm(now + 120_000, 1)
        val second = scheduleAlarm(now + 240_000, 2)

        observe { triggers ->
            assertEquals(listOf(now + 120_000), triggers)
            manager.cancel(first)
            notifyAlarmChanged()
            assertEquals(listOf(now + 120_000, now + 240_000), triggers)
            manager.cancel(second)
            notifyAlarmChanged()
            assertEquals(listOf(now + 120_000, now + 240_000, null), triggers)
        }
    }

    @Test
    fun refreshClearsCancellationWithoutBroadcast() {
        val trigger = System.currentTimeMillis() + 120_000
        val operation = scheduleAlarm(trigger, 1)

        observe { triggers ->
            assertEquals(listOf(trigger), triggers)
            manager.cancel(operation)
            looper.idleFor(Duration.ofMinutes(1))
            assertEquals(listOf(trigger, null), triggers)
        }
    }

    @Test
    fun expiryClearsAnAlarmEvenWhenAlarmManagerStillReturnsIt() {
        val trigger = System.currentTimeMillis() + 10_000
        scheduleAlarm(trigger, 1)

        observe { triggers ->
            looper.idleFor(Duration.ofMillis(9_999))
            assertEquals(listOf(trigger), triggers)
            looper.idleFor(Duration.ofMillis(1))
            assertEquals(trigger, manager.nextAlarmClock?.triggerTime)
            assertEquals(listOf(trigger, null), triggers)
        }

        observe { triggers ->
            assertEquals(listOf<Long?>(null), triggers)
        }
    }

    private fun scheduleAlarm(trigger: Long, requestCode: Int): PendingIntent {
        val operation = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent("test.NEXT_ALARM").setPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE,
        )
        manager.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, operation), operation)
        return operation
    }

    private fun notifyAlarmChanged() {
        context.sendBroadcast(Intent(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED))
        looper.idle()
    }

    private fun observe(assertUpdates: (List<Long?>) -> Unit) {
        val scope = CoroutineScope(Dispatchers.Main.immediate)
        val triggers = mutableListOf<Long?>()
        scope.launch {
            NextAlarmObserver(context).state.collect { triggers += it.triggerAtMillis }
        }
        try {
            looper.idle()
            assertUpdates(triggers)
        } finally {
            scope.cancel()
            looper.idle()
        }
    }
}
