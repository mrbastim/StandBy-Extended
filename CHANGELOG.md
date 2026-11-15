# Changelog

[English](CHANGELOG.md) | [Русский](CHANGELOG.ru.md)

All notable changes to StandBy are documented here.

## Unreleased

### Added

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

- Ambient uses a black night theme and limits clock/alarm countdown updates to minutes without changing saved appearance.
- Pixel Shifting keeps its five-position ±3 dp / 60 s pattern and reserves edge space around all pager content, including music.
- Keep Awake and Screen Timeout share the display policy; finite timeouts now restart on real interaction.

- Application namespace and ID are now `com.emretenoz.standby`.
- Release builds enable code and resource shrinking.
- The stack editor and customization rows adapt better to compact landscape displays.
- Night mode copy now reflects the selected night theme instead of promising a fixed red palette.

### Fixed

- Rebind Activity-owned system-state observers after Activity recreation so a retained ViewModel does not keep stopped flows.

- Missing OEM screen-saver settings no longer crash the app.
- Recoverable DataStore read failures fall back to safe defaults.

### Removed

- Notification-listener and media-control access from the GitHub build to avoid sensitive-access blocking during sideloading.
