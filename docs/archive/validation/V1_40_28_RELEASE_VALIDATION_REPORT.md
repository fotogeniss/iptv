# v1.40.28 Release Hardening Validation Report

## Scope

Small lifecycle-only hardening of the v1.40.27 startup flow. No playback, provider, catalog, or Multiview architecture refactor was introduced.

## Change

- Bumped application version to `1.40.28` / `versionCode 72`.
- Moved the intro failsafe scheduling from `onCreate()` to `onResume()`.
- Cancels the delayed launch callback in `onPause()`.
- Pauses the intro video in background and resumes it when interactive again.
- Keeps final listener, callback, and `VideoView` cleanup in `onDestroy()`.
- Prevents the splash timeout from launching `MainActivity` after Home/background.

## Checks completed

- Version contract: PASS (`1.40.28`, code `72`).
- Startup lifecycle contract audit: PASS.
- Manifest startup declaration audit: PASS.
- XML parsing: PASS (23 XML files, 0 parse errors).
- Intro MP4 container recognition: PASS.
- Logo resource presence: PASS (`drawable-nodpi/lumina_logo.png`).
- ZIP integrity: PASS.
- Patch generation: PASS.
- SHA-256 generation: PASS.

## Android/Gradle status

A Gradle wrapper attempt was made. It failed before project configuration or Kotlin/Android compilation because Gradle 8.9 is not cached and the environment cannot connect to `services.gradle.org` (`java.net.ConnectException`).

Therefore this report does **not** claim successful Android compilation, lint, Gradle unit tests, APK generation, signing, or installation testing.

## Release assessment

The delivered source passes the checks available in this environment. A connected build environment should still run at minimum:

```text
./gradlew clean testDebugUnitTest lintDebug assembleDebug
```
