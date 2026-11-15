# StandBy Extended

[English](README.md) | [Русский](README.ru.md)

This build uses application ID and namespace `com.mrbastim.standby.extended`, so it can be installed alongside the original StandBy (`com.emretenoz.standby`). Each app keeps its own settings; existing StandBy settings are not automatically transferred.

A polished, native Android ambient display inspired by iPhone StandBy. While the app is open, it watches charging and orientation state. When automatic mode is enabled and the device is both charging and in landscape, it enters an immersive, AMOLED-black StandBy interface. An Android `DreamService` provides the same experience after display timeout on supported devices.

The four horizontally swipeable pages show:

- Two side-by-side, vertically swipeable widget stacks
- A customizable full-screen clock
- An editorial calendar view
- A visual battery and charging view

Five clock faces are included: Digital, Digital Split, Analog, Solar / Orbit, and World Clock. The secondary world-clock city can be selected from eleven timezone-safe presets. Long-press the full-screen clock to open the live customization surface, where theme, face, clock color, seconds, 12/24-hour time, and date visibility can be previewed before the theme is saved.

The theme engine includes Classic, Midnight, Crimson Night, Sunset, Forest, Neon, Mono Light, and Custom themes. Every theme supplies centralized background, primary, secondary, accent, clock, analog-marker, separator, and icon tokens; screens and widgets consume these tokens instead of hard-coded colors. Day and Night Mode themes can be chosen independently. Mono Light is intentionally unavailable as a night theme, and Night Mode safely falls back to Crimson Night if an older preference requests it.

Custom themes expose separate background, primary, secondary, and accent colors. Five restrained background treatments are available: Solid, Radial, Vignette, Aura, and Grain. They remain subtle and contain no animated effects or gradients in the default Solid style. Typography can follow each theme automatically or use Modern, Editorial, Rounded, Condensed, or Mono styling.

Settings include automatic activation, screen timeout behavior, keeping the screen awake, theme and background choices, clock appearance, manual Night Mode, and OLED burn-in protection. Theme choices, custom colors, typography, and widget styles are persisted with DataStore Preferences.

The widget page follows the dual-stack model: swipe either half vertically to change its widget, or long-press a stack to open an editor directly over StandBy. Clock, date, battery, charging, world-clock, next-alarm, and weather widgets can be independently added, removed, and reordered. The next-alarm widget reads Android’s system alarm schedule without an additional permission. Weather is opt-in and uses the selected world-clock city. At least one widget is retained in each stack. Clock widgets provide Minimal, Bold, Editorial, and Compact styles; date widgets provide Numeric, Editorial, Calendar, and Minimal styles; battery widgets provide Circular, Horizontal, Percentage, and Minimal styles. Tap the style label beside a widget in the stack editor to cycle its presentation, or select it from Settings.

Night Mode switches between the independently selected day and night themes. Crimson Night provides the dim red, true-black OLED treatment; other night themes keep their own palettes. Pixel Shifting is enabled by default and moves page content through five positions at ±3 dp once per minute, with reserved edge space and no continuous movement animation.

The settings and customization surfaces are available in English, Russian, and Turkish. Interactive ambient controls expose accessibility labels and use enlarged touch targets.

## Display & AMOLED Protection

Open **Settings → Display & AMOLED Protection → Show display controls**. Protection 2.0
starts StandBy at 35% window brightness, waits for 2 minutes of inactivity, then fades
to 8% over 12 seconds. Brightness limits, delay and fade duration are configurable
and saved in DataStore. Touch restores active brightness smoothly and restarts inactivity;
clock ticks, widget updates and recomposition do not restart it.

Optional ambient-light adaptation uses TYPE_LIGHT with filtering, hysteresis and rate
limiting. Without a sensor, configured brightness remains usable. Inactivity takes
priority over illumination. Ambient hides seconds, including the alarm countdown, while
preserving the selected theme. With light adaptation enabled, automatic Night Mode requires
sustained near darkness (about 0–1 lux for 10 seconds) and the configured minimum brightness.
Inactivity alone never changes the theme; manual Night Mode remains available.

Keep Awake and Screen Timeout remain available in the same section. An optional
additional inactivity deadline overrides Keep Awake: the Activity releases its screen
hold and Android applies its normal timeout; DreamService ends the screen saver.
This does not guarantee immediate screen-off. Each resumed window has an independent
session; leaving StandBy restores its previous brightness and releases subscriptions.

Protection uses per-window Android brightness, never global Settings.System writes
or a dark overlay. Percentages are brightness requests, not calibrated nits. OEMs may
restrict brightness and screen-saver behavior. These measures reduce OLED wear risk
without guaranteeing prevention of burn-in.

See [architecture and policy](docs/amoled-protection.md) and
[automated and device testing](docs/testing.md).

## Activation and Android limitations

StandBy activates when all three conditions are true:

1. The app is in the foreground.
2. Android reports that the battery is charging (AC, USB, or wireless).
3. The orientation sensor or Android configuration reports landscape orientation. This also works when system auto-rotate is disabled, provided the device has an orientation sensor.

An ordinary Android app cannot reliably launch an activity from the background whenever power is connected. Modern Android background-activity launch restrictions intentionally prevent this, so this MVP does not attempt unreliable workarounds. Open the app before docking or charging; it will react immediately to charging and rotation changes while it is running.

For activation after the display times out or locks, the app also provides an Android `DreamService`. In the app, tap **Open Android screen saver settings**, select **StandBy Extended**, and configure the system screen saver to start **while charging**. Android then owns activation and can safely show StandBy after screen timeout without background-activity hacks. Menu wording and availability vary by device manufacturer; some OEMs disable or replace Android screen savers.

The system-state and UI layers are separated so future versions can add an opt-in foreground service with a persistent notification, a notification action, OEM-specific integration, or another user-granted capability without rewriting the StandBy UI or activation logic.

Immersive mode hides system bars while StandBy is active. Android may temporarily reveal them after an edge swipe, and device policy or OEM behavior can limit fullscreen behavior.

While the activity is already running and StandBy is active, it requests permission to remain visible above the keyguard using Android's supported `setShowWhenLocked` API. The keep-awake setting prevents normal screen timeout. Deliberately turning the display off with the physical power button is still respected; the app does not use wake-lock or background-launch workarounds to override an explicit user lock action.

## Project structure

```text
app/src/main/java/com/mrbastim/standby/extended/
├── MainActivity.kt                  Activity and window behavior
├── StandByDreamService.kt           System screen saver using shared StandBy UI
├── data/                           Settings and weather repositories
├── system/                         Charging, orientation, and next-alarm observers
└── ui/
    ├── SettingsScreen.kt            Categorized Material 3 settings
    ├── StandByViewModel.kt          Combined observable UI state
    ├── standby/                     Pager, theme engine, clock faces, widgets, editors
    └── theme/                       Application color theme
```

`ChargingObserver` reads the sticky `ACTION_BATTERY_CHANGED` broadcast, checks `BatteryManager.isCharging` as an OEM fallback, and continues listening for updates. `OrientationObserver` combines physical orientation-sensor readings with Android configuration callbacks. `SystemStateRepository` exposes both as `StateFlow`; `StandByViewModel` combines them with persisted settings into a single UI state.

## Development branch

`main` is the stable branch; merge feature branches into `dev` for integration.
Every push to `dev`, including a merge, runs tests and lint, builds a signed APK and
publishes a separate GitHub **prerelease** tagged `dev-<run>-<short-sha>`.
The stable Latest release is retained. See [pipeline setup](docs/github-release-pipeline.md).

## Build and run

Requirements: Android Studio with JDK 17 and Android SDK 35 or newer.

1. Open this directory in Android Studio.
2. Allow Gradle sync to finish.
3. Run the `app` configuration on a physical device or emulator running Android 9 (API 28) or later.
4. Open StandBy Extended, connect power, and rotate the device to landscape.
5. For locked/timeout activation, use the in-app button to select StandBy Extended as Android's charging screen saver.

From a terminal:

```shell
./gradlew testDebugUnitTest lintDebug assembleDebug
```

On Windows, use `gradlew.bat testDebugUnitTest lintDebug assembleDebug`.

The same verification runs in GitHub Actions for pushes and pull requests. Release builds enable R8 optimization and resource shrinking; signing credentials must remain outside the repository.

## Privacy

StandBy does not request notification, location, storage, calendar, or contacts access and includes no analytics or advertising SDK. Online weather is disabled by default; when enabled, the app sends the selected preset city's coordinates to [Open-Meteo](https://open-meteo.com/) to fetch current conditions. It never reads the device's location. All other charging, alarm, orientation, preference, and timezone processing remains on the device.

## Project status

The project is under active development. See [CHANGELOG.md](CHANGELOG.md) for unreleased changes and [CONTRIBUTING.md](CONTRIBUTING.md) for the verification and release checklist, and [release publishing instructions](docs/github-release-pipeline.md) for GitHub Release setup. The source is available under the [MIT License](LICENSE).
