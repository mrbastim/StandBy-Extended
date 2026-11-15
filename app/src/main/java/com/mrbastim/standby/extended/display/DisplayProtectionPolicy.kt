package com.mrbastim.standby.extended.display

import com.mrbastim.standby.extended.data.settings.DisplayProtectionSettings
import com.mrbastim.standby.extended.data.settings.ScreenTimeout
import com.mrbastim.standby.extended.data.settings.StandBySettings
import kotlin.math.abs
import kotlin.math.exp

enum class DisplayPhase { ACTIVE, IDLE, AMBIENT, RELEASED }

data class DisplayDecision(
    val phase: DisplayPhase,
    val brightness: Float?,
    val keepAwake: Boolean,
    val releaseRequested: Boolean,
    val automaticNightMode: Boolean = false,
)

/** Pure session policy. All times are monotonic milliseconds; a new session resets inactivity. */
class DisplayProtectionPolicy(startedAt: Long) {
    private var lastInteraction = startedAt
    private var lastFrame = startedAt
    private var brightness: Float? = null
    private var destination: Float? = null
    private var fadeFrom = 0f
    private var fadeStarted = startedAt
    private var fadeDuration = 0L
    private var previousConfig: DisplayProtectionSettings? = null
    var light = AmbientLightFilter()
        private set

    fun resetLight() { light = AmbientLightFilter() }
    fun isFading(now: Long): Boolean = destination != null && now - fadeStarted < fadeDuration

    fun interact(now: Long) { lastInteraction = now }

    fun evaluate(now: Long, settings: StandBySettings): DisplayDecision {
        val config = settings.displayProtection.normalized()
        val idle = (now - lastInteraction).coerceAtLeast(0)
        val dimAt = config.dimDelaySeconds * 1000L
        val phase = when {
            !config.enabled || idle < dimAt -> DisplayPhase.ACTIVE
            idle < dimAt + config.transitionSeconds * 1000L -> DisplayPhase.IDLE
            else -> DisplayPhase.AMBIENT
        }
        val target = if (!config.enabled) null else if (phase != DisplayPhase.ACTIVE) {
            config.minimumPercent / 100f
        } else {
            val factor = if (config.ambientLight) light.factor(now) else 1f
            (config.minimumPercent + (config.initialPercent - config.minimumPercent) * factor) / 100f
        }
        // Sample the current fade before retargeting, so touch/settings/sensor events never jump.
        brightness = sample(now)
        val durationChanged = phase != DisplayPhase.ACTIVE && isFading(now) &&
            previousConfig?.transitionSeconds != config.transitionSeconds
        if (target != destination || durationChanged) {
            fadeFrom = brightness ?: target ?: 0f
            fadeStarted = if (!durationChanged && phase != DisplayPhase.ACTIVE && lastFrame < lastInteraction + dimAt) {
                lastInteraction + dimAt
            } else now
            fadeDuration = if (phase != DisplayPhase.ACTIVE) config.transitionSeconds * 1000L else 800L
            destination = target
            brightness = sample(now)
        }
        if (target == null) brightness = null
        lastFrame = now
        previousConfig = config
        val displayPhase = if (phase != DisplayPhase.ACTIVE) {
            if (isFading(now) && abs((brightness ?: 0f) - (target ?: 0f)) > 0.0001f) DisplayPhase.IDLE
            else DisplayPhase.AMBIENT
        } else phase

        val timeout = when {
            settings.keepScreenAwake || settings.screenTimeout == ScreenTimeout.NEVER -> Long.MAX_VALUE
            settings.screenTimeout == ScreenTimeout.ONE_MINUTE -> 60_000L
            settings.screenTimeout == ScreenTimeout.FIVE_MINUTES -> 300_000L
            else -> 0L
        }
        val protectionDeadline = if (config.enabled && config.releaseAfterMinutes > 0) {
            config.releaseAfterMinutes * 60_000L
        } else Long.MAX_VALUE
        val deadline = minOf(timeout, protectionDeadline)
        val released = (timeout in 1 until Long.MAX_VALUE && idle >= timeout) ||
            (protectionDeadline != Long.MAX_VALUE && idle >= protectionDeadline)
        return DisplayDecision(
            if (released) DisplayPhase.RELEASED else displayPhase,
            brightness,
            idle < deadline,
            released,
            config.enabled && config.ambientLight && light.isDark(now) &&
                target != null && target <= config.minimumPercent / 100f + 0.0001f &&
                (brightness ?: 1f) <= config.minimumPercent / 100f + 0.0001f,
        )
    }

    private fun sample(now: Long): Float? {
        val target = destination ?: return null
        val fraction = if (fadeDuration == 0L) 1f else
            ((now - fadeStarted).toFloat() / fadeDuration).coerceIn(0f, 1f)
        return (fadeFrom + (target - fadeFrom) * fraction).coerceIn(0.01f, 1f)
    }
}

/** Time-based smoothing also advances between events from an on-change TYPE_LIGHT sensor. */
class AmbientLightFilter {
    private var measurement: Float? = null
    private var filtered: Float? = null
    private var filteredAt = 0L
    private var candidate = 1f
    private var candidateSince = 0L
    private var accepted = 1f
    private var acceptedAt = Long.MIN_VALUE / 2
    private var darkCandidate = false
    private var darkCandidateSince = 0L
    private var dark = false

    fun observe(now: Long, lux: Float) {
        if (!lux.isFinite() || lux < 0f) return
        advance(now)
        measurement = lux
        if (filtered == null) {
            filtered = lux
            filteredAt = now
        }
        settle(now)
    }

    fun factor(now: Long): Float {
        advance(now)
        return if (dark) 0f else accepted
    }

    fun isDark(now: Long): Boolean {
        advance(now)
        return dark
    }

    private fun advance(now: Long) {
        val raw = measurement ?: return
        val previous = filtered ?: return
        val dt = (now - filteredAt).coerceAtLeast(0)
        val alpha = (1.0 - exp(-dt / 5000.0)).toFloat()
        filtered = previous + alpha * (raw - previous)
        filteredAt = now
        settle(now)
    }

    private fun settle(now: Long) {
        val value = filtered ?: return
        val next = when {
            value < if (candidate == 0.45f) 18f else 12f -> 0.45f
            value > if (candidate == 1f) 200f else 300f -> 1f
            else -> 0.7f
        }
        if (next != candidate) {
            candidate = next
            candidateSince = now
        }
        if (abs(candidate - accepted) > 0.01f &&
            now - candidateSince >= 5000 && now - acceptedAt >= 10_000) {
            accepted = candidate
            acceptedAt = now
        }

        // Near darkness is separate from the ordinary dim-room brightness bucket.
        val nextDark = if (darkCandidate) value < 3f else value <= 1f
        if (nextDark != darkCandidate) {
            darkCandidate = nextDark
            darkCandidateSince = now
        }
        val dwell = if (darkCandidate) 10_000L else 5000L
        if (dark != darkCandidate && now - darkCandidateSince >= dwell) dark = darkCandidate
    }
}
