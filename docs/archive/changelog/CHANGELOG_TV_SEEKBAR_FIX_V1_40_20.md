# v1.40.20 — Android TV Seek-Bar Fix

## Fixed

- Fixed DPAD LEFT/RIGHT on the focused VOD seek bar.
- The Android framework `SeekBar` previously consumed those key events before
  `PlayerActivity.onKeyDown()`, so the thumb could move visually without the
  ExoPlayer/libVLC position actually changing.
- The seek bar now handles remote keys directly before framework default
  processing and routes them through the existing debounced `scrubBy()` path.
- LEFT rewinds 10 seconds and RIGHT advances 10 seconds.
- Repeated key-down events continue scrubbing while the button is held, but are
  coalesced into a single player seek 400 ms after the final step.
- DPAD CENTER / ENTER commits a pending scrub immediately; otherwise it toggles
  play/pause. Long-press repeats are ignored for confirm keys to prevent rapid
  play/pause toggling.
- Key-up events are consumed so the framework cannot apply a second visual-only
  progress change after the application-level scrub.

## Version

- `versionName`: `1.40.20`
- `versionCode`: `64`
