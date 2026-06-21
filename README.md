# Firefont

 **Firefont** is an [LSPosed](https://github.com/LSPosed/LSPosed) / [Xposed](https://github.com/rovo89/Xposed) module that forces Firefox on Android to use your system's custom fonts by disabling web fonts (`browser.display.use_document_fonts = 0`).

## Why?

Firefox Android (Fenix) hardcodes `browser.display.use_document_fonts = 1` and resets it every time the app starts, even if you change it in `about:config`. This module hooks into GeckoView's initialization and forces the preference to `0`, so your custom fonts are used everywhere.

## Supported Versions

| App       | Package Name            |
|-----------|-------------------------|
| Firefox Nightly | `org.mozilla.fenix` |
| Firefox         | `org.mozilla.firefox` |
| Firefox Beta    | `org.mozilla.firefox.beta` |

## How It Works

The module hooks three points in the GeckoView initialization chain, providing triple redundancy:

1. **`GeckoRuntimeSettings.setWebFontsEnabled(boolean)`** — intercepts the call from GeckoEngine and forces the parameter to `false`
2. **`GeckoRuntimeSettings(GeckoRuntimeSettings)` copy constructor** — after construction, uses reflection to commit `0` to the `mWebFonts` pref
3. **`GeckoRuntimeSettings()` default constructor** — same fallback

## Requirements

- Android 8.0 (API 26) or higher
- [LSPosed](https://github.com/LSPosed/LSPosed) or compatible Xposed framework
- Root access

## Installation

1. Download the latest APK from [Releases](https://github.com/HittyGubby/Firefont/releases)
2. Install it:
   ```sh
   adb install app-release.apk
   ```
3. Enable the module in **LSPosed** → **Modules** → check **Firefont**
4. Scope the module to your Firefox app(s):
   - `org.mozilla.fenix` (Nightly)
   - `org.mozilla.firefox` (Stable)
   - `org.mozilla.firefox.beta` (Beta)
5. Force stop and restart Firefox:
   ```sh
   adb shell am force-stop org.mozilla.fenix
   ```
6. Open Firefox, go to `about:config` and verify `browser.display.use_document_fonts` is `0`

## Building from Source

```sh
git clone https://github.com/HittyGubby/Firefont.git
cd Firefont
JAVA_HOME=/path/to/jdk17 ./gradlew assembleRelease
```

The signed APK will be at `app/build/outputs/apk/release/app-release.apk`.

## How It Was Discovered

The root cause is in the Fenix codebase:

- [`Settings.kt`](https://github.com/mozilla-mobile/firefox-android/blob/9f8e3ba718d922b3eb05fb7b3c263077ffaca20c/android-components/components/concept/engine/src/main/java/mozilla/components/concept/engine/Settings.kt#L216): `val useDocumentFonts: Boolean get() = true`
- [`DefaultSettings.java`](https://github.com/mozilla-mobile/firefox-android/blob/9f8e3ba718d922b3eb05fb7b3c263077ffaca20c/android-components/components/concept/engine/src/main/java/mozilla/components/concept/engine/DefaultSettings.kt#L48): `val webFontsEnabled: Boolean = true`
- [`GeckoEngine.java`](https://github.com/mozilla-mobile/firefox-android/blob/9f8e3ba718d922b3eb05fb7b3c263077ffaca20c/android-components/components/browser/engine/gecko/src/main/java/mozilla/components/browser/engine/gecko/GeckoEngine.kt#L85): applies `defaultSettings.webFontsEnabled` to `GeckoRuntimeSettings`

## License

This project is licensed under the MIT License.
