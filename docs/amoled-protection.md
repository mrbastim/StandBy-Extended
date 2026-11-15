# AMOLED Protection 2.0

[English](amoled-protection.md) | [Русский](amoled-protection.ru.md)

## Previous implementation and scope

Previously, Activity held the screen with FLAG_KEEP_SCREEN_ON and measured finite
timeouts from mode activation. Touch did not restart that timer. DreamService always
enabled isScreenBright, ignoring Keep Awake and Screen Timeout. Neither host set
window brightness or changed global Android brightness. There was no shared brightness policy.

BurnInContainer already moved all HorizontalPager content through five positions
(center and four diagonals at ±3 dp) every 60 seconds. A music widget added to a stack
is inside this container. The pattern is retained, with 3 dp reserved along every edge.
It redistributes exposure around high-contrast edges but does not protect large filled
areas or guarantee prevention of burn-in. Editors temporarily disable shifting,
the page indicator disappears, and touch uses ordinary Compose layout and hit testing.

Changes are limited to display policy, settings, tests and a related Activity recreation
fix. No widgets, permissions, network integrations or background services were added.

## Components and lifecycle

- DisplayProtectionSettings: normalized configuration in the existing Preferences DataStore.
- DisplayProtectionPolicy: a pure Kotlin model of one session, receiving monotonic time.
- AmbientLightFilter: lux filtering, hysteresis, dwell time and decision rate limiting.
- AmbientLightSource: the SensorManager / TYPE_LIGHT subscription.
- DisplayProtectionController: one coroutine per window, a phase StateFlow,
  screenBrightness and Keep Awake application, and restoration of original attributes.
- DisplayProtectionHost: shared Compose integration for Activity and DreamService;
  starts the controller only while RESUMED and stops it on pause, exit or composition disposal.

Activity retains immersive-mode and keyguard handling. DreamService retains its
LifecycleRegistry and system screen-saver integration; isScreenBright follows the shared
policy. Each Window has its own controller. Stopping unregisters the sensor listener,
cancels the coroutine and restores the captured screenBrightness (normally -1, follow
system) and screen-retention flag.

A new session after resume, window recreation or screen-saver restart begins in Active.
Light history and inactivity timers are not shared between windows. The retained Activity
ViewModel receives fresh SystemStateRepository flows after recreation, so the destroyed
Activity's lifecycleScope cannot freeze charging or orientation updates. User preferences
remain in DataStore.

## Priorities and states

1. Normalize settings: initial brightness 1–100%, minimum 1–initial,
   delay 10–3600 seconds, fade 1–60 seconds, additional deadline 0–720 minutes.
2. Active: use the user's initial brightness, 35% by default.
3. With the sensor enabled, adjust Active within the configured range:
   minimum + (initial - minimum) × factor. A missing sensor or failed registration
   leaves factor = 1 and does not block manual brightness.
4. Idle: after 120 seconds of inactivity, fade linearly to the minimum over 12 seconds.
5. Ambient: minimum brightness, 8% by default, without seconds or smooth clock movement.
   The selected palette is preserved; inactivity does not enable Night Mode. Basic data, music and pages remain available;
   the alarm countdown changes to minute updates. Saved appearance settings are not overwritten.
6. Released: a finite retention deadline has expired. This is a policy state,
   not confirmation that the physical display has switched off.
7. Android, power-saving and OEM restrictions always have the final say.

Touch, including swipes and long presses, is observed in the Initial pass without
consuming pointer events; keys are observed through preview key events. Input restarts
inactivity and restores active brightness over 800 ms from the current value. Repeated
events do not restart an animation already heading toward the same target. Clock ticks,
weather, music updates, data changes and recomposition do not count as user activity.

Timing uses SystemClock.elapsedRealtime; wall-clock time is used only by widgets.
The single loop wakes every second while quiet and every 50 ms during a transition.
Input and setting changes wake it immediately through a conflated Channel.
There are no competing brightness animators.

## Light sensor

Adaptation is off by default. A sample interval of roughly one second is requested;
hardware determines the actual delivery rate. The exponential filter has a 5-second
time constant and advances between events, including on-change sensors that send only
one reading for steady light. Dim-room, indoor and bright-environment factors are 0.45 / 0.7 / 1. Hysteresis thresholds are 12/18 lux
and 200/300 lux. A new zone must persist for 5 seconds, and accepted changes are at least
10 seconds apart. Invalid lux values are ignored. Light readings never raise
Idle/Ambient brightness above the inactivity target.

Automatic Night Mode is separate from inactivity. Filtered illumination at or below 1 lux
must persist for 10 seconds. This requests the configured minimum brightness (factor = 0);
the configured night palette is selected only after the brightness fade reaches that minimum.
Exit requires filtered illumination at or above 3 lux for 5 seconds. These thresholds are
conservative sensor readings, not a guarantee of room illumination or panel luminance.
Without a sensor, with adaptation disabled, or with Protection disabled, there is no automatic
night switch. Manual Night Mode remains in force independently. No saved theme is overwritten.

## Keep Awake and Screen Timeout

| Setting | Activity | DreamService |
| --- | --- | --- |
| Keep Awake enabled, or Timeout = Never | Retains the screen | Enables isScreenBright |
| Keep Awake disabled, Follow system | Releases retention immediately | Disables isScreenBright; Android controls sleep |
| Keep Awake disabled, 1 / 5 minutes | Retains until 1 / 5 minutes of inactivity, then releases | Calls finish() when the deadline expires |
| Additional Protection deadline | Releases retention even with Keep Awake / Never | Finishes the screen saver |

When two finite deadlines apply, the earlier one wins. The additional deadline is off
by default; the UI offers 15–720 minutes. The policy also accepts a one-minute interval
for tests. Disabling Protection 2.0 retains the existing retention settings and the
independent Pixel Shifting switch.

After Activity releases retention, the system timeout still applies. Android may start
the configured system screen saver, whose session and timer are independent. Finishing
DreamService also does not guarantee immediate sleep: Android/OEM behavior, preview mode
and charging settings affect the outcome. The app does not use goToSleep, root,
Device Admin, partial wake locks or Settings.System changes.

## Limitations and verification

Percentages are WindowManager.LayoutParams.screenBrightness requests, not physical nits.
OEMs may limit minimum brightness and override DreamService behavior. A user-selected black
theme reduces the lit area but does not replace hardware brightness reduction.
No translucent black overlay is used.

JVM/Robolectric results do not prove reduced wear on a physical panel. Before prolonged
use, verify the target device using the [test plan](testing.md).

Official contracts:
[screenBrightness](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#screenBrightness),
[DreamService](https://developer.android.com/reference/android/service/dreams/DreamService),
[SensorManager and lifecycle](https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview).
