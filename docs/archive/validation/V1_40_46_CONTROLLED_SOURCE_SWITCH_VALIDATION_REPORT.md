# v1.40.46 Controlled Source Switch Coordinator — Validation Report

## Scope

This release extracts one responsibility only: playlist-switch orchestration and freshness-token ownership. It does not move provider loading, series retrieval, ExoPlayer/VLC ownership, EPG parsing, navigation or persistence schemas.

## Version

- Baseline: `v1.40.45`
- `versionName`: `1.40.46`
- `versionCode`: `90`

## Implementation

### `SourceGenerationGate`

The gate replaces the raw mutable generation counters previously stored in `MainViewModel`.

It owns:

- current catalog generation,
- current series generation,
- catalog-only invalidation,
- series-only invalidation,
- combined invalidation during source changes,
- paired series request tokens that become stale after either a newer series request or a source/catalog switch.

All existing catalog and series publication checks now delegate to this gate.

### `SourceSwitchCoordinator`

The coordinator validates the target before performing any side effect and owns the ordering of:

- target index persistence,
- generation invalidation,
- active work cancellation,
- EPG cancellation,
- runtime/client cleanup,
- provider/session invalidation,
- target-source plan creation,
- state publication,
- optional automatic load.

`MainViewModel.selectPlaylist()` remains public and unchanged in signature, but now contains only:

```kotlin
sourceSwitchCoordinator.switchTo(i)
```

### `SourceSwitchStatePolicy`

The policy clears source-bound fields in one immutable state transition:

- channels and groups,
- selected group and search,
- loading flags and old status,
- loaded section markers,
- EPG state,
- category/load/refresh dialogs,
- pending series detail state.

It preserves user/profile settings and source-independent state.

## Correctness issue fixed

Previously, `cancelActiveLoad()` canceled jobs and cleared `loadingAllSections`, but it did not clear the main `loading` flag. `selectPlaylist()` also inherited the current `loading`, status, search and series fields.

A rapid switch from a loading source to a target without a remembered choice could therefore display the target chooser with stale loading/status or detail state from the previous source. The new policy guarantees a clean target state before any optional network load begins.

## Focused tests

`SourceSwitchCoordinatorTest` contains ten focused cases:

1. invalid index has no side effects or generation change,
2. valid switch follows strict cancellation/publication order,
3. remembered choice auto-loads only after publication,
4. unknown choice does not start a network load,
5. switch plan carries target-source favorites only,
6. state policy clears source-bound UI and preserves settings,
7. load invalidation rejects an earlier catalog token,
8. a newer series request rejects the earlier request without changing catalog generation,
9. source switch rejects catalog and series tokens together,
10. closing series does not invalidate the catalog generation.

Runtime result: `10 pass, 0 fail`.

A separate semantic harness compiled and executed the same callback wiring used by `MainViewModel`, including the function references for repository invalidation, last-section lookup and source-scoped favorites.

## Compatibility and architecture gates

- `PlayerActivity`: 10/10 frozen public members preserved.
- `MainViewModel`: 89/89 frozen public members preserved.
- Compatibility contracts: 29 pass, 0 fail.
- Architecture audit: 41 pass, 2 warnings, 0 fail.
- Deep validation audit: 53 pass, 1 warning, 0 fail.
- Production risk inventory: 0 critical categories failed.

Known size warnings:

- `PlayerActivity.kt`: 3,228 lines.
- `MainViewModel.kt`: 1,837 lines.

`MainViewModel.kt` decreased from 1,844 to 1,837 lines despite the new adapter wiring; the extracted coordinator is 155 lines and independently testable.

## Static validation

- Changed Kotlin files: 3.
- Changed-file Kotlin parser findings: 0.
- XML resources/manifests: 24 parsed, 0 errors.
- CI YAML: parsed successfully.
- Version contract: passed.
- No production non-null assertions, GlobalScope, runBlocking, Thread.sleep or process exit calls were introduced.

## Gradle gate

The following command was executed:

```text
./gradlew clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease --stacktrace --no-daemon
```

The wrapper started and attempted to download Gradle 8.9, but stopped before Gradle or Android compilation because `services.gradle.org` could not be resolved/reached (`ConnectException` / `UnresolvedAddressException`).

Therefore this report does not claim:

- full Android/Kotlin compilation,
- Gradle unit-test execution,
- lint execution,
- debug or release APK generation,
- R8 verification,
- emulator or physical-device execution.

## Result

The controlled source-switch extraction is accepted at every validation level available in the current environment. The next safe seam is `SeriesCoordinator`; playback-engine ownership remains deferred until a connected Android build/device gate is available.
