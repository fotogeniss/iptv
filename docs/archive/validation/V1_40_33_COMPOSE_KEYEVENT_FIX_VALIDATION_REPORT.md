# v1.40.33 Compose KeyEvent Fix — Validation Report

## Scope

A single compile compatibility correction in `TvLiveRail.kt`:

- removed `KeyEvent.nativeKeyEvent`
- used Compose `Key` for DPAD/Enter detection
- used Compose `KeyEventType` for key-down/key-up handling

No Multiview routing, player, provider, mobile UI, or persistence logic was refactored.

## Version

- versionName: `1.40.33`
- versionCode: `77`

## Checks completed

- Focused Kotlin semantic compile of the actual `TvLiveRail.kt` against a Compose-key API stub exposing `KeyEvent.key` and `KeyEvent.type`: **PASS**
- Produced `TvLiveRailKt.class` and related card/rail classes: **PASS**
- Source scan for `nativeKeyEvent`: **0 references**
- DPAD key contract (`DirectionCenter`, `Enter`, `NumPadEnter`): **PASS**
- Key-down/key-up contract: **PASS**
- XML parse: **23 files, 0 errors**
- Version contract: **PASS**

## Gradle status

`./gradlew :app:compileDebugKotlin --offline --no-daemon` was attempted. The wrapper did not reach project compilation because Gradle 8.9 was not cached and the environment could not resolve `services.gradle.org`.

Therefore this report does **not** claim a complete Android/Gradle build, lint run, APK build, or Gradle test run.
