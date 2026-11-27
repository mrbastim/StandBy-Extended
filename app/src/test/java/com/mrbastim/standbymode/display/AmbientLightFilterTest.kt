package com.mrbastim.standbymode.display

import org.junit.Assert.*
import org.junit.Test

class AmbientLightFilterTest {
    @Test fun nearDarknessHasIndependentThresholdAndDwell() {
        val filter = AmbientLightFilter()
        filter.observe(0, 0f)
        assertFalse(filter.isDark(9999))
        assertTrue(filter.isDark(10_000))
        assertEquals(0f, filter.factor(10_000))
    }

    @Test fun oneOnChangeReadingCanBothEnterAndLeaveDarkness() {
        val filter = AmbientLightFilter()
        filter.observe(0, 0f)
        assertTrue(filter.isDark(10_000))
        filter.observe(11_000, 5f)
        for (second in 12..25) filter.isDark(second * 1000L)
        assertFalse(filter.isDark(26_000))
        assertTrue(filter.factor(26_000) > 0f)
    }

    @Test fun noSensorOrOrdinaryDimRoomIsNotTotalDarkness() {
        val filter = AmbientLightFilter()
        assertFalse(filter.isDark(30_000))
        filter.observe(30_000, 8f)
        assertFalse(filter.isDark(90_000))
    }

    @Test fun briefInitialDarkReadingDoesNotEnableNightMode() {
        val filter = AmbientLightFilter()
        filter.observe(0, 0f)
        filter.observe(1000, 100f)
        for (second in 2..20) assertFalse(filter.isDark(second * 1000L))
    }

    @Test fun requiresDwellBeforeAcceptingFirstDarkReading() {
        val filter = AmbientLightFilter()
        filter.observe(0, 1f)
        assertEquals(1f, filter.factor(4999))
        assertEquals(0.45f, filter.factor(5000))
    }

    @Test fun briefShadowDoesNotChangeBrightness() {
        val filter = AmbientLightFilter()
        filter.observe(0, 400f)
        filter.observe(1000, 0f)
        filter.observe(2000, 400f)
        assertEquals(1f, filter.factor(10_000))
    }

    @Test fun hysteresisPreventsFlickerNearThreshold() {
        val filter = AmbientLightFilter()
        filter.observe(0, 10f)
        assertEquals(0.45f, filter.factor(5000))
        repeat(60) {
            filter.observe(6000L + it * 1000, if (it % 2 == 0) 13f else 17f)
            assertEquals(0.45f, filter.factor(6000L + it * 1000))
        }
    }

    @Test fun rapidLightChangesAreRateLimited() {
        val filter = AmbientLightFilter()
        filter.observe(0, 0f)
        filter.factor(5000)
        repeat(8) { filter.observe(6000L + it * 1000, 1000f) }
        assertEquals(0.45f, filter.factor(14_999))
        assertEquals(1f, filter.factor(15_000))
    }

    @Test fun invalidMeasurementsDoNotPoisonFilter() {
        val filter = AmbientLightFilter()
        listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f).forEach { filter.observe(0, it) }
        assertEquals(1f, filter.factor(50_000))
    }

    @Test fun denseEventBurstDoesNotBypassTimeFilter() {
        val filter = AmbientLightFilter()
        filter.observe(0, 1000f)
        repeat(1000) { filter.observe(1000, 0f) }
        assertEquals(1f, filter.factor(10_000))
    }
}
