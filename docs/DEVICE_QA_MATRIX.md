# Prelude+ device QA matrix

This matrix defines the minimum evidence required before a public release. A
successful build is necessary, but it is not a device-QA pass.

## Required release matrix

| ID | Form factor | Android/API | ABI / navigation | Primary risks |
|---|---|---|---|---|
| M1 | Small/medium phone | Android 8 / API 26 | armeabi-v7a or arm64, 3-button | minimum SDK, memory, system-bar overlap |
| M2 | Current phone | Android 14+ / API 34–35 | arm64, gesture navigation | edge-to-edge, PiP, portrait/landscape player |
| M3 | Tablet or foldable-sized emulator | API 33+ | arm64/x86_64 | responsive layouts, rotation, multi-window |
| T1 | Android TV emulator | API 28–30 | x86_64 | D-pad focus, dialogs, TV launcher |
| T2 | Physical Android/Google TV | API 30+ | arm64 | codecs, frame-rate matching, long playback |
| B1 | Low-memory TV box | API 26+ | armeabi-v7a where supported | libVLC, large catalogs, memory pressure |

M1, M2, T1 and T2 are blocking. M3 and B1 may be waived only with a written
release note that identifies the missing device and the owner of the follow-up.

## Automated gate on every matrix entry

1. Static compatibility/architecture/risk contracts pass.
2. JVM unit tests pass.
3. `connectedDebugAndroidTest` passes in the device's current orientation.
4. On touch devices, repeat instrumentation once in portrait and once in landscape.
5. The installed app launches and remains alive for the smoke window.
6. Collected logcat contains no Prelude+ fatal exception, ANR or force-finish.
7. The run stores device identity, launch timing, screenshot and memory snapshot.

Windows:

```powershell
.\scripts\verify-device.ps1 -Serial <adb-serial> -Orientation Portrait
.\scripts\verify-device.ps1 -Serial <adb-serial> -Orientation Landscape
```

Linux/macOS/CI host:

```bash
ANDROID_SERIAL=<adb-serial> ./scripts/verify-device.sh
```

Artifacts are written under `validation/device-runs/` and are intentionally
ignored by Git. Release evidence should be archived outside the source tree or
attached to the release/CI run.

## Critical manual flows

Every blocking device must cover the flows below using provider-owned test data.
No credentials or playlist URLs may appear in screenshots or logs.

### Startup and source lifecycle

- Clean install reaches an actionable empty/settings state.
- Upgrade preserves playlists, favorites, history, category layout and EPG.
- Add, edit, refresh and delete M3U, Xtream and Stalker/MAC sources.
- Cancel or switch source during a large refresh; stale data must not commit.
- Load a large catalog, background the app under pressure and resume it.

### Browse and customization

- Mobile Home/Live/Movies/Series changes persist after process restart.
- Hidden, reordered, deleted and restored categories affect real content.
- Search, favorites, recently watched and recommendations open the correct item.
- TV routes keep deterministic D-pad focus and restore focus after Player/Back.

### Playback

- HLS, MPEG-TS and VOD start with Media3; one fallback case starts with libVLC.
- Pause/play, seek, zapping, audio tracks, embedded/downloaded subtitles and
  subtitle appearance do not recreate or stop playback unexpectedly.
- CC/Audio panel fits portrait and landscape, including 3-button navigation.
- Aspect ratio, mini player, PiP, background/foreground and next episode work.
- Run one live/VOD stream for two hours and record memory, ANR and player count.

### EPG and network failure

- Xtream, Stalker and custom XMLTV EPG sources show correct now/next data.
- A malformed replacement never erases the last valid guide.
- Disconnect/reconnect during load and playback without crash or stale commit.

## Release sign-off

For every matrix row record: app version/code, commit, device/build fingerprint,
orientation, source types exercised, PASS/FAIL, artifact location, tester and
date. Any crash, ANR, data loss, credential leak, focus trap or black-video
playback is release-blocking.
