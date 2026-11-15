package com.mrbastim.standby.extended.ui.standby

import com.mrbastim.standby.extended.data.settings.BackgroundStyle
import com.mrbastim.standby.extended.data.settings.StandBySettings
import com.mrbastim.standby.extended.data.settings.StandByThemeId
import org.junit.Assert.*
import org.junit.Test

class StandByPresentationTest {
    @Test fun ambientRetainsDayThemeInsteadOfForcingCrimsonNight() {
        for (theme in StandByThemeId.entries) {
            val saved = StandBySettings(dayTheme = theme, nightTheme = StandByThemeId.FOREST, showSeconds = true)
            val display = standByPresentationSettings(saved, ambient = true, automaticNightMode = false)
            assertFalse(display.nightMode)
            assertEquals(theme, display.dayTheme)
            assertEquals(saved.nightTheme, display.nightTheme)
            assertEquals(resolveStandByTheme(saved).id, resolveStandByTheme(display).id)
            assertFalse(display.showSeconds)
            assertEquals(BackgroundStyle.SOLID, display.backgroundStyle)
        }
    }

    @Test fun automaticNightSelectsConfiguredNightThemeWithoutChangingPreferences() {
        val saved = StandBySettings(dayTheme = StandByThemeId.CLASSIC, nightTheme = StandByThemeId.MIDNIGHT)
        val display = standByPresentationSettings(saved, ambient = false, automaticNightMode = true)
        assertTrue(display.nightMode)
        assertEquals(StandByThemeId.MIDNIGHT, resolveStandByTheme(display).id)
        assertFalse(saved.nightMode)
    }

    @Test fun manualNightModeRemainsEnabledRegardlessOfSensorAndPhase() {
        val saved = StandBySettings(nightMode = true, nightTheme = StandByThemeId.FOREST)
        assertTrue(standByPresentationSettings(saved, false, false).nightMode)
        assertTrue(standByPresentationSettings(saved, true, false).nightMode)
        assertEquals(saved, standByPresentationSettings(saved, false, false))
    }
}
