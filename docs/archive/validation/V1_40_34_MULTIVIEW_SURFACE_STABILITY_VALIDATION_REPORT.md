# v1.40.34 Multiview surface/stability validation report

## Reported behavior

- The right pane played for roughly ten seconds and then stopped.
- Returning to the left pane showed that it was also frozen.
- Video occupied a small fitted rectangle instead of the complete left/right halves.

## Targeted changes

### Rendering/layout

- Added `activity_multiview.xml` with two weighted, full-height panes.
- Removed pane margins.
- Configured each Media3 `PlayerView` with `surface_type="texture_view"`.
- Configured `resize_mode="zoom"` so each stream fills its complete half.
- Added immersive system-UI handling and a 2dp center divider.

The old implementation instantiated two default PlayerViews programmatically. Their default SurfaceView-backed surfaces can be problematic on hardware that has limited video-overlay planes, and `fit` behavior can leave a small video rectangle inside a tall half-screen pane. The new layout forces GPU-composited TextureViews and a deterministic 50/50 viewport.

### Playback stability

- Uses the same desktop user agent and redirect behavior as the app's main ExoPlayer path.
- Adds 15s connect and 20s read timeouts.
- Adds Media3 load-error retry policy (minimum 6 load retries).
- Adds a watchdog every 2s; a pane stuck for 12s in idle/buffering/non-playing-ready state is restarted.
- Restarts are bounded and back off from 500ms to 5s.
- Retry counters reset only after 30s of stable playback.
- Player generation checks ignore stale callbacks.
- Restart/release is pane-scoped, so a failed secondary does not release the primary.
- The inactive pane still has its audio track type disabled, with volume=0 as a fail-safe.

## Checks completed

| Check | Result |
|---|---|
| Focused Kotlin semantic compile with Android/Media3 API stubs | Passed |
| Focused policy assertions | 6 passed |
| Android XML parse | Passed: 24 files, 0 errors |
| 50/50 layout contract | Passed |
| TextureView/zoom contract | Passed: 2 panes |
| No pane margins contract | Passed |
| Per-pane retry/watchdog contract | Passed |
| Audio renderer isolation contract | Passed |
| Secure token-only Intent contract | Passed |
| Version contract | Passed: 1.40.34 / 78 |
| ZIP integrity | Passed |

## Gradle limitation

`./gradlew :app:compileDebugKotlin --stacktrace` was attempted. The wrapper failed before Gradle startup because Gradle 8.9 was not cached and `services.gradle.org` could not be resolved. Therefore a full Android/Kotlin Gradle compile, lint, APK build and device playback test are not claimed.

## Required hardware verification

Run `:app:compileDebugKotlin`, `assembleDebug`, then test on the affected TV box for at least 3–5 minutes while switching audio between panes repeatedly.

If both streams still terminate together after this surface/reconnect fix, inspect the logged Media3 error code and the provider account's simultaneous-connection limit. A provider plan limited to one active stream cannot be bypassed safely by the application.
