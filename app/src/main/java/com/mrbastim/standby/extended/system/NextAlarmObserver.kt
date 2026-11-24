package com.mrbastim.standby.extended.system

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import java.time.ZoneId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class NextAlarmState(
    val triggerAtMillis: Long? = null,
    val zoneId: ZoneId = ZoneId.systemDefault(),
    val isCalendarReminder: Boolean = false,
) {
    internal fun futureTriggerAtMillis(nowMillis: Long): Long? =
        triggerAtMillis?.takeIf { it > nowMillis }
}

private const val ALARM_REFRESH_INTERVAL_MILLIS = 60_000L
private const val CALENDAR_PROVIDER_PACKAGE = "com.android.providers.calendar"

class NextAlarmObserver internal constructor(
    private val context: Context,
    private val currentTimeMillis: () -> Long,
) {
    constructor(context: Context) : this(context, System::currentTimeMillis)

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    val state: Flow<NextAlarmState> = callbackFlow {
        val refreshRequests = Channel<Unit>(Channel.CONFLATED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                refreshRequests.trySend(Unit)
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED).apply {
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_SCREEN_ON)
            },
            ContextCompat.RECEIVER_EXPORTED,
        )
        launch {
            while (isActive) {
                val alarm = alarmManager?.nextAlarmClock
                val nowMillis = currentTimeMillis()
                val triggerAtMillis = alarm?.triggerTime?.takeIf { it > nowMillis }
                // Some calendar reminders occupy the system's single next-alarm slot.
                val isCalendarReminder = triggerAtMillis != null &&
                    alarm?.showIntent?.creatorPackage == CALENDAR_PROVIDER_PACKAGE
                send(
                    NextAlarmState(
                        triggerAtMillis = if (isCalendarReminder) null else triggerAtMillis,
                        isCalendarReminder = isCalendarReminder,
                    ),
                )

                // Recheck at the system event's expiry even if its time is hidden in the UI.
                val waitMillis = triggerAtMillis?.let { it - nowMillis }
                    ?.coerceAtMost(ALARM_REFRESH_INTERVAL_MILLIS) ?: ALARM_REFRESH_INTERVAL_MILLIS
                withTimeoutOrNull(waitMillis) { refreshRequests.receive() }
            }
        }
        awaitClose { context.unregisterReceiver(receiver) }
    }.conflate().distinctUntilChanged()
}
