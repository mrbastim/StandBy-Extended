package com.mrbastim.standby.extended.ui.standby

import com.mrbastim.standby.extended.data.settings.BackgroundStyle
import com.mrbastim.standby.extended.data.settings.StandBySettings

/** Inactivity reduces visual activity; it never selects a different theme by itself. */
internal fun standByPresentationSettings(
    settings: StandBySettings,
    ambient: Boolean,
    automaticNightMode: Boolean,
): StandBySettings = settings.copy(
    showSeconds = settings.showSeconds && !ambient,
    backgroundStyle = if (ambient) BackgroundStyle.SOLID else settings.backgroundStyle,
    nightMode = settings.nightMode || automaticNightMode,
)
