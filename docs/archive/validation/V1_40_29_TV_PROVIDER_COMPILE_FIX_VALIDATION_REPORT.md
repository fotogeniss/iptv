# v1.40.29 TV Provider Compile Fix — Validation Report

## Scope

Small build-only correction on top of v1.40.28. The change is restricted to Android TV content-type constant ownership in two legacy TV Home publishers plus the version bump.

## Root cause

`COLUMN_TYPE` values were being read from `TvContract.Programs`, but the framework exposes the relevant content-type constants on the concrete provider contracts:

- `TvContract.PreviewPrograms` for preview channel rows.
- `TvContract.WatchNextPrograms` for Watch Next rows.

This caused Kotlin unresolved-reference errors for `TYPE_TV_EPISODE`, `TYPE_CHANNEL`, and `TYPE_MOVIE`.

## Change

- `LegacyMyListChannelPublisher.kt`
  - `TvContract.PreviewPrograms.TYPE_TV_EPISODE`
  - `TvContract.PreviewPrograms.TYPE_CHANNEL`
  - `TvContract.PreviewPrograms.TYPE_MOVIE`
- `LegacyWatchNextPublisher.kt`
  - `TvContract.WatchNextPrograms.TYPE_TV_EPISODE`
  - `TvContract.WatchNextPrograms.TYPE_MOVIE`
- Version: `1.40.29`, `versionCode 73`.

## Checks completed

- Focused semantic Kotlin compile of both production publisher files against a minimal Android/API contract harness: PASS.
- Stale invalid-symbol scan (`TvContract.Programs.TYPE_*`): PASS, zero matches.
- Expected concrete-contract symbol scan: PASS.
- Version contract: PASS (`1.40.29`, code `73`).
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

Therefore this report does **not** claim a successful full Android Gradle compilation. The reported compiler failure is directly addressed, and the affected production files passed the focused semantic compile described above.

## Recommended connected check

```text
./gradlew :app:compileDebugKotlin
```

If that passes, continue with:

```text
./gradlew testDebugUnitTest lintDebug assembleDebug
```
