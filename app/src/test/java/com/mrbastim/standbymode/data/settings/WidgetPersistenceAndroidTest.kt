package com.mrbastim.standbymode.data.settings

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35])
class WidgetPersistenceAndroidTest {
    @Test fun selectedWidgetTypesRoundTripIndependentlyAndSurviveReorder() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val repo = SettingsRepository(context)
        val before = repo.settings.first()
        val left = before.leftWidgets.last()
        val right = before.rightWidgets.last()
        repo.setActiveWidget(WidgetColumn.LEFT, left)
        repo.setActiveWidget(WidgetColumn.RIGHT, right)
        val restored = SettingsRepository(context).settings.first()
        assertEquals(left, restored.leftActiveWidget)
        assertEquals(right, restored.rightActiveWidget)
        if (restored.leftWidgets.size > 1) {
            repo.moveWidget(WidgetColumn.LEFT, restored.leftWidgets.lastIndex, 0)
            val reordered = SettingsRepository(context).settings.first()
            assertEquals(left, reordered.leftWidgets.first())
            assertEquals(left, reordered.leftActiveWidget)
            repo.toggleWidget(WidgetColumn.LEFT, left)
            val removed = SettingsRepository(context).settings.first()
            assertEquals(removed.leftWidgets.first(), removed.leftActiveWidget)
            assertEquals(right, removed.rightActiveWidget)
        }
    }

    @Test fun everyUniqueWidgetCanBeStoredInBothStacks() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val repo = SettingsRepository(context)
        for (column in WidgetColumn.entries) {
            for (widget in StandByWidgetType.entries) {
                val settings = repo.settings.first()
                val stack = if (column == WidgetColumn.LEFT) settings.leftWidgets else settings.rightWidgets
                if (widget !in stack) repo.toggleWidget(column, widget)
            }
        }
        val restored = SettingsRepository(context).settings.first()
        for (stack in listOf(restored.leftWidgets, restored.rightWidgets)) {
            assertEquals(StandByWidgetType.entries.size, stack.size)
            assertEquals(StandByWidgetType.entries.toSet(), stack.toSet())
        }
    }
}
