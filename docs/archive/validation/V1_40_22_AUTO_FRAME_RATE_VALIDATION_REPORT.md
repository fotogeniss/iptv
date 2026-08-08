# v1.40.22 Auto Frame Rate — Validation Report

Date: 2026-07-21  
Baseline: v1.40.21 (`versionCode 65`)  
Candidate: v1.40.22 (`versionCode 66`)

## Scope

This release adds opt-in Auto Frame Rate support to the Android TV player while
preserving the v1.40.21 seek architecture and all security hardening from
v1.40.19.

### User modes

- `off`: no display refresh-rate request.
- `seamless`: Media3 and the window request a change only when the platform can
  perform it without a disruptive mode switch.
- `always`: the app selects a concrete same-resolution display mode and permits
  a potentially visible HDMI timing transition.

The setting is TV-only in the user interface and defaults to `off`.

## Implementation audit

### Playback engines

- ExoPlayer reads the selected video track's declared frame rate from
  `Tracks`/`Format.frameRate`.
- Media3's `VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS` is enabled only
  for Seamless mode.
- Media3 frame-rate calls are disabled in Full mode so the app owns display-mode
  selection.
- VLC reads `frameRateNum / frameRateDen` from its active video track after
  `MediaPlayer.Event.Vout`.
- Repeated identical frame-rate requests are deduplicated.

### Display safety

- Display-mode matching is isolated in Android-free `FrameRateMatchPolicy`.
- Only modes with the current physical resolution are considered.
- The chosen refresh rate must be an integer multiple of the content rate.
- 24 fps therefore rejects 60 Hz and accepts timings such as 24, 48, 72, 96 or
  120 Hz when exposed by the TV.
- Fractional families such as 23.976/47.952 and 29.97/59.94 are supported with a
  narrow tolerance.
- Missing, non-finite or implausible metadata clears the preference instead of
  forcing a mode.

### Lifecycle

- Requests are cleared when the player enters the background.
- The last valid request is reapplied on foreground return.
- Preferences are released on sleep-stop and Activity destruction.
- Mobile/PiP behavior is unchanged because the controller is called only on TV.

## Automated and static validation

| Check | Result |
|---|---|
| Kotlin PSI syntax parse | 171 files, 0 syntax errors |
| Production Kotlin count | 145 files |
| Test Kotlin count | 26 files |
| Focused AFR policy runner | 8 passed, 0 failed |
| Controller semantic compilation with Android API stubs | Passed |
| XML parse | 23 files, 0 errors |
| GitHub Actions YAML parse | 1 file, 0 errors |
| Settings/player wiring audit | Passed |
| Version audit | `1.40.22` / `66` |

Focused cases cover safe persisted-mode parsing, invalid metadata, rejection of
60 Hz for 24 fps, exact 23.976 selection, 25→50, 29.97→59.94,
same-resolution enforcement and 24→120 support.

## Full Gradle build status

The wrapper attempted to run:

```text
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

It could not bootstrap because Gradle 8.9 was not cached and this execution
environment cannot resolve/download `services.gradle.org`. The failure occurred
before project configuration or Kotlin/Android compilation. See
`gradle_build_attempt_v1_40_22.txt`.

A normal Android Studio or connected GitHub Actions run is still required before
publishing an APK/AAB.

## Recommended device regression matrix

1. 23.976/24 fps movie on a TV exposing 23.976/24 and 59.94/60 modes.
2. 25 fps European live/VOD content on a 50/60 Hz TV.
3. 29.97 fps content on 59.94/60 Hz modes.
4. ExoPlayer and forced VLC playback.
5. Home → return, episode change, player fallback and final player exit.
6. Seamless mode on a device with Android TV's “Match content frame rate”
   preference both enabled and disabled.
7. Full mode on hardware that performs non-seamless HDMI mode switching.

## Files added

- `app/src/main/java/com/prelude/iptv/player/FrameRateMatchPolicy.kt`
- `app/src/main/java/com/prelude/iptv/player/DisplayFrameRateController.kt`
- `app/src/test/java/com/prelude/iptv/player/FrameRateMatchPolicyTest.kt`
- `CHANGELOG_AUTO_FRAME_RATE_V1_40_22.md`
- This validation report and machine-readable validation logs.
