package com.mrbastim.standby.extended.display

import com.mrbastim.standby.extended.data.settings.DisplayProtectionSettings
import com.mrbastim.standby.extended.data.settings.ScreenTimeout
import com.mrbastim.standby.extended.data.settings.StandBySettings
import org.junit.Assert.*
import org.junit.Test

class DisplayProtectionPolicyTest {
    private val settings = StandBySettings()
    private fun brightness(expected: Float, decision: DisplayDecision) =
        assertEquals(expected, requireNotNull(decision.brightness), 0.0001f)

    @Test fun changedTransitionDurationTakesEffectDuringFade() {
        val policy = DisplayProtectionPolicy(0)
        policy.evaluate(0, settings)
        policy.evaluate(120_000, settings)
        val halfway = policy.evaluate(126_000, settings)
        val faster = settings.copy(displayProtection = settings.displayProtection.copy(transitionSeconds = 2))
        assertEquals(halfway.brightness, policy.evaluate(126_000, faster).brightness)
        assertEquals(DisplayPhase.IDLE, policy.evaluate(127_000, faster).phase)
        brightness(0.08f, policy.evaluate(128_000, faster))
        assertEquals(DisplayPhase.AMBIENT, policy.evaluate(128_000, faster).phase)
    }

    @Test fun actualWindowRequestsStayWithinBrightnessBounds() {
        for (percent in listOf(1, 100)) {
            val policy = DisplayProtectionPolicy(0)
            val extremes = settings.copy(displayProtection =
                DisplayProtectionSettings(initialPercent = percent, minimumPercent = percent))
            brightness(percent / 100f, policy.evaluate(0, extremes))
            brightness(percent / 100f, policy.evaluate(500_000, extremes))
        }
    }

    @Test fun inactivityAtMinimumDoesNotEnableNightModeWithoutDarkness() {
        val policy = DisplayProtectionPolicy(0)
        val automatic = settings.copy(displayProtection = settings.displayProtection.copy(ambientLight = true))
        policy.light.observe(0, 80f)
        policy.evaluate(0, automatic)
        val ambient = policy.evaluate(132_000, automatic)
        brightness(0.08f, ambient)
        assertFalse(ambient.automaticNightMode)
        assertEquals(DisplayPhase.AMBIENT, ambient.phase)
    }

    @Test fun totalDarknessEnablesNightOnlyAfterTheFadeReachesMinimum() {
        val policy = DisplayProtectionPolicy(0)
        val automatic = settings.copy(displayProtection = settings.displayProtection.copy(ambientLight = true))
        policy.light.observe(0, 0f)
        policy.evaluate(0, automatic)
        policy.evaluate(5000, automatic)
        policy.evaluate(5800, automatic)
        val beginning = policy.evaluate(10_000, automatic)
        assertFalse(beginning.automaticNightMode)
        val dimmest = policy.evaluate(10_800, automatic)
        brightness(0.08f, dimmest)
        assertTrue(dimmest.automaticNightMode)
        policy.interact(11_000)
        brightness(0.08f, policy.evaluate(11_000, automatic))
        assertTrue(policy.evaluate(11_800, automatic).automaticNightMode)
    }

    @Test fun ordinaryDimRoomDoesNotEnableNightMode() {
        val policy = DisplayProtectionPolicy(0)
        val automatic = settings.copy(displayProtection = settings.displayProtection.copy(ambientLight = true))
        policy.light.observe(0, 8f)
        policy.evaluate(0, automatic)
        assertFalse(policy.evaluate(120_000, automatic).automaticNightMode)
        assertFalse(policy.evaluate(132_000, automatic).automaticNightMode)
    }

    @Test fun disablingAdaptationOrMissingSensorNeverSelectsNightMode() {
        val policy = DisplayProtectionPolicy(0)
        policy.light.observe(0, 0f)
        policy.evaluate(0, settings)
        assertFalse(policy.evaluate(132_000, settings).automaticNightMode)
        val fresh = DisplayProtectionPolicy(0)
        val automatic = settings.copy(displayProtection = settings.displayProtection.copy(ambientLight = true))
        assertFalse(fresh.evaluate(132_000, automatic).automaticNightMode)
    }

    @Test fun returningLightExitsNightEvenWhileIdleBrightnessRemainsMinimum() {
        val policy = DisplayProtectionPolicy(0)
        val automatic = settings.copy(displayProtection = settings.displayProtection.copy(ambientLight = true))
        policy.light.observe(0, 0f)
        policy.evaluate(0, automatic)
        assertTrue(policy.evaluate(132_000, automatic).automaticNightMode)
        policy.light.observe(133_000, 100f)
        for (second in 134..145) policy.evaluate(second * 1000L, automatic)
        val lit = policy.evaluate(146_000, automatic)
        brightness(0.08f, lit)
        assertFalse(lit.automaticNightMode)
    }

    @Test fun activeIdleAmbient() {
        val policy = DisplayProtectionPolicy(0)
        brightness(0.35f, policy.evaluate(0, settings))
        assertEquals(DisplayPhase.ACTIVE, policy.evaluate(119_999, settings).phase)
        val idle = policy.evaluate(120_000, settings)
        assertEquals(DisplayPhase.IDLE, idle.phase)
        brightness(0.35f, idle)
        brightness(0.215f, policy.evaluate(126_000, settings))
        val ambient = policy.evaluate(132_000, settings)
        assertEquals(DisplayPhase.AMBIENT, ambient.phase)
        brightness(0.08f, ambient)
    }

    @Test fun delayedFrameUsesMonotonicDeadline() {
        val policy = DisplayProtectionPolicy(9000)
        policy.evaluate(9000, settings)
        brightness(0.08f, policy.evaluate(500_000, settings))
    }

    @Test fun interactionRetargetsCurrentFadeWithoutJump() {
        val policy = DisplayProtectionPolicy(0)
        policy.evaluate(0, settings)
        policy.evaluate(120_000, settings)
        val previous = policy.evaluate(126_000, settings)
        policy.interact(126_000)
        val restored = policy.evaluate(126_000, settings)
        assertEquals(DisplayPhase.ACTIVE, restored.phase)
        assertEquals(previous.brightness, restored.brightness)
        brightness(0.2825f, policy.evaluate(126_400, settings))
        brightness(0.35f, policy.evaluate(126_800, settings))
        assertEquals(DisplayPhase.ACTIVE, policy.evaluate(245_999, settings).phase)
        assertEquals(DisplayPhase.IDLE, policy.evaluate(246_000, settings).phase)
    }

    @Test fun repeatedInteractionDoesNotRestartBrightnessAnimation() {
        val policy = DisplayProtectionPolicy(0)
        policy.evaluate(0, settings)
        policy.evaluate(132_000, settings)
        policy.interact(132_000)
        policy.evaluate(132_000, settings)
        repeat(8) {
            policy.interact(132_000L + it * 100L)
            policy.evaluate(132_000L + it * 100L, settings)
        }
        brightness(0.35f, policy.evaluate(132_800, settings))
    }

    @Test fun recompositionAndDataRefreshDoNotResetInactivity() {
        val policy = DisplayProtectionPolicy(0)
        repeat(133) { second ->
            policy.evaluate(second * 1000L, settings.copy(showDate = second % 2 == 0))
        }
        assertEquals(DisplayPhase.AMBIENT, policy.evaluate(133_000, settings).phase)
    }

    @Test fun invalidPreferencesAreBounded() {
        val config = DisplayProtectionSettings(initialPercent = -50, minimumPercent = 300,
            dimDelaySeconds = -1, transitionSeconds = 1000, releaseAfterMinutes = -20).normalized()
        assertEquals(1, config.initialPercent)
        assertEquals(1, config.minimumPercent)
        assertEquals(10, config.dimDelaySeconds)
        assertEquals(60, config.transitionSeconds)
        assertEquals(0, config.releaseAfterMinutes)
        val maximum = DisplayProtectionSettings(initialPercent = 900, minimumPercent = -100).normalized()
        assertEquals(100, maximum.initialPercent)
        assertEquals(1, maximum.minimumPercent)
    }

    @Test fun missingSensorFallsBackToUserBrightness() {
        val policy = DisplayProtectionPolicy(0)
        val automatic = settings.copy(displayProtection = settings.displayProtection.copy(ambientLight = true))
        brightness(0.35f, policy.evaluate(0, automatic))
        brightness(0.35f, policy.evaluate(110_000, automatic))
        brightness(0.08f, policy.evaluate(132_000, automatic))
    }

    @Test fun darkRoomNeverFallsBelowConfiguredMinimum() {
        val policy = DisplayProtectionPolicy(0)
        val automatic = settings.copy(displayProtection = settings.displayProtection.copy(ambientLight = true))
        policy.light.observe(0, 0f)
        policy.evaluate(0, automatic)
        policy.evaluate(5000, automatic)
        brightness(0.2015f, policy.evaluate(5800, automatic))
        policy.light.observe(120_000, 10000f)
        brightness(0.08f, policy.evaluate(132_000, automatic))
    }

    @Test fun settingsChangesRetargetWithoutResettingInactivity() {
        val policy = DisplayProtectionPolicy(0)
        policy.evaluate(0, settings)
        val changed = settings.copy(displayProtection = settings.displayProtection.copy(initialPercent = 60))
        brightness(0.35f, policy.evaluate(50_000, changed))
        brightness(0.60f, policy.evaluate(50_800, changed))
        assertEquals(DisplayPhase.IDLE, policy.evaluate(120_000, changed).phase)
        val minimum = changed.copy(displayProtection = changed.displayProtection.copy(minimumPercent = 20))
        policy.evaluate(132_000, changed)
        brightness(0.08f, policy.evaluate(132_000, minimum))
        brightness(0.20f, policy.evaluate(144_000, minimum))
    }

    @Test fun changingDelayDuringSessionUsesExistingLastInput() {
        val policy = DisplayProtectionPolicy(0)
        policy.evaluate(0, settings)
        val changed = settings.copy(displayProtection = settings.displayProtection.copy(dimDelaySeconds = 20))
        assertEquals(DisplayPhase.AMBIENT, policy.evaluate(40_000, changed).phase)
        brightness(0.08f, policy.evaluate(40_000, changed))
    }

    @Test fun shorteningDelayAfterRecentFrameKeepsElapsedInactivity() {
        val policy = DisplayProtectionPolicy(0)
        policy.evaluate(0, settings)
        policy.evaluate(40_000, settings)
        val changed = settings.copy(displayProtection = settings.displayProtection.copy(dimDelaySeconds = 20))
        val result = policy.evaluate(40_000, changed)
        assertEquals(DisplayPhase.AMBIENT, result.phase)
        brightness(0.08f, result)
    }

    @Test fun shorteningDelayWhileAlreadyFadingDoesNotRestartFullFade() {
        val policy = DisplayProtectionPolicy(0)
        policy.evaluate(0, settings)
        policy.evaluate(120_000, settings)
        policy.evaluate(126_000, settings)
        val changed = settings.copy(displayProtection = settings.displayProtection.copy(dimDelaySeconds = 100))
        val result = policy.evaluate(126_000, changed)
        assertEquals(DisplayPhase.AMBIENT, result.phase)
        brightness(0.08f, result)
    }

    @Test fun disabledProtectionReturnsWindowControlAndKeepsLegacyTimeout() {
        val policy = DisplayProtectionPolicy(0)
        val disabled = settings.copy(keepScreenAwake = false, screenTimeout = ScreenTimeout.ONE_MINUTE,
            displayProtection = settings.displayProtection.copy(enabled = false))
        assertNull(policy.evaluate(0, disabled).brightness)
        assertTrue(policy.evaluate(59_999, disabled).keepAwake)
        assertTrue(policy.evaluate(60_000, disabled).releaseRequested)
    }

    @Test fun keepAwakeOverridesLegacyTimeout() {
        for (timeout in ScreenTimeout.entries) {
            val policy = DisplayProtectionPolicy(0)
            assertTrue(policy.evaluate(1_000_000, settings.copy(screenTimeout = timeout)).keepAwake)
        }
    }

    @Test fun finiteTimeoutResetsOnInteraction() {
        val policy = DisplayProtectionPolicy(0)
        val timed = settings.copy(keepScreenAwake = false, screenTimeout = ScreenTimeout.ONE_MINUTE)
        assertTrue(policy.evaluate(0, timed).keepAwake)
        policy.interact(59_000)
        assertFalse(policy.evaluate(60_000, timed).releaseRequested)
        assertTrue(policy.evaluate(119_000, timed).releaseRequested)
        policy.interact(120_000)
        assertTrue(policy.evaluate(120_000, timed).keepAwake)
    }

    @Test fun followSystemDoesNotHoldScreen() {
        val policy = DisplayProtectionPolicy(0)
        val system = settings.copy(keepScreenAwake = false)
        assertFalse(policy.evaluate(0, system).keepAwake)
        assertFalse(policy.evaluate(100_000, system).releaseRequested)
    }

    @Test fun explicitProtectionDeadlineOverridesKeepAwakeAndNever() {
        val timed = settings.copy(screenTimeout = ScreenTimeout.NEVER,
            displayProtection = settings.displayProtection.copy(releaseAfterMinutes = 15))
        val policy = DisplayProtectionPolicy(0)
        assertTrue(policy.evaluate(899_999, timed).keepAwake)
        assertEquals(DisplayPhase.RELEASED, policy.evaluate(900_000, timed).phase)
        policy.interact(900_001)
        assertTrue(policy.evaluate(900_001, timed).keepAwake)
    }

    @Test fun explicitReleaseWorksEvenWhenSystemTimeoutAlreadyOwnsScreen() {
        val timed = settings.copy(keepScreenAwake = false,
            displayProtection = settings.displayProtection.copy(releaseAfterMinutes = 15))
        assertTrue(DisplayProtectionPolicy(0).evaluate(900_000, timed).releaseRequested)
    }

    @Test fun newActivitySessionDoesNotInheritOldIdleState() {
        val old = DisplayProtectionPolicy(0)
        old.evaluate(0, settings)
        assertEquals(DisplayPhase.AMBIENT, old.evaluate(132_000, settings).phase)
        val recreated = DisplayProtectionPolicy(132_000)
        assertEquals(DisplayPhase.ACTIVE, recreated.evaluate(132_000, settings).phase)
        brightness(0.35f, recreated.evaluate(132_000, settings))
    }

    @Test fun dreamRestartGetsIndependentSessionAndSensorHistory() {
        val stopped = DisplayProtectionPolicy(0)
        stopped.light.observe(0, 0f)
        assertEquals(0.45f, stopped.light.factor(5000))
        val restarted = DisplayProtectionPolicy(200_000)
        assertEquals(1f, restarted.light.factor(200_000))
        assertEquals(DisplayPhase.ACTIVE, restarted.evaluate(200_000, settings).phase)
    }
}
