# v1.40.21 Player Architecture and Stability Validation Report

## Scope

This release forward-builds on v1.40.20 and preserves the Android TV seek-bar
fix while moving its state and routing rules out of `PlayerActivity` into
focused, JVM-testable components.

## Implemented components

### `TvSeekKeyPolicy`

- Converts TV seek-bar LEFT/RIGHT/OK intent into deterministic decisions.
- Passes unrelated keys, mobile input and live playback through unchanged.
- Consumes key-up events so Android `SeekBar` cannot apply an extra
  visual-only movement.
- Prevents repeated OK/ENTER key-down events from repeatedly toggling playback.

### `TvSeekController`

- Owns the pending scrub target.
- Accumulates repeated LEFT/RIGHT presses against the pending target rather
  than a stale player position.
- Clamps known-duration targets to `0..duration`.
- Keeps immediate relative seek behavior when an IPTV provider exposes no
  usable duration.
- Supports deterministic commit and cancellation.

### `PlaybackSeekPolicy`

- Centralizes absolute and relative seek boundary handling.
- Prevents negative positions, seeks past a known duration and `Long` overflow.
- Is used by touch, remote, gesture and media-key seek paths through the shared
  `seekTo()` / `seekBy()` functions.

### `SeekBarPositionMapper`

- Maps `Long` media positions to Android's `Int`-based `SeekBar`.
- Uses direct millisecond mapping for normal content.
- Scales timelines longer than `Int.MAX_VALUE` milliseconds without overflow.
- Converts touch progress back to the correct media position.

## Lifecycle stability

- Starting touch tracking cancels any pending remote scrub.
- Changing queue content cancels a pending target from the previous item.
- Pressing Home/backgrounding within the 400 ms debounce window commits the
  intended target before resume progress is persisted.
- Destroying the activity cancels callbacks and pending scrub state.

## Automated validation completed

- Kotlin PSI parse of all `app/src` Kotlin files: **168 files, 0 syntax errors**.
  - Main Kotlin files: **143**.
  - Test Kotlin files: **25**.
- Focused JVM tests: **14 passed, 0 failed**.
- Android-free production seek components compiled successfully with `kotlinc`.
- New focused test sources compiled successfully.
- XML parse: **23 files, 0 errors**.
- GitHub Actions YAML parse: **1 file, 0 errors**.
- Source contract checks confirmed:
  - `PlayerActivity` owns one `TvSeekController`;
  - the focused `SeekBar` still installs a direct key listener;
  - the old `scrubTarget` field is removed;
  - queue changes cancel pending scrub state;
  - `onStop()` commits pending scrub before saving progress;
  - long-duration seek-bar mapping is used for both display and touch input.
- Version updated to `1.40.21` / `65`.

Evidence files:

- `FOCUSED_SEEK_TESTS_V1_40_21.txt`
- `KOTLIN_SYNTAX_V1_40_21.txt`
- `gradle_test_attempt_v1_40_21.txt`

## Full Gradle validation status

A full `:app:testDebugUnitTest` invocation was attempted. The local environment
cannot bootstrap the Gradle 8.9 wrapper because that distribution is not cached
and outbound DNS/network access is unavailable. The failure occurs before
project configuration or compilation and is recorded in
`gradle_test_attempt_v1_40_21.txt`.

The existing GitHub Actions workflow remains the authoritative full unit-test,
lint, debug assembly and release assembly gate.

## Recommended Android TV device regression

1. Open a movie and an episode using ExoPlayer.
2. Repeat using forced VLC mode.
3. Focus the seek bar and press LEFT/RIGHT once, rapidly and by holding.
4. Confirm a pending target using OK/ENTER.
5. Press Home immediately after a scrub, return and verify resume position.
6. Drag the bar by touch on a phone/tablet and verify preview and final seek.
7. Verify UP/DOWN still move focus away from the seek bar.
8. Verify live playback does not expose VOD seek behavior.
