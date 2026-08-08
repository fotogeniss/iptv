# v1.40.30 Watch Next Aspect Ratio Compile Fix — Validation Report

## Scope

Small build-only correction on top of v1.40.29. The change is limited to poster-art aspect-ratio constant ownership in `LegacyWatchNextPublisher.kt` plus the version bump.

## Root cause

`COLUMN_POSTER_ART_ASPECT_RATIO` belongs to `TvContract.WatchNextPrograms`, and the accepted aspect-ratio values are exposed by that concrete contract class. The source still referenced `TvContract.Programs.ASPECT_RATIO_2_3` and `TvContract.Programs.ASPECT_RATIO_1_1`, which are not present on `TvContract.Programs` and caused Kotlin unresolved-reference errors.

## Change

- `LegacyWatchNextPublisher.kt`
  - `TvContract.WatchNextPrograms.ASPECT_RATIO_2_3`
  - `TvContract.WatchNextPrograms.ASPECT_RATIO_1_1`
- Removed the two invalid `TvContract.Programs.ASPECT_RATIO_*` references.
- Version: `1.40.30`, `versionCode 74`.

## Checks completed

- Focused semantic Kotlin compile of the production `LegacyWatchNextPublisher.kt` against a minimal Android/API contract harness: PASS.
- Stale invalid-symbol scan (`TvContract.Programs.TYPE_*` and `TvContract.Programs.ASPECT_RATIO_*`): PASS, zero matches.
- Concrete TV provider contract scan: PASS.
- Version contract: PASS (`1.40.30`, code `74`).
- XML parsing: PASS (23 XML files, 0 parse errors).
- Patch generation: PASS.
- ZIP integrity: PASS.
- SHA-256 generation: PASS.

## Android/Gradle status

The real command was attempted:

```text
./gradlew :app:compileDebugKotlin --stacktrace --no-daemon
```

It stopped in the Gradle wrapper bootstrap before project configuration or Kotlin compilation because Gradle 8.9 is not cached and this environment cannot resolve/connect to `services.gradle.org` (`java.net.ConnectException` / `UnresolvedAddressException`).

This report therefore does **not** claim a successful full Android Gradle compilation. The reported unresolved references are directly corrected and the affected production file passed the focused semantic compile.

## Recommended connected check

```text
./gradlew :app:compileDebugKotlin
```

If that passes, continue with:

```text
./gradlew testDebugUnitTest lintDebug assembleDebug
```
