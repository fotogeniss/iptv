# Ultimate Playlist Loader v1.40.39 Validation Report

## Version

- `versionName`: `1.40.39`
- `versionCode`: `83`
- Baseline: `v1.40.38`

## Implemented scope

### Persistent EPG selection

- A successful manual or discovered public EPG selection updates only the active playlist's `epgUrl`.
- Playlist persistence reuses the existing encrypted `PlaylistStore.savePlaylists()` path.
- The URL is committed only after the requested guide is loaded and `EpgManager.currentSource()` still matches it.
- Failed, cancelled or stale requests do not change the playlist.
- The persisted URL becomes the automatic EPG source on later loads and can use the existing disk EPG cache.

### Source and lifecycle isolation

- Automatic and manual EPG requests use an atomic generation plus the stable playlist identity.
- Both paths share one mutex because `EpgManager` is process-global.
- Source switch, playlist deletion/addition reset and EPG disable cancel the active request and clear global EPG state.
- A blocking old request that completes late is rejected before UI or playlist persistence commit and clears its stale global result.
- Coroutine cancellation is rethrown rather than being presented as a network failure.
- Closing EPG discovery cancels `epgSearchJob`, so late directory results cannot reopen the candidate list.

### Input hardening

- Only remote HTTP(S) EPG URLs with a non-empty host are accepted.
- `file:`, `content:` and malformed locations are rejected before network work.

## Checks completed

- `EpgSelectionPolicy.kt` semantic compilation: **passed**.
- Focused policy runtime assertions: **8/8 passed**.
- Real `EpgSelectionPolicyTest.kt` semantic compilation with minimal JUnit stubs: **passed**.
- Production source contracts: **13/13 passed**.
- MainViewModel standalone parser/syntax-risk scan: **0 parser diagnostics**. The standalone compiler exit remains non-zero because Android, Lifecycle and Compose dependencies are intentionally absent from that invocation.
- Manifest/resource XML parse audit: **24 files, 0 errors**.
- Version contract: **passed**.

Validation logs are under `validation/`.

## Gradle status

Attempted:

```text
./gradlew :app:compileDebugKotlin --stacktrace
```

The wrapper stopped before Android/Kotlin compilation while downloading Gradle 8.9:

```text
Gradle wrapper bootstrap failed
java.net.ConnectException
Caused by: java.nio.channels.UnresolvedAddressException
```

Therefore this report does **not** claim a full Gradle compile, lint run, APK build or instrumentation test run.

## Device verification still required

- Select a public/manual EPG, restart the app and confirm the same source loads automatically.
- Start a large EPG download, switch playlist immediately and confirm no guide from the previous playlist appears.
- Close EPG discovery while public lookup is running and confirm the candidate list does not reappear.
