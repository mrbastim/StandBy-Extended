package com.mrbastim.standby.extended.ui.standby

import androidx.compose.ui.graphics.Color
import com.mrbastim.standby.extended.data.settings.ClockColor
import com.mrbastim.standby.extended.data.settings.StandBySettings
import com.mrbastim.standby.extended.data.settings.StandByThemeId
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeEngineTest {
    @Test
    fun monoLightFallsBackToCrimsonAtNight() {
        val settings = StandBySettings(
            nightMode = true,
            nightTheme = StandByThemeId.MONO_LIGHT,
        )

        assertEquals(StandByThemeId.CRIMSON_NIGHT, resolveStandByTheme(settings).id)
    }

    @Test
    fun explicitClockColorOverridesThemeClockColor() {
        val settings = StandBySettings(clockColor = ClockColor.ORANGE)

        assertEquals(Color(ClockColor.ORANGE.argb), resolveStandByTheme(settings).clockPrimary)
    }
}
