# v1.40.21 — Player Architecture and Seek Stability

## Architecture

- Moved Android TV seek-key routing rules into the Android-free
  `TvSeekKeyPolicy`.
- Moved pending scrub target state into `TvSeekController`.
- Added `PlaybackSeekPolicy` as the shared source of truth for absolute and
  relative seek boundary handling.
- Added `SeekBarPositionMapper` to safely translate `Long` media timelines to
  Android's `Int`-based `SeekBar` range.
- `PlayerActivity` now acts mainly as the Android/ExoPlayer/libVLC adapter for
  these policies instead of owning all seek logic directly.

## Stability fixes

- Repeated LEFT/RIGHT presses accumulate deterministically into one pending
  target and commit once after the debounce window.
- DPAD key-up remains consumed, preventing Android `SeekBar` from applying an
  extra visual-only movement.
- OK/ENTER commits a pending target once; repeated long-press events are
  consumed without toggling play/pause repeatedly.
- All seeks clamp to zero and, when duration is known, to the content end.
- Unknown-duration IPTV VOD keeps immediate relative seek behavior.
- Pending TV scrub state is cancelled when touch tracking starts or content is
  changed.
- A pending target is committed before `onStop()` saves resume progress, so a
  quick Home press does not persist the old position.
- Timelines longer than `Int.MAX_VALUE` milliseconds no longer overflow the
  seek-bar max/progress values.

## Tests

Added JVM unit coverage for:

- repeated scrub accumulation;
- lower and upper boundary clamping;
- unknown-duration fallback;
- cancellation and single commit;
- LEFT/RIGHT/OK key routing and repeat behavior;
- safe absolute/relative seek math;
- very long timeline seek-bar mapping.

## Version

- `versionName`: `1.40.21`
- `versionCode`: `65`
