package com.mrbastim.standbymode.display

import android.app.Activity
import android.os.Looper
import android.view.WindowManager
import com.mrbastim.standbymode.StandByDreamService
import com.mrbastim.standbymode.data.settings.StandBySettings
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.time.Duration

/** Framework/window simulations, not evidence of physical AMOLED/OEM behavior. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35])
@LooperMode(LooperMode.Mode.PAUSED)
class DisplayProtectionAndroidTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controllers = mutableListOf<DisplayProtectionController>()

    @After fun cleanup() {
        controllers.forEach { it.stop() }
        scope.cancel()
    }

    private fun controller(activity: Activity, onRelease: () -> Unit = {}): DisplayProtectionController =
        DisplayProtectionController(activity, activity.window, scope, onRelease = onRelease)
            .also { controllers += it }

    private fun advance(millis: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))
    }

    @Test fun restoresWindowBrightnessAndKeepAwakeOnStop() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        val window = activity.get().window
        window.attributes = window.attributes.apply { screenBrightness = 0.62f }
        val controller = controller(activity.get())
        controller.start()
        advance(1)
        assertEquals(0.35f, window.attributes.screenBrightness, 0.001f)
        assertTrue(window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
        controller.stop()
        assertEquals(0.62f, window.attributes.screenBrightness, 0.001f)
        assertEquals(0, window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        advance(150_000)
        assertEquals(0.62f, window.attributes.screenBrightness, 0.001f)
        activity.pause().stop().destroy()
    }

    @Test fun interactionDuringQuietAmbientStartsSmoothRecoveryImmediately() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        val controller = controller(activity.get())
        controller.start()
        advance(133_000)
        assertEquals(DisplayPhase.AMBIENT, controller.phase.value)
        assertEquals(0.08f, activity.get().window.attributes.screenBrightness, 0.001f)
        controller.interact()
        advance(400)
        val halfway = activity.get().window.attributes.screenBrightness
        assertTrue("Recovery must have intermediate hardware brightness", halfway > 0.08f && halfway < 0.35f)
        advance(500)
        assertEquals(0.35f, activity.get().window.attributes.screenBrightness, 0.001f)
        controller.stop()
        activity.pause().stop().destroy()
    }

    @Test fun recreatedWindowGetsIndependentSessionAndRestoresSystemOverride() {
        val first = Robolectric.buildActivity(Activity::class.java).setup()
        val old = controller(first.get())
        old.start()
        advance(133_000)
        old.stop()
        assertEquals(-1f, first.get().window.attributes.screenBrightness, 0.001f)
        first.pause().stop().destroy()

        val second = Robolectric.buildActivity(Activity::class.java).setup()
        val recreated = controller(second.get())
        recreated.start()
        advance(1)
        assertEquals(DisplayPhase.ACTIVE, recreated.phase.value)
        assertEquals(0.35f, second.get().window.attributes.screenBrightness, 0.001f)
        recreated.stop()
        assertEquals(-1f, second.get().window.attributes.screenBrightness, 0.001f)
        second.pause().stop().destroy()
    }

    @Test fun releaseCallbackFiresOnceAndRestartBeginsActive() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        var releases = 0
        val controller = controller(activity.get()) { releases++ }
        controller.update(StandBySettings(displayProtection =
            com.mrbastim.standbymode.data.settings.DisplayProtectionSettings(releaseAfterMinutes = 1)))
        controller.start()
        advance(65_000)
        assertEquals(1, releases)
        controller.stop()
        controller.start()
        advance(1)
        assertEquals(DisplayPhase.ACTIVE, controller.phase.value)
        assertEquals(1, releases)
        controller.stop()
        activity.pause().stop().destroy()
    }

    @Test fun dreamLifecycleCanStopRestartAndDestroy() {
        val service = Robolectric.buildService(StandByDreamService::class.java).create()
        val dream = service.get()
        dream.onDreamingStarted()
        assertEquals(Lifecycle.State.RESUMED, dream.lifecycle.currentState)
        dream.onDreamingStopped()
        assertEquals(Lifecycle.State.CREATED, dream.lifecycle.currentState)
        dream.onDreamingStarted()
        assertEquals(Lifecycle.State.RESUMED, dream.lifecycle.currentState)
        dream.onDreamingStopped()
        service.destroy()
        assertEquals(Lifecycle.State.DESTROYED, dream.lifecycle.currentState)
    }
}
