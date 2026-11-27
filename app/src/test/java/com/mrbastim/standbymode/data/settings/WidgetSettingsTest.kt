package com.mrbastim.standbymode.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSettingsTest {
    private val defaults = listOf(StandByWidgetType.CLOCK, StandByWidgetType.BATTERY)

    @Test
    fun invalidOrEmptyStoredWidgetsFallBackToDefaults() {
        assertEquals(defaults, decodeWidgets(null, defaults))
        assertEquals(defaults, decodeWidgets("UNKNOWN", defaults))
    }

    @Test
    fun storedWidgetsAreDeduplicatedAndInvalidValuesIgnored() {
        assertEquals(
            listOf(StandByWidgetType.DATE, StandByWidgetType.CLOCK),
            decodeWidgets("DATE,UNKNOWN,DATE,CLOCK", defaults),
        )
    }

    @Test
    fun lastWidgetCannotBeRemoved() {
        val onlyClock = listOf(StandByWidgetType.CLOCK)
        assertEquals(onlyClock, toggleWidgetInList(onlyClock, StandByWidgetType.CLOCK))
    }

    @Test
    fun widgetsCanBeAddedRemovedAndReordered() {
        assertEquals(
            listOf(StandByWidgetType.CLOCK, StandByWidgetType.BATTERY, StandByWidgetType.DATE),
            toggleWidgetInList(defaults, StandByWidgetType.DATE),
        )
        assertEquals(
            listOf(StandByWidgetType.BATTERY),
            toggleWidgetInList(defaults, StandByWidgetType.CLOCK),
        )
        assertEquals(
            listOf(StandByWidgetType.BATTERY, StandByWidgetType.CLOCK),
            moveWidgetInList(defaults, 0, 1),
        )
    }

    @Test
    fun clockAccessoryIsIndependentOfAlarmCardsAndMigratesLegacyChoice() {
        assertEquals(ClockAccessory.NONE, decodeClockAccessory(null, "NEXT_ALARM", false))
        assertEquals(ClockAccessory.NEXT_ALARM, decodeClockAccessory("NEXT_ALARM", null, false))
        assertEquals(ClockAccessory.NONE, decodeClockAccessory("NONE", null, true))
        assertEquals(ClockAccessory.TIMER, decodeClockAccessory(null, "TIMER", false))
    }

    @Test
    fun hiddenAlarmCardsDoNotDestroyConfiguredStacks() {
        val configured = listOf(StandByWidgetType.NEXT_ALARM, StandByWidgetType.MUSIC)
        assertEquals(configured, visibleWidgets(configured, true))
        assertEquals(listOf(StandByWidgetType.MUSIC), visibleWidgets(configured, false))
        assertEquals(listOf(StandByWidgetType.CLOCK), visibleWidgets(listOf(StandByWidgetType.NEXT_ALARM), false))
    }

    @Test
    fun invalidMoveLeavesWidgetsUntouched() {
        assertEquals(defaults, moveWidgetInList(defaults, -1, 1))
        assertEquals(defaults, moveWidgetInList(defaults, 0, 5))
    }
}
