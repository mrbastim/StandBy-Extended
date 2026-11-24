# Changelog

[English](CHANGELOG.md) | [Русский](CHANGELOG.ru.md)

All notable changes to StandByMode are documented here.

## 1.1.0 — StandByMode

### Breaking change

- **New Android app identity:** `com.mrbastim.standbymode` replaces `com.mrbastim.standby.extended`. Android installs this as a **separate app**, not as an in-place update of v1.0.6. Existing settings are not transferred. The old installation can be removed manually when no longer needed.
- The GitHub repository and visible app name are renamed to **StandByMode**. Project sources and tests use the new Kotlin package.

### Changed

- Version name `1.1.0` and Android `versionCode 8`.
- New stable APK name: `standbymode.apk`. New development APK name: `standbymode-dev.apk`.
- Release metadata checks explicitly allow the one-time package migration from v1.0.6, while continuing to require an increased version code.

## 1.0.6

### Added

- Exit controls for the system screen saver and automatic StandBy inside the app. Touch reveals the button for five seconds. Exiting automatic StandBy returns to Settings until the next activation session.
- Moscow, Saint Petersburg, Kazan, Yekaterinburg, Novosibirsk and Vladivostok in the shared clock and weather city list.

### Changed

- Clearer Protection 2.0 setting names and descriptions, current values next to titles, readable durations, dim-delay presets and custom input.
- Updated English, Russian and Turkish labels, with previews for compact landscape screens and larger system text.

### Fixed

- Clock seconds fit inside a two-widget column, including at increased font sizes.
- Brightness controls keep standby brightness at or below startup brightness.


### Fixed

- Shortened inactivity delays keep elapsed time after recent frames and during dimming.
- Ambient-light adaptation is disabled in Settings when TYPE_LIGHT is unavailable.
- Release signing runs separately from Gradle with an owner-only publication guard and an environment; owner setup is required for branch/secret protection.
- CI launches the minified release APK twice on an API 28 emulator before publication.

- Release startup crash (`LocalLifecycleOwner not present`) caused by R8 removing the Compose 1.6 reflection bridge used by Lifecycle 2.8.

### Added

- A `dev` integration branch with one rolling prerelease (`dev`), refreshed after successful checks and excluded from stable Latest. APKs use `standby-android-extended.apk` for stable and `standby-android-extended-dev.apk` for development.
- Sensor-confirmed automatic Night Mode only in sustained near darkness at the configured minimum brightness.

- English and Russian versions of the README, contributor guide, architecture, testing and release documentation.

- AMOLED Protection 2.0 with independent per-window brightness (35% initial / 8% minimum), configurable inactivity delay (120 s) and fade duration (12 s).
- Shared Activity/DreamService display policy, lifecycle cleanup, smooth touch recovery and optional filtered TYPE_LIGHT adaptation.
- Optional inactivity deadline that releases Activity screen retention or finishes DreamService, subject to Android/OEM sleep behavior.
- Collapsible Display & AMOLED Protection settings with persisted controls and English, Russian and Turkish strings.
- Pure policy/filter tests plus Robolectric window, sensor and lifecycle checks for API 28 and 35.
- Architecture notes and a manual device verification matrix; physical AMOLED protection is not guaranteed.

- Android CI for unit tests, lint, and debug assembly.
- Unit coverage for activation, widget-list invariants, and theme fallback behavior.
- English and Turkish UI resources.
- Accessibility labels and larger interaction targets for ambient customization controls.
- Dependabot configuration and a pull request verification checklist.
- A selectable secondary world-clock city with timezone-safe presets.
- A permission-free next-alarm widget backed by Android’s system alarm schedule.
- MIT licensing for reuse and redistribution.
- An opt-in current-weather widget powered by Open-Meteo and the selected world-clock city.
- A compact live alarm countdown card on the lower-left of the second clock page.
- A shorter, background-matched pill treatment for the alarm countdown card.
- A box-free minimal alarm indicator containing only the clock icon and countdown.

### Changed

- Ambient preserves the selected day/night palette and limits clock/alarm countdown updates to minutes without changing saved preferences.
- Pixel Shifting keeps its five-position ±3 dp / 60 s pattern and reserves edge space around all pager content, including music.
- Keep Awake and Screen Timeout share the display policy; finite timeouts now restart on real interaction.

- Application namespace and ID are now `com.mrbastim.standby.extended`.
- Release builds enable code and resource shrinking.
- The stack editor and customization rows adapt better to compact landscape displays.
- Night mode copy now reflects the selected night theme instead of promising a fixed red palette.

### Fixed

- Inactivity dimming no longer forces Crimson Night; manual Night Mode and sensor-confirmed darkness are independent of the inactivity timer.
- Light filtering advances between on-change sensor events, allowing recovery when a new steady lux value produces no further events.

- Rebind Activity-owned system-state observers after Activity recreation so a retained ViewModel does not keep stopped flows.

- Missing OEM screen-saver settings no longer crash the app.
- Recoverable DataStore read failures fall back to safe defaults.

### Known installation limitation

- Google Play Protect may block installation from a downloaded APK because music controls use notification-listener access. The signed development APK was installed and launched through Android Studio debugging on realme XT with Android 13.
