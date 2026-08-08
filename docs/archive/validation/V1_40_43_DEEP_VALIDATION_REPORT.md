# v1.40.43 Deep Validation & TV Focus Gate — validation report

## Release identity

- `versionName`: `1.40.43`
- `versionCode`: `87`
- Baseline: `v1.40.42`
- Scope: release validation, routing/focus/security/lifecycle hardening; no DVR, timeshift or playback-engine rewrite.

## High-impact findings fixed

### Source mutation integrity

The previous mutation flow treated every edit/delete like an active-source change. That could cancel the active provider job, blank the visible catalog or erase source-scoped data still owned by a duplicate playlist entry.

The new flow calculates whether the removed/edited item is active, preserves the active source when it is not, invalidates generations before active cleanup and deletes source-scoped data only after the final stable-source reference disappears.

### TV Provider ownership

The browsable-disabled receiver previously accepted a broadcast program ID and deleted that TV Provider row after local suppression logic. A foreign row ID could therefore reach a delete path. The receiver now reads the row's internal provider ID, validates an application prefix plus canonical UUID, resolves it in the corresponding app-owned entry store and only then suppresses/deletes it.

### Structured cancellation

Several synchronous helper clients used broad fallback catches. During source switching, cancellation could become empty metadata, empty categories or a connection-test failure and allow old work to continue. A shared `ProviderCancellation` boundary now preserves direct cancellation, interrupted I/O and recognized transport-cancellation signals while leaving ordinary provider/business errors available to normal fallback behavior.

### Crash/lifecycle risks

- Removed all production `!!` operators.
- Replaced nullable SAF streams with controlled failures.
- Replaced Stalker `activeUa!!` access with an explicit connected-session requirement.
- Player teardown now removes anonymous as well as named delayed Handler callbacks.

## Automated results

### Broad pure/JVM regression suite

- Result: **179 pass, 0 fail**.
- Includes player input/session behavior, source deletion, TV Home route/ownership, provider cancellation, catalog normalization/refresh, large datasets, EPG policy, parsing, library and cache behavior.

### Compatibility contracts

- `PlayerActivity`: 10/10 v1.40.41 public members preserved.
- `MainViewModel`: 89/89 v1.40.41 public members preserved.
- Combined compatibility/source contracts: 11 pass, 0 fail.

### Architecture and risk gates

- Architecture: 29 pass, 2 warnings, 0 fail.
- Warnings: `PlayerActivity` remains 3,233 lines; `MainViewModel` remains 1,894 lines.
- Production critical-risk categories: 0 failures.
- Production `!!`, `GlobalScope`, `runBlocking`, `Thread.sleep`, process exit, unsafe SAF stream assertion and `activeUa!!`: all zero.
- Informational debt: 62 broad `catch (Exception)` boundaries and 30 delayed posts remain; network cancellation boundaries and Activity-handler teardown were specifically hardened in this release.

### TV focus instrumentation foundation

Five Compose instrumentation scenarios were added:

1. Load-mode initial focus.
2. Load-mode DPAD Down transition.
3. Refresh-mode safe initial focus.
4. Refresh-mode DPAD Down transition.
5. Category-picker initial focus and DPAD Right shortcut to Load.

These tests were not executed here because no Android SDK/device/emulator was available.

### Semantic/static validation

- Actual Stalker client semantic compilation against minimal dependency stubs: pass.
- Actual Xtream client plus provider-cancellation semantic compilation against minimal dependency stubs: pass.
- Kotlin changed-file parser-risk scan: pass with zero syntax findings.
- All 24 Android XML files parse successfully.
- CI YAML parses successfully.
- Credential-bearing log-pattern scan: pass.

## Build gate

The following real command was attempted:

```text
./gradlew clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease --stacktrace --no-daemon
```

It failed before Gradle execution because the Gradle 8.9 distribution was not cached and `services.gradle.org` could not be resolved/reached. Therefore this report does **not** claim a successful Android compile, lint run, R8/release APK, installation or physical-device execution.

## Remaining release blockers

1. Execute the full Gradle gate in an online Android build environment.
2. Run `connectedDebugAndroidTest` on TV and touch form factors.
3. Complete the physical TV focus/remote matrix.
4. Perform upgrade-install, real-provider source-switch, long playback/zapping and low-memory catalog tests.
5. Retain the documented cleartext compatibility exception until HTTPS-only migration UX is designed and device-tested.

## Decision

The source passes every validation available in this environment and is materially safer than v1.40.42. It is ready for the online build/device gate, but it is not represented as a fully device-verified production APK.
