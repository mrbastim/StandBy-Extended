package com.mrbastim.standbymode.display

import com.mrbastim.standbymode.data.settings.DisplayProtectionSettings
import com.mrbastim.standbymode.data.settings.SettingsRepository
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
class DisplayProtectionSettingsAndroidTest {
    @Test fun allDisplaySettingsRoundTripThroughExistingDataStore() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val repository = SettingsRepository(context)
        val before = repository.settings.first()
        val expected = DisplayProtectionSettings(
            enabled = false,
            initialPercent = 47,
            minimumPercent = 5,
            dimDelaySeconds = 75,
            transitionSeconds = 18,
            ambientLight = true,
            releaseAfterMinutes = 60,
        )
        try {
            repository.setDisplayProtection(expected)
            val reopened = SettingsRepository(context).settings.first()
            assertEquals(expected, reopened.displayProtection)
            assertEquals(before.copy(displayProtection = expected), reopened)
        } finally {
            repository.setDisplayProtection(before.displayProtection)
        }
    }

    @Test fun invalidDisplaySettingsAreNormalizedBeforePersistence() = runBlocking {
        val repository = SettingsRepository(RuntimeEnvironment.getApplication())
        val before = repository.settings.first().displayProtection
        try {
            repository.setDisplayProtection(DisplayProtectionSettings(initialPercent = 4, minimumPercent = 99))
            val persisted = repository.settings.first().displayProtection
            assertEquals(4, persisted.initialPercent)
            assertEquals(4, persisted.minimumPercent)
        } finally {
            repository.setDisplayProtection(before)
        }
    }
}
