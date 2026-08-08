# v1.40.41 Architecture Safety Validation Report

## Result

The controlled architecture phase completed with no static contract failures. The source package is suitable for a real Android compiler/device pass, but this environment could not bootstrap Gradle 8.9 and therefore did not produce an APK or a full Android compile result.

## Version

- `versionName`: `1.40.41`
- `versionCode`: `85`

## Size changes

- `PlayerActivity.kt`: 3,383 → 3,236 lines
- `MainViewModel.kt`: 2,182 → 1,821 lines

These files remain intentionally flagged as large. The release creates safe seams rather than attempting a high-risk rewrite.

## Extracted production boundaries

- `player/PlayerLaunchRequest.kt`
- `player/PlayerRemoteInputPolicy.kt`
- `player/AndroidPlayerKeyAdapter.kt`
- `player/PlayerEpgPanelController.kt`
- `ui/MainUiState.kt`
- `ui/coordinator/MainEpgCoordinator.kt`
- `ui/coordinator/CatalogSessionStore.kt`
- `ui/route/AppRouteContract.kt`

## Bugs and risks fixed

1. Player entry points could drift because each route manually constructed intent extras.
2. Player DPAD/media shortcut behavior was embedded in a very large Activity and could swallow focus navigation.
3. One-shot TV focus requests could fail before a lazy item or dialog target was attached.
4. Profile switching and settings restore terminated the process after launching a new task.
5. The profile list was captured as a stale remembered snapshot.
6. Several asynchronous provider/metadata/EPG paths swallowed coroutine cancellation and could publish stale work.
7. EPG and catalog lifecycle state was mixed directly into MainViewModel.
8. App-shell tab/browse state did not survive recreation.

## Functional-preservation checks

- No non-private `PlayerActivity` function removed.
- No non-private `MainViewModel` function removed.
- No raw player extras outside `PlayerLaunchRequest`.
- No `nativeKeyEvent` dependency.
- No `Runtime.getRuntime().exit` calls.
- Profile-gate intent key centralized.
- Existing provider, Multiview, EPG and catalog public routes retained.

## Validation executed

### Focused runtime assertions

- Player remote-input policy: 7 assertions
- Catalog session store: 4 assertions
- Typed player launch contract: 2 assertions
- Total: 13 assertions

### Semantic compilation

Passed with focused Android/data stubs:

- `PlayerRemoteInputPolicy`
- `CatalogSessionStore`
- `PlayerLaunchRequest`
- `MainEpgCoordinator`
- `MainUiState`
- `AppRouteContract`
- Associated production test sources

### Static audits

- Architecture audit: 21 pass, 2 warnings, 0 failures
- Kotlin parser/signature-risk scan: 0 findings across 27 changed Kotlin files
- XML parse: 24 files, 0 errors
- Patch whitespace: passed

### Remaining warnings

- `PlayerActivity.kt` remains 3,236 lines.
- `MainViewModel.kt` remains 1,821 lines.
- These are tracked migration warnings, not validation failures.

## Gradle status

Command attempted:

```text
./gradlew :app:compileDebugKotlin --stacktrace
```

The wrapper failed before Gradle started because the Gradle 8.9 distribution was not cached and `services.gradle.org` could not be resolved (`ConnectException` / `UnresolvedAddressException`).

Therefore, this report does **not** claim:

- full Android/Kotlin compilation,
- Android lint,
- Gradle JUnit execution,
- instrumentation tests,
- APK packaging,
- device verification.

## Recommended next verification

Run `:app:compileDebugKotlin`, unit tests and TV/device smoke tests in an environment with Gradle 8.9 available. The next architecture phase should not move ExoPlayer/VLC lifecycle ownership until instrumentation coverage exists.
