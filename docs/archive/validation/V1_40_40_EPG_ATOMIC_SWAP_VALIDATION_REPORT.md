# v1.40.40 EPG Atomic Swap — Validation Report

## Scope

Targeted hardening of EPG replacement and cache publication. No provider, playback, Multiview, navigation, or database-schema refactor.

## Changes

- Added isolated `EpgManager.Snapshot` candidates.
- `fetchSnapshot()` downloads/parses XMLTV without mutating visible EPG state.
- `installSnapshot()` atomically publishes a fully parsed candidate.
- Disk cache can be read as an unpublished candidate via `readCacheSnapshot()`.
- Failed automatic/manual EPG replacement preserves the previously working guide.
- Source/generation validation happens before publication.
- Cache writes use the immutable committed snapshot rather than mutable global state.
- Version bumped to `1.40.40` / `versionCode 84`.

## Passed checks

- Focused semantic Kotlin compile of production `EpgManager.kt` with Android/XML/HTTP stubs.
- Focused runtime harness: `EPG_ATOMIC_SWAP_OK`.
- Production unit-test source semantic compile and execution with JUnit stubs: `EPG_SNAPSHOT_TESTS_OK` (2 tests).
- MainViewModel syntax-risk scan: no parser diagnostics.
- Static EPG transactional contracts: 7/7.
- Android XML parse: 24 files, 0 errors.
- Version contract: passed.
- Patch generated from v1.40.39 baseline.
- ZIP integrity and SHA-256: passed.

## Gradle status

`./gradlew :app:compileDebugKotlin --no-daemon --stacktrace` was attempted. The wrapper stopped before Android/Kotlin compilation because Gradle 8.9 was not cached and `services.gradle.org` could not be resolved (`ConnectException` / `UnresolvedAddressException`).

Therefore this report does **not** claim a complete Android build, lint run, APK, instrumentation run, or device verification.
