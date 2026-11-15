package com.mrbastim.standby.extended.display

import org.junit.Assert.*
import org.junit.Test

class AmbientLightFilterTest {
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
