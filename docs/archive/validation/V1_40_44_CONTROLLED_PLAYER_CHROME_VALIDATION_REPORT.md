# v1.40.44 Controlled Player Chrome — Validation Report

## Scope

This release extracts one responsibility only: player chrome lifecycle. Playback engines, provider URL resolution, subtitles, EPG, routing, source state and database formats remain unchanged.

## Extracted responsibility

`PlayerChromeController` now owns:

- visible/hidden overlay state
- auto-hide scheduling and cancellation
- transient player status scheduling and replacement
- bounded TV focus restoration
- chrome-dependent subtitle lift selection
- teardown of chrome-owned scheduled work

The controller has no Android dependency. `PlayerActivity` retains all `View` references and exposes them through a small host adapter.

## Compatibility contracts

- `PlayerActivity`: 10/10 frozen v1.40.41 public members preserved.
- `MainViewModel`: 89/89 frozen v1.40.41 public members preserved.
- No raw player Intent extras were introduced.
- `PlayerSessionController` remains the playback/session owner.
- No ExoPlayer/VLC ownership moved.
- No Manifest, database or storage schema change.

## Focused behavior tests

Nine focused assertions passed:

1. show renders the expected subtitle lift and auto-hides
2. rearm cancels the previous auto-hide timer
3. focus retries stop after success
4. focus retries stop at the configured bound
5. the legacy subtitle-lift matrix is preserved
6. valid existing TV focus prevents unnecessary focus requests
7. hide cancels focus and auto-hide work
8. replacing status cancels the old timeout
9. dispose prevents late scheduled callbacks

## Static and semantic validation

- Real `PlayerChromeController.kt` semantic compile: pass.
- Real `PlayerChromeControllerTest.kt` compile with minimal JUnit API stubs: pass.
- PlayerActivity Kotlin syntax-risk scan: 0 syntax diagnostics.
- Compatibility audit: 15 pass, 0 fail.
- Architecture audit: 31 pass, 2 warnings, 0 fail.
- Deep validation audit: 47 pass, 1 warning, 0 fail.
- XML parse: 24 files, 0 errors.

The two architecture warnings remain the known large files:

- `PlayerActivity.kt`: 3,228 lines
- `MainViewModel.kt`: 1,894 lines

## Gradle status

The following real gate was attempted:

```text
./gradlew clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --stacktrace --no-daemon
```

The wrapper stopped before Gradle startup because Gradle 8.9 was not cached and `services.gradle.org` could not be resolved/reached. Therefore this report does not claim a full Android compile, lint run, APK build or device verification.

## Result

The controlled chrome extraction passes all checks available in this environment and preserves the frozen public compatibility surface. The next safe seam is `CatalogLoadCoordinator`.
