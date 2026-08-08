# Ultimate Playlist Loader v1.40.26 — Validation Report

## Scope

Small playback-stability and TV startup polish increment on top of v1.40.25.

Implemented:

- Coalesced rapid TV channel changes with a 180 ms bounded zap window.
- Existing stream remains attached while the next provider URL resolves; replacement occurs only after a non-blank URL is available.
- Stale async resolves remain rejected by the existing monotonically increasing `loadToken` contract.
- ExoPlayer retries only transient `IOException` failures, only while the request/player is still current, with a maximum of two retries.
- TV focus restoration after resume when controls are visible and focus has been lost.
- Cold-start Lumina intro using the supplied MP4 and logo.
- Warm starts skip the intro for 15 minutes; reboot-safe elapsed-time comparison.
- Launcher is isolated in `StartupActivity`; `MainActivity` is internal and `singleTask` to avoid duplicate app shells.
- Version bumped to `1.40.26` / `versionCode 70`.

## Checks completed

- Pure Kotlin compilation of `PlaybackStabilityPolicy` with Kotlin/JVM compiler: **passed**.
- Focused policy harness: **10 assertions passed**.
- XML parse audit: **23 XML files passed**.
- Manifest launcher/export/launch-mode audit: **passed**.
- Startup logo/video resource existence audit: **passed**.
- Intro media probe: **10.005 seconds**, valid MP4 container.
- Playback contract audit for zap coalescing, stale-request gating, bounded retry and cleanup callback removal: **passed**.
- Version contract: **1.40.26 / 70 passed**.
- Final ZIP integrity test: **passed**.

## Android/Gradle status

A full Android build was **not completed**. `./gradlew --offline testDebugUnitTest --no-daemon` attempted to bootstrap Gradle 8.9, but that distribution was not cached. The wrapper then failed because the environment could not resolve/connect to `services.gradle.org`.

Therefore this report does **not** claim:

- Android source compilation,
- Android Lint,
- Gradle JUnit execution,
- APK/AAB packaging,
- installation or device playback validation.

The deliverable contains only changes that passed the available independent checks listed above.
