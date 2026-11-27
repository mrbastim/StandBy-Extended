<p align="center">
  <strong>English</strong> · <a href="README.ru.md">Русский</a>
</p>

<h1 align="center">StandByMode</h1>

<p align="center">
  Turn your Android phone into an elegant desk clock and ambient dashboard.
</p>

<p align="center">
  <img alt="Android 9+" src="https://img.shields.io/badge/Android-9%2B-3DDC84?logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white">
  <a href="LICENSE"><img alt="MIT License" src="https://img.shields.io/badge/License-MIT-blue"></a>
  <a href="https://github.com/mrbastim/StandByMode/releases/latest"><img alt="Latest release" src="https://img.shields.io/github/v/release/mrbastim/StandByMode?label=stable"></a>
</p>

<p align="center">
  <a href="https://github.com/mrbastim/StandByMode/releases/latest/download/standbymode.apk"><strong>⬇ Download APK</strong></a>
  &nbsp;·&nbsp;
  <a href="https://github.com/mrbastim/StandByMode/releases">All releases</a>
  &nbsp;·&nbsp;
  <a href="#get-started">Get started</a>
</p>

---

Place your phone horizontally on a stand to see **clocks, music, a calendar, weather and useful widgets** in a customizable, AMOLED-friendly interface.

**Free · Open source · No root required.**

> **Upgrading from StandBy Extended 1.0.6 or earlier?** StandByMode 1.1.0 uses a new Android application ID (`com.mrbastim.standbymode`, previously `com.mrbastim.standby.extended`). Android installs it as a separate app, not an update. Existing settings are not migrated; both apps may coexist. StandByMode also remains independent of [StandBy-Android](https://github.com/emretenoz/StandBy-Android).

## 🚀 Get started

1. **Install the app.** Download the APK above and open it on your phone. Android may ask you to allow installation from the chosen source.
2. **Try it without a charger.** Launch StandByMode and tap **Preview StandBy** to explore the interface and customize its appearance.
3. **Use automatic activation.** Keep the app open, connect a charger, and turn the phone to landscape. With **Automatic activation** enabled, StandBy appears automatically.
4. **Optionally enable the Android screen saver.** Tap **Open Android screen saver settings** in the app, select **StandByMode**, and configure it to run while charging. Some manufacturers do not expose this option.

**What's the difference?** Automatic activation works while the app is open. The **Android screen saver** can start after the display timeout if your device supports it. The app does not bypass Android's restrictions to launch itself from the background whenever a charger is connected.

## ✨ Features

- **Four pages:** two independently scrollable widget columns, a full-screen clock, a calendar, and battery information.
- **Five clock faces:** Digital, Digital Split, Analog, Solar / Orbit, and World Clock with a selectable second city.
- **Useful widgets:** clock, date, battery, charging, world clock, next alarm, weather, music playback controls, countdown timer, Pomodoro, and stopwatch.
- **Remembered widgets:** each column restores its last selected card; add and reorder all unique widget types independently.
- **Clock tools:** choose an alarm, timer, Pomodoro, stopwatch or None next to the full-screen clock; place the tool on either side.
- **Personalization:** preset themes, independent day and night palettes, custom colors, clock styles, and backgrounds.
- **AMOLED protection:** idle dimming, optional light-sensor adaptation, and subtle periodic pixel shifting.

### 👆 Controls

| Gesture | Action |
| --- | --- |
| Swipe left or right | Switch between the four pages |
| Swipe up or down inside a widget column | Switch widgets in that column |
| Long-press a widget column | Edit its widgets |
| Long-press the full-screen clock | Customize the clock and theme |
| Touch the Android screen saver | Reveal the **Exit** control briefly |

Use the **×** button to leave **Preview**. Regular StandBy mode also provides a way back to the app's settings. To close the Android screen saver, use its **Exit** control; Android decides which screen appears afterward.

## ⏱ Clock tools and alarms

Choose **Next to the clock** in Settings or long-press the full-screen clock to configure the tool and its side. Select **None** to use the whole screen for the clock.

The timer supports 1–180 minutes (default 5), Pomodoro has 25-minute focus and 5-minute break phases with manual transitions, and the stopwatch counts elapsed time. Each tool is also a separate widget. Start, pause and reset controls share one local session across both layouts; leaving StandBy does not pause it. Reboot resets sessions; if Android does not expose a boot count, process recreation also resets them. Completion is visual only, without background alarms or screen wake. Ambient hides the controls.

Android's next-alarm information may include inaccurate OEM alarm entries. To hide alarms entirely, select **None** (or another tool) next to the clock and turn off **Alarm in widgets and calendar**. Configured alarm cards are retained when hidden.

## 🎵 Music and weather

**Music:** The optional widget displays the currently playing track from a compatible media app and can control playback. Android requires you to explicitly enable **notification access** so StandBy can discover other apps' media sessions. You can leave this permission disabled if you don't use the widget.

**Weather:** Off by default. When enabled, it requests current conditions for your selected World Clock city from [Open-Meteo](https://open-meteo.com/). It does not need your device location.

## 🌙 AMOLED protection

By default, StandBy starts at **35% requested brightness**, waits **2 minutes** without interaction, then fades to **8%** over **12 seconds**. Touching the display restores the active brightness. You can change these values under **Display & AMOLED Protection**.

You can also enable ambient-light adaptation (if the device has a light sensor), use a night palette, and shift page content slightly once a minute. Brightness percentages are Android window requests, not calibrated screen luminance. These measures can *reduce the risk* of OLED burn-in, but cannot guarantee prevention.

[How AMOLED protection works](docs/amoled-protection.md)

## ❓ FAQ

<details>
<summary><strong>Why doesn't StandBy appear when I plug in my phone?</strong></summary>

Make sure the app is open, Automatic activation is enabled, Android reports the device is charging, and the phone is in landscape. To start StandBy after display timeout, configure the system screen saver.

</details>

<details>
<summary><strong>Can I use it without charging?</strong></summary>

Yes, in Preview. Automatic activation in normal mode requires charging.

</details>

<details>
<summary><strong>Why doesn't the screen saver start after locking the phone?</strong></summary>

Check Android's screen-saver settings. Some manufacturers disable or change screen-saver support.

</details>

<details>
<summary><strong>Why can't I see my music?</strong></summary>

Start playback in another app and enable notification access for StandByMode if prompted. Not every media app exposes every control.

</details>

<details>
<summary><strong>Why doesn't the screen turn off at the exact timeout?</strong></summary>

StandBy can release its screen-awake request or finish the screen saver, but Android controls when the physical display turns off.

</details>

<details>
<summary><strong>Can I install it alongside the original StandBy?</strong></summary>

Yes. StandByMode has a different application ID and maintains separate settings.

</details>

## 🔒 Privacy

StandByMode has no advertising or analytics SDKs. Internet access is used for the optional weather widget. Notification access is an optional, user-enabled feature for music controls. The described features do not need your location, contacts, files, or calendar. Preferences are stored on-device.

## 🛠️ For developers

Built with **Kotlin and Jetpack Compose**. Building requires JDK 17 and Android SDK 35 or newer:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

Documentation: [display protection architecture](docs/amoled-protection.md) · [testing](docs/testing.md) · [release pipeline](docs/github-release-pipeline.md) · [contributing](CONTRIBUTING.md) · [changelog](CHANGELOG.md).

**Branches:** [`main`](https://github.com/mrbastim/StandByMode/tree/main) contains stable code; [`dev`](https://github.com/mrbastim/StandByMode/tree/dev) is for development. The [experimental APK](https://github.com/mrbastim/StandByMode/releases/tag/dev) may include unfinished changes.

Based on [StandBy-Android](https://github.com/emretenoz/StandBy-Android). Released under the [MIT License](LICENSE).
