package com.mrbastim.standby.extended.display

import android.content.Context
import android.os.SystemClock
import android.view.Window
import android.view.WindowManager
import com.mrbastim.standby.extended.data.settings.StandBySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

/** One owner per window. start/stop may repeat; stop restores the captured window attributes. */
class DisplayProtectionController(
    context: Context,
    private val window: Window,
    private val scope: CoroutineScope,
    private val onHoldChanged: (Boolean) -> Unit = {},
    private val onRelease: () -> Unit = {},
) {
    private val sensor = AmbientLightSource(context.applicationContext)
    private var policy: DisplayProtectionPolicy? = null
    private var settings = StandBySettings()
    private var job: Job? = null
    private val changes = Channel<Unit>(Channel.CONFLATED)
    private var savedBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    private var savedKeepAwake = false
    private var lastHold: Boolean? = null
    private var released = false
    private val mutablePhase = MutableStateFlow(DisplayPhase.ACTIVE)
    val phase = mutablePhase.asStateFlow()
    private val mutableAutomaticNightMode = MutableStateFlow(false)
    val automaticNightMode = mutableAutomaticNightMode.asStateFlow()

    fun update(value: StandBySettings) {
        if (settings == value) return
        if (settings.displayProtection.ambientLight != value.displayProtection.ambientLight ||
            settings.displayProtection.enabled != value.displayProtection.enabled) policy?.resetLight()
        settings = value
        updateSensor()
        render()
        changes.trySend(Unit)
    }

    fun start() {
        if (job != null) return
        savedBrightness = window.attributes.screenBrightness
        savedKeepAwake = window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
        policy = DisplayProtectionPolicy(SystemClock.elapsedRealtime())
        released = false
        lastHold = null
        updateSensor()
        job = scope.launch {
            while (true) {
                render()
                // Input wakes this single loop immediately; only an active fade needs 20 Hz.
                val waitMillis = if (policy?.isFading(SystemClock.elapsedRealtime()) == true) 50L else 1000L
                withTimeoutOrNull(waitMillis) { changes.receive() }
            }
        }
    }

    fun interact() {
        policy?.interact(SystemClock.elapsedRealtime())
        released = false
        render()
        changes.trySend(Unit)
    }

    private fun updateSensor() {
        if (policy != null && settings.displayProtection.enabled && settings.displayProtection.ambientLight) {
            sensor.start { policy?.light?.observe(SystemClock.elapsedRealtime(), it) }
        } else sensor.stop()
    }

    private fun render() {
        val decision = policy?.evaluate(SystemClock.elapsedRealtime(), settings) ?: return
        mutablePhase.value = decision.phase
        mutableAutomaticNightMode.value = decision.automaticNightMode
        if (lastHold != decision.keepAwake) {
            onHoldChanged(decision.keepAwake)
            if (decision.keepAwake) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            lastHold = decision.keepAwake
        }
        val brightness = decision.brightness ?: savedBrightness
        if (abs(window.attributes.screenBrightness - brightness) >= 0.001f) {
            window.attributes = window.attributes.apply { screenBrightness = brightness }
        }
        if (decision.releaseRequested && !released) {
            released = true
            onRelease()
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        sensor.stop()
        policy = null
        mutablePhase.value = DisplayPhase.ACTIVE
        mutableAutomaticNightMode.value = false
        if (lastHold != null) {
            onHoldChanged(savedKeepAwake)
            window.attributes = window.attributes.apply { screenBrightness = savedBrightness }
            if (savedKeepAwake) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        lastHold = null
    }
}
