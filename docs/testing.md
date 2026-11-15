# Testing AMOLED Protection 2.0

[English](testing.md) | [Русский](testing.ru.md)

## Test stack and verification boundaries

The app uses Kotlin 2.0.21, Compose, AGP 8.5.2, Gradle 8.7, JDK 17,
compile/target SDK 35 and minSdk 28. Before this change, the project had only JUnit 4.13.2
pure-logic tests; no DI, mocking, UI, screenshot or end-to-end harness was configured.

JUnit4 is retained. Only Robolectric 4.14.1 was added for window, SensorManager and lifecycle
simulation on API 28 and 35. This version works with the existing stack; production
dependencies, AGP and the app version were not upgraded. A broad Hilt migration or new
screenshot-testing system is unnecessary for the isolated display policy.

- DisplayProtectionPolicyTest / AmbientLightFilterTest: plain JVM tests of phases,
  timers, bounds, setting changes, touch recovery, rapid lux changes, hysteresis,
  missing sensors and independent sessions.
- DisplayProtectionAndroidTest: Window and coroutine simulation with the Android main
  looper, attribute restoration, intermediate brightness values, a fresh Activity session,
  controller restart and DreamService lifecycle events.
- AmbientLightAndroidTest: missing sensors, registration failures, idempotent
  subscriptions, disabling adaptation and listener cleanup on stop.
- DisplayProtectionSettingsAndroidTest: DataStore persistence of every parameter,
  normalization of invalid values and preservation of other preferences.

Simulating a fresh window does not replace real Compose Activity recreation. The
DreamService lifecycle test does not create a system dream session through DreamManager.
Those integrations and physical brightness require separate device testing.

## Verification results: November 15, 2026

- Before the changes, testDebugUnitTest, lintDebug and assembleDebug passed.
- For the completed implementation, testDebugUnitTest (including Robolectric on API 28/35),
  lintDebug, assembleDebug and assembleRelease with R8/resource shrinking passed.
- Gradle sync and Settings/StandBy Compose Preview rendering succeeded.
- git diff --check found no whitespace errors.
- No Android devices were running in Android Studio during the automated verification.
  Physical AMOLED behavior, system DreamService activation and real Activity recreation
  were not tested.
- Later, the debug build was installed and launched on a realme RMX1921 through Android Studio.
  Package com.mrbastim.standby.extended, versionCode 6 and the main StandBy screen were confirmed.
  This is an installation/startup smoke check, not the complete brightness, lifecycle
  or physical-panel test matrix.
- Android CI configuration was checked: pushes to main / pull_request,
  testDebugUnitTest lintDebug assembleDebug assembleRelease, JDK 17.
  Remote GitHub Actions results were not confirmed: gh was unavailable and the browser
  tool could not access the GitHub API. No publishing or remote CI run was performed.

## Automated commands

From the repository root:

```shell
./gradlew --console=plain testDebugUnitTest lintDebug assembleDebug
./gradlew --console=plain assembleRelease
```

The first Robolectric run may need to download Android runtimes for API 28/35.
Test report: app/build/reports/tests/testDebugUnitTest/index.html.
Lint: app/build/reports/lint-results-debug.html.
APK: app/build/outputs/apk/debug/app-debug.apk.
Release assembly additionally checks R8/resource shrinking; it does not publish a release.

Run only the pure protection logic:

```shell
./gradlew testDebugUnitTest --tests '*DisplayProtectionPolicyTest' --tests '*AmbientLightFilterTest'
```

## Manual Android matrix

Cover API 28 and a recent Android version, a device without TYPE_LIGHT,
an OLED/AMOLED phone and at least one OEM implementation supporting DreamService.
For the first pass, use a short 10–20 second inactivity delay, then restore the
120-second delay and 12-second fade. The following steps are instructions,
not claims of completed physical-panel testing.

1. System brightness: record the current setting, start StandBy at 35% and let it
   reach 8% after inactivity. Exit preview, background the app, and open system
   settings and another app. The global brightness setting must remain unchanged.
2. Transitions: touch at the start, middle and end of dimming; try repeated taps,
   swipes and long presses. There should be one smooth recovery, followed by
   dimming after a new full inactivity period.
3. Data sources: leave clock/music/weather updating without touch. Ambient must
   start on schedule; seconds and smooth movement disappear, and the alarm countdown
   shows hours/minutes. The selected theme must remain unchanged in an illuminated room.
   Touch restores the normal update frequency.
4. Illumination: enable adaptation, change the light gradually, then briefly cover
   the sensor. A brief spike must not cause a jump. Sustained darkness lowers
   active brightness within the configured bounds. Bright light must not override
   the Ambient minimum. In near total darkness (filtered ≤1 lux for 10 seconds), brightness
   reaches the configured minimum before the selected night palette activates. In a dim room
   above that threshold, inactivity must never select the night palette. Returning light
   (filtered ≥3 lux for 5 seconds) restores the day palette even while idle brightness stays low.
   Repeat with adaptation disabled and without a sensor: no automatic night switching.
   Verify manual Night Mode still selects the configured night theme.
5. Missing sensor: check the explanation and manual brightness. Disabling adaptation,
   backgrounding the app and stopping the screen saver must release the TYPE_LIGHT
   listener; inspect with Android Studio Profiler / dumpsys sensorservice if needed.
6. Timeouts: test Keep Awake on/off × Follow system/1 min/5 min/Never, then the
   additional 15-minute deadline. Keep Awake overrides the ordinary timeout; the
   additional deadline explicitly releases retention. Activity then waits for the
   system timeout, while the screen saver finishes.
7. Activity recreation: enable Android's “Don't keep activities”, rotate the device,
   change language/window size, and close/reopen the app. A fresh window starts
   Active, preferences remain saved, and charging/orientation continue updating.
   Check lock/unlock, Home/return and the physical power button.
8. DreamService: select the screen saver in system settings and start it while
   charging. Check minimum brightness and touch recovery; stop/start it twice,
   then unplug power. After completion, there must be no retained subscriptions
   or forced brightness in another window. Test both preview and real timeout activation.
9. Pixel Shifting: cover all four pages and the music widget in both stacks;
   wait at least five minutes for each size/density group. Content must move without
   clipping, and touch targets must match its position. Include compact landscape,
   cutouts, large fonts, editors and disabling the feature.
10. Persistence: change each parameter, terminate the process and reopen the app.
    Values must remain saved. Disabling Protection restores system brightness;
    independent Pixel Shifting and Keep Awake settings still work.
11. Localization and accessibility: open the section in Russian, English and Turkish;
    check TalkBack labels for switches/sliders, keyboard control and readable values
    with large fonts.

Do not test burn-in by intentionally displaying high-brightness content for prolonged
periods. Observing brightness changes and lifecycle correctness does not prove that
burn-in is prevented.
