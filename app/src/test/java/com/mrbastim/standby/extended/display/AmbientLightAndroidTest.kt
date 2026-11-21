package com.mrbastim.standby.extended.display

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorManager
import com.mrbastim.standby.extended.data.settings.DisplayProtectionSettings
import com.mrbastim.standby.extended.data.settings.StandBySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSensor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35])
class AmbientLightAndroidTest {
    @Test fun registrationIsOptionalIdempotentAndReleasedAcrossSessions() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        val manager = shadowOf(activity.get().getSystemService(SensorManager::class.java))
        manager.addSensor(ShadowSensor.newInstance(Sensor.TYPE_LIGHT))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val controller = DisplayProtectionController(activity.get(), activity.get().window, scope)
        try {
            controller.start()
            assertEquals(0, manager.listeners.size)
            val automatic = StandBySettings(displayProtection = DisplayProtectionSettings(ambientLight = true))
            controller.update(automatic)
            assertEquals(1, manager.listeners.size)
            controller.start()
            controller.update(automatic.copy(showSeconds = true))
            assertEquals(1, manager.listeners.size)
            controller.stop()
            assertEquals(0, manager.listeners.size)
            controller.start()
            assertEquals(1, manager.listeners.size)
            controller.update(automatic.copy(displayProtection = automatic.displayProtection.copy(enabled = false)))
            assertEquals(0, manager.listeners.size)
        } finally {
            controller.stop()
            scope.cancel()
            activity.pause().stop().destroy()
        }
    }

    @Test fun missingSensorLeavesManualWindowBrightnessUsable() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        val source = AmbientLightSource(activity.get())
        assertFalse(source.available)
        source.start { fail("A missing sensor must not emit readings") }
        source.stop()
        activity.pause().stop().destroy()
    }

    @Test fun failedSensorRegistrationCanBeStoppedSafely() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        val manager = shadowOf(activity.get().getSystemService(SensorManager::class.java))
        manager.addSensor(ShadowSensor.newInstance(Sensor.TYPE_LIGHT))
        manager.setForceListenersToFail(true)
        val source = AmbientLightSource(activity.get())
        assertTrue(source.available)
        source.start { }
        assertEquals(0, manager.listeners.size)
        source.stop()
        activity.pause().stop().destroy()
    }
}
