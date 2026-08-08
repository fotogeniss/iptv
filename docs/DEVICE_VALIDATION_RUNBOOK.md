# Device validation runbook

This runbook is the required physical-device gate after the source/static/JVM gate.
It is intentionally repeatable and records behavior rather than screenshots alone.

## Preconditions

- Install as an upgrade over the previous production build first; repeat once as a clean install.
- Test at least one Android TV / Google TV device with a physical remote and one touch device.
- Keep one M3U source and one authenticated Xtream or Stalker source available.
- Keep a valid XMLTV URL and an intentionally invalid URL available.
- Capture `adb logcat`, ANR traces and app version before each run.

## Automated device smoke

```bash
ANDROID_SERIAL=<serial> ./scripts/verify-device.sh
```

The script refuses to run without a ready device and refuses ambiguous multi-device runs.
It executes the compatibility, architecture, deep-validation and risk gates before
`connectedDebugAndroidTest`.

On Windows, use the equivalent runner. Touch devices must be exercised in both
orientations because the player and track panel use different responsive branches:

```powershell
.\scripts\verify-device.ps1 -Serial <serial> -Orientation Portrait
.\scripts\verify-device.ps1 -Serial <serial> -Orientation Landscape
```

The mandatory form factors and sign-off fields are defined in
[`DEVICE_QA_MATRIX.md`](DEVICE_QA_MATRIX.md).

## Upgrade and data integrity

1. Open the previous version and create two playlists, favorites, history, group selections and an EPG source.
2. Upgrade without clearing app data.
3. Verify every source-scoped value remains attached to the correct playlist.
4. Edit and delete an inactive playlist; confirm the active catalog remains visible and playing.
5. Delete the active playlist; confirm the replacement source loads and no deleted-source state appears.
6. Repeat with two playlist entries that resolve to the same stable source identity; deleting one must not erase shared data.

## Android TV focus matrix

For every Home, Live, Movies, Series, Search, Library, Settings, details, category picker and dialog route:

- A visible actionable element receives initial focus.
- Up/Down/Left/Right always have a deterministic result.
- The first and last item do not trap focus or move focus off-screen.
- Scrolling follows focus in lazy lists and grids.
- Back closes the topmost overlay before leaving the route.
- Returning from Player restores the previously selected item or a safe visible fallback.
- Refresh/loading/error transitions never leave focus on a removed or invisible node.
- Menu, Info, captions, media keys, channel keys and number keys do not double-fire on repeated key-down events.

## Source switching and cancellation

1. Start a large catalog refresh and switch playlist immediately.
2. Start EPG loading and switch playlist immediately.
3. Open series metadata, switch source and return.
4. Trigger refresh twice, then press Back.
5. Confirm no data, status, error or EPG from the old source appears after the switch.
6. Confirm normal coroutine cancellation is silent and is not rendered as a network error.

## Playback and lifecycle

- Test HLS, MPEG-TS and direct HTTP streams where available.
- Exercise rapid channel zapping, audio/subtitle selection, seek, pause/play and player fallback.
- Background/foreground the app, turn the screen off/on and disconnect/reconnect the network.
- Verify the committed item remains the resume/history owner while a new channel URL is resolving.
- Verify a failed zap keeps the last stream that actually started.
- Leave playback running for at least two hours and inspect memory, player instances, wakelocks and ANRs.

## EPG transactional behavior

1. Load a working EPG source.
2. Attempt an invalid or malformed replacement.
3. Verify the existing guide stays visible and configured.
4. Load a second valid source and verify atomic replacement.
5. Switch playlist during download and verify the late result is discarded.
6. Check timezone and DST boundaries against known programme times.

## Large catalog and low-memory behavior

- Load a provider with thousands of channels/items or an equivalent controlled fixture.
- Browse already published sections while remaining sections download.
- Scroll/search continuously during loading.
- Send the app to background under memory pressure and resume it.
- Record first-visible-content time, peak memory, GC churn, jank and any ANR.

## Pass criteria

- No crash, ANR, focus trap, stale-source commit or data loss.
- No foreign TV Provider row can be removed by an app broadcast.
- No credentials, playlist URLs or resolved stream URLs appear in logs.
- Debug and release APKs build, install and launch in the online build environment.
- All instrumentation tests pass on both TV and touch form factors.
