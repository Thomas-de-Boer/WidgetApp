# WidgetApp

An Android app with five home screen widgets: quotes, your own photos, generated art, mazes and a Plinko game. Everything runs on your phone. There is no account and no server.

## Widgets

| Widget | What it does |
|---|---|
| **Quote/Fun Fact** | Shows a new quote or fun fact every so often. You choose which kinds you want to see. |
| **Image** | Shows your own photos, one after another. You upload them in the app, and they stay on your phone. |
| **Generated Images** | Shows animated art made from noise (value, Perlin and cell noise). Every image is new. You choose how it looks. |
| **Maze** (3×3) | Makes a maze with exactly one way out. Tap **Solve** to watch the path being drawn, or **New maze** for another one. It makes a new maze by itself every 30 minutes. |
| **Plinko** (4×3) | A game. Tap to drop a ball through the pins, build chains and climb the levels. |

### Plinko

- **Dropping balls:** tap anywhere on the widget to drop a ball. Every level gives you 5 balls and a number of points to reach.
- **Slots:** the slots give `+4 +3 +2 +1 −1 +1 +2 +3 +4`. The middle slot (−1) removes the ball. The other slots give that many points, and the ball turns into that many new balls, which drop from the top again.
- **Chains:** all balls that come from one of your drops are a chain. A long chain multiplies the points: ×2 from 15 balls, then ×3, ×5 and ×10.
- **Bonus pin:** every level, one pin glows. When a ball you dropped bounces off it, you get an extra ball to drop.
- **Lives:** you have 3. A failed level costs one, and when they're gone you start again at level 1. The star shows the highest level you ever reached.
- **Settings:** you can choose the colors, turn **Quick finish** on or off, and reset your progress.

## Installing

1. Download `app-release.apk` from the [latest release](https://github.com/Thomas-de-Boer/WidgetApp/releases/latest).
2. Open it on your phone and allow installing from that source.

You need Android 8.0 (API 26) or newer.

To add a widget, touch and hold an empty spot on your home screen, tap **Widgets**, find **Widget App**, then drag a widget onto your home screen.

## Updating

When you open the app, it checks GitHub for a new release. If there is one, a bar appears at the top. Tap **Update** to download and install it. Your settings, photos and widgets are kept.

## Backup

In the app's settings you can export all your settings and uploaded photos to one file, and import that file again later, for example on a new phone.

## Building

The app is written in Java and built with Gradle. Open the project in Android Studio, or run these from the command line:

```sh
./gradlew :app:assembleDebug        # build a debug APK
./gradlew :app:testDebugUnitTest    # run the unit tests
```

Release APKs are signed in Android Studio with **Build → Generate Signed App Bundle / APK**. Each new version needs a higher `versionCode` and `versionName` in `app/build.gradle.kts`, and a GitHub release with the APK attached. The in-app updater downloads the APK from the latest release.

## Project layout

```
app/src/main/java/com/tdeboer/widgetapp/
├── widgets/
│   ├── quote/    Quote/Fun Fact widget
│   ├── image/    Image widget
│   ├── gen/      Generated Images widget (noise renderer, image store, job service)
│   ├── maze/     Maze widget
│   └── plinko/   Plinko widget (game logic in PlinkoBoard, drawing in PlinkoRenderer)
├── fragments/    app screens
├── preferences/  settings screens per widget
├── update/       in-app updater (GitHub releases)
└── DataBackup.java, SettingsManager.java, ...
```
