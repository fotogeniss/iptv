# Ultimate Playlist Loader v1.40.27 — RC Hardening Validation

## Scope
Small release-candidate corrections on top of v1.40.26. No feature expansion or broad refactor.

## Changes
- Startup intro now has a 12-second failsafe and cannot trap the user on an unsupported/corrupt media resource.
- Startup VideoView and callbacks are released on lifecycle teardown.
- Main launch uses CLEAR_TOP + SINGLE_TOP while MainActivity remains the production singleTask shell.
- Playback retry recognizes IOException anywhere in the wrapped cause chain.
- Retry captures the failing player's position immediately instead of reading mutable player state later.
- Version bumped to 1.40.27 / versionCode 71.

## Checks completed
- XML parse: 23 files, 0 errors.
- Version, Manifest, startup lifecycle, retry cause-chain and resume-position contract audits: passed.
- Platform-free Kotlin semantic compile: passed.
- Focused policy assertions: 8 passed.
- Source ZIP integrity: passed.

## Android/Gradle status
`./gradlew --offline testDebugUnitTest` did not reach project configuration or compilation. The wrapper attempted to download Gradle 8.9 and failed with `java.net.ConnectException`; the distribution is not cached and network access is unavailable.

Therefore this package is not claimed as a completed Android build, lint run, APK build, or Gradle JUnit run.
