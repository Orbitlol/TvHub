# Hearth

A small, calm, fully offline Android TV launcher — a quiet replacement for PatchWall.
Dark soft UI, D-pad only, no ads, no analytics, no INTERNET permission.

## What it does

- **Home** — greeting and clock, then **Favorites**, **Recent apps** and an alphabetical **All apps** grid.
- **Enter** opens an app. **Hold Enter** (or long-press) opens **Pin / Unpin, App info, Uninstall**.
- **Quick settings** tiles: **Inputs** (HDMI / AV / TV sources), Wi-Fi, Display, Sound, Settings.
- **Inputs** lists the sources the TV reports, and switches to them via the standard
  TV-input passthrough link, falling back to the TV's own source menu (Xiaomi's
  `ExternalSourceActivity`, then the AOSP `VIEW_INPUTS` picker).
- **Wallpaper** picker with five built-in soft gradients, remembered in SharedPreferences.
- Lists every installed app exposing `LEANBACK_LAUNCHER` or `LAUNCHER`, with banner or icon.
- Refreshes itself when apps are installed, removed or updated.

## Build

APK is built by GitHub Actions (`.github/workflows/build.yml`) on every push:
download the `hearth-debug-apk` artifact from the workflow run.

Locally (Android Studio or a JDK 17 machine with Gradle 8.7):

```bash
gradle assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Set it as the home screen: **Settings → Apps → Default apps → Home app → Hearth**
(or just press Home and pick Hearth).

## Stack

Kotlin 1.9.24 · AGP 8.5.2 · Gradle 8.7 · minSdk 21 · target/compileSdk 34 · Java 17.
Plain Views + RecyclerView (DiffUtil), a single background executor, no image library,
no Compose, no network. All art is vector XML — no PNGs anywhere.
