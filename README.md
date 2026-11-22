# StandBy Extended

**Turn your Android phone into a beautiful desk clock and ambient dashboard.** Place your phone horizontally on a stand to see clocks, music, a calendar, weather, and useful widgets—all with a customizable look.

**Android 9+ · No root required · Free · Open source**

**[⬇ Download the latest stable APK](https://github.com/mrbastim/StandBy-Extended/releases/latest/download/standby-android-extended.apk)** · [All releases](https://github.com/mrbastim/StandBy-Extended/releases) · [Русский](README.ru.md)

> StandBy Extended is an independent version of [StandBy-Android](https://github.com/emretenoz/StandBy-Android). It can be installed alongside the original app; settings are not automatically transferred.

## Get started

1. **Install the app.** Download the APK above and open it on your phone. Android may ask you to allow installation from the chosen source.
2. **Try it without a charger.** Launch StandBy Extended and tap **Preview StandBy** to explore the interface and customize its appearance.
3. **Use automatic activation.** Keep the app open, connect a charger, and turn the phone to landscape. With **Automatic activation** enabled, StandBy appears automatically.
4. **Optionally enable the Android screen saver.** Tap **Open Android screen saver settings** in the app, select **StandBy Extended**, and configure it to run while charging. Some manufacturers do not expose this option.

**What's the difference?** Automatic activation works while the app is open. The **Android screen saver** can start after the display timeout if your device supports it. The app does not bypass Android's restrictions to launch itself from the background whenever a charger is connected.

## Features

- **Four pages:** two independently scrollable widget columns, a full-screen clock, a calendar, and battery information.
- **Five clock faces:** Digital, Digital Split, Analog, Solar / Orbit, and World Clock with a selectable second city.
- **Useful widgets:** clock, date, battery, charging, world clock, next alarm, weather, and music playback controls.
- **Personalization:** preset themes, independent day and night palettes, custom colors, clock styles, and backgrounds.
- **AMOLED Protection 2.0:** idle dimming, optional light-sensor adaptation, and subtle periodic pixel shifting.

### Controls

| Gesture | Action |
| --- | --- |
| Swipe left or right | Switch between the four pages |
| Swipe up or down inside a widget column | Switch widgets in that column |
| Long-press a widget column | Edit its widgets |
| Long-press the full-screen clock | Customize the clock and theme |
| Touch the Android screen saver | Reveal the **Exit** control briefly |

Use the **×** button to leave **Preview**. Regular StandBy mode also provides a way back to the app's settings. To close the Android screen saver, use its **Exit** control; Android decides which screen appears afterward.

## Music and weather

**Music:** The optional widget displays the currently playing track from a compatible media app and can control playback. Android requires you to explicitly enable **notification access** so StandBy can discover other apps' media sessions. You can leave this permission disabled if you don't use the widget.

**Weather:** Off by default. When enabled, it requests current conditions for your selected World Clock city from [Open-Meteo](https://open-meteo.com/). It does not need your device location.

## AMOLED protection

By default, StandBy starts at **35% requested brightness**, waits **2 minutes** without interaction, then fades to **8%** over **12 seconds**. Touching the display restores the active brightness. You can change these values under **Display & AMOLED Protection**.

You can also enable ambient-light adaptation (if the device has a light sensor), use a night palette, and shift page content slightly once a minute. Brightness percentages are Android window requests, not calibrated screen luminance. These measures can *reduce the risk* of OLED burn-in, but cannot guarantee prevention.

[How AMOLED protection works](docs/amoled-protection.md)

## FAQ

**Why doesn't StandBy appear when I plug in my phone?**  
Make sure the app is open, Automatic activation is enabled, Android reports the device is charging, and the phone is in landscape. To start StandBy after display timeout, configure the system screen saver.

**Can I use it without charging?**  
Yes, in Preview. Automatic activation in normal mode requires charging.

**Why doesn't the screen saver start after locking the phone?**  
Check Android's screen-saver settings. Some manufacturers disable or change screen-saver support.

**Why can't I see my music?**  
Start playback in another app and enable notification access for StandBy Extended if prompted. Not every media app exposes every control.

**Why doesn't the screen turn off at the exact timeout?**  
StandBy can release its screen-awake request or finish the screen saver, but Android controls when the physical display turns off.

**Can I install it alongside the original StandBy?**  
Yes. Extended has a different application ID and maintains separate settings.

## Privacy

StandBy Extended has no advertising or analytics SDKs. Internet access is used for the optional weather widget. Notification access is an optional, user-enabled feature for music controls. The described features do not need your location, contacts, files, or calendar. Preferences are stored on-device.

## For developers

Built with **Kotlin and Jetpack Compose**. Building requires JDK 17 and Android SDK 35 or newer:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

Documentation: [display protection architecture](docs/amoled-protection.md) · [testing](docs/testing.md) · [release pipeline](docs/github-release-pipeline.md) · [contributing](CONTRIBUTING.md) · [changelog](CHANGELOG.md).

**Branches:** [`main`](https://github.com/mrbastim/StandBy-Extended/tree/main) contains stable code; [`dev`](https://github.com/mrbastim/StandBy-Extended/tree/dev) is for development. The [experimental APK](https://github.com/mrbastim/StandBy-Extended/releases/tag/dev) may include unfinished changes.

Based on [StandBy-Android](https://github.com/emretenoz/StandBy-Android). Released under the [MIT License](LICENSE).
