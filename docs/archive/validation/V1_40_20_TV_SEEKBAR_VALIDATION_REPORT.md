# v1.40.20 TV Seek-Bar Validation Report

## Root cause

Android's standard `SeekBar` handles DPAD LEFT/RIGHT inside the focused view.
Those events therefore did not reliably reach `PlayerActivity.onKeyDown()`.
The default widget changed its progress value, but the touch-only
`OnSeekBarChangeListener.onStopTrackingTouch()` callback was never invoked, so
no real ExoPlayer/libVLC seek occurred.

## Implemented fix

- Added a direct `OnKeyListener` to the VOD seek bar.
- Consumed LEFT, RIGHT, CENTER and ENTER key down/up events before default
  `SeekBar` processing.
- Routed LEFT/RIGHT through the existing 10-second debounced scrub mechanism.
- Kept UP/DOWN unconsumed so normal TV focus navigation still works.
- Preserved the Activity-level handler as a fallback.
- Prevented repeated OK/ENTER key-down events from toggling play/pause more than
  once on a long press.

## Validation completed

- Kotlin PSI parse of `app/src/main`: **142 files, 0 syntax errors**.
- Kotlin PSI parse of `app/src/test`: **21 files, 0 syntax errors**.
- Source contract audit confirmed:
  - the seek bar installs its key listener;
  - LEFT calls `scrubBy(-DOUBLE_TAP_MS)`;
  - RIGHT calls `scrubBy(DOUBLE_TAP_MS)`;
  - key-up is consumed;
  - UP/DOWN remain available to focus navigation;
  - pending scrubs still use the existing 400 ms single-seek debounce.
- Version updated to `1.40.20` / `64`.

## Full Gradle validation status

The local environment could not bootstrap the Gradle 8.9 wrapper because the
distribution is not cached and outbound network/DNS access is unavailable. The
failure is saved in `gradle_compile_attempt_v40_20.txt`. The existing GitHub
Actions workflow remains the authoritative full build, lint and unit-test gate.

## Recommended device check

On Android TV, open a movie or episode, show the controls, move focus down to
the progress bar, and verify:

1. LEFT changes the preview by -10 seconds.
2. RIGHT changes the preview by +10 seconds.
3. Rapid presses produce one final seek after the last press.
4. Holding LEFT/RIGHT keeps moving the target.
5. UP returns focus to play/pause and DOWN moves to the toolbar.
6. OK commits a pending target immediately.
