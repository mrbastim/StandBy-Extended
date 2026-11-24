package com.mrbastim.standbymode.data.settings

data class DisplayProtectionSettings(
    val enabled: Boolean = true,
    val initialPercent: Int = 35,
    val minimumPercent: Int = 8,
    val dimDelaySeconds: Int = 120,
    val transitionSeconds: Int = 12,
    val ambientLight: Boolean = false,
    // Zero disables the additional deadline. Android still owns actual display sleep.
    val releaseAfterMinutes: Int = 0,
) {
    fun normalized(): DisplayProtectionSettings {
        val initial = initialPercent.coerceIn(1, 100)
        return copy(
            initialPercent = initial,
            minimumPercent = minimumPercent.coerceIn(1, initial),
            dimDelaySeconds = dimDelaySeconds.coerceIn(10, 3600),
            transitionSeconds = transitionSeconds.coerceIn(1, 60),
            releaseAfterMinutes = releaseAfterMinutes.coerceIn(0, 720),
        )
    }
}
