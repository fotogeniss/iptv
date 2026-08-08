# v1.40.42 Controlled Player Session Refactor — Validation Report

## Result

The first controlled refactor step is complete. Player session identity, queue transition state and stale async generation ownership moved out of `PlayerActivity` behind a testable boundary. Public PlayerActivity/MainViewModel members remain compatible with the frozen v1.40.41 contract.

This package passed the available focused semantic, runtime, compatibility, architecture, TV-focus, XML and version checks. A full Android Gradle compile was attempted but did not start because Gradle 8.9 could not be downloaded in this environment.

## Version

- `versionName`: `1.40.42`
- `versionCode`: `86`

## Controlled scope

### Added production boundary

- `player/PlayerSessionController.kt`
  - `PlayerSessionController`
  - `PlayerSessionState`
  - `PlayerSessionTransition`
  - `PlayerSessionQueue`
  - `PlaybackQueueSessionAdapter`

### Ownership moved from PlayerActivity

- current playback URL
- presented title/content type/EPG ID
- source identity
- resume-position key
- queue transition generation
- committed queue index versus pending target index
- subtitle identity associated with channel transitions

No ExoPlayer/VLC lifecycle ownership was moved. No player UI redesign, provider change, database migration or navigation rewrite was included.

## Behavior defects fixed during extraction

1. **Rapid-zap rollback mismatch**
   - Two unresolved channel changes followed by a failure could restore an intermediate queue index while the original stream was still playing.
   - Rollback now returns to the last channel whose URL was successfully committed.

2. **Resume saved under the wrong item**
   - While a target URL was pending, `savePos()` could use the target item's key although the previous stream was still playing.
   - Resume persistence now uses `playerSession.playbackState`, the committed stream identity.

3. **Unnecessary restart after failed resolve**
   - The previous stream was deliberately kept alive during URL resolution, but a failure recreated that same player anyway.
   - Failure now restores chrome/queue state without restarting the committed stream.

4. **Failed targets in recents**
   - A movie or episode could be added to recents before its URL resolved.
   - Recents commit now occurs only after successful session commit.

5. **Stale provider result**
   - A delayed result from an older channel request cannot replace a newer selection.

## Compatibility contracts

Frozen v1.40.41 public-member snapshots were added for:

- `PlayerActivity`: 10 members preserved
- `MainViewModel`: 89 members preserved

`compatibility_contracts.py` result:

- 11 pass
- 0 fail

The architecture/focus/routing audit result:

- 23 pass
- 2 tracked size warnings
- 0 fail

Tracked warnings:

- `PlayerActivity.kt`: 3,214 lines
- `MainViewModel.kt`: 1,821 lines

`PlayerActivity` decreased from 3,236 to 3,214 lines. The controller is intentionally separate and testable; the goal is responsibility reduction rather than cosmetic line movement.

## Focused tests

Six runtime assertions passed:

1. pending target updates presentation but keeps the committed URL active
2. successful resolve becomes the next rollback baseline
3. rapid-zap failure returns to the last committed channel
4. stale resolve cannot replace a newer target
5. boundary step does not mutate state
6. committed subtitle identity survives a pending transition rollback

The real production controller and real test source were semantically compiled with minimal Android-independent stubs.

## Additional validation

- PlayerSessionController semantic compile: passed
- Kotlin syntax-risk scan: 0 syntax findings
- XML parse: 24 files, 0 errors
- Version contract: passed
- No legacy `loadToken`
- No mutable duplicate `streamUrl` field in PlayerActivity
- Resume persistence follows committed playback
- Existing TV initial-focus and remote-shortcut contracts remain passing

## Gradle status

Attempted command:

```text
./gradlew :app:compileDebugKotlin --stacktrace
```

The wrapper started and attempted to download Gradle 8.9, then failed before Gradle execution with:

```text
java.net.ConnectException
Caused by: java.nio.channels.UnresolvedAddressException
```

Therefore this report does **not** claim:

- full Android/Kotlin compilation
- Android lint
- complete Gradle unit-test execution
- instrumentation tests
- APK assembly
- device verification

## Next controlled seam

The next safe release should extract one responsibility only. The preferred order remains:

1. Player chrome/overlay visibility and focus lifecycle
2. CatalogLoadCoordinator from MainViewModel
3. SeriesCoordinator
4. Playback engine abstraction only after instrumentation coverage exists
