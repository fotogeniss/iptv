# v1.40.22 — Android TV Auto Frame Rate

## User-visible changes

- Added **Auto Frame Rate** to Android TV Settings → Playback.
- Added three modes:
  - **Off**: keep the system refresh rate.
  - **Seamless**: request matching only when the platform can switch without a
    disruptive display-mode transition.
  - **Full**: request a concrete matching display mode; some TVs may briefly
    show a black screen while HDMI timing changes.
- The setting is disabled by default and does not affect touch/mobile devices.

## Playback integration

- ExoPlayer now explicitly enables Media3's seamless frame-rate strategy only
  when the user selects Seamless mode.
- Full mode disables ExoPlayer's internal seamless-only request and applies the
  app's display-mode selection policy.
- Selected ExoPlayer video tracks provide their declared frame rate to the
  display controller.
- VLC provides frame rate from its active video track after video output starts.
- Display requests are suspended while the Activity is in the background,
  reapplied on return and fully cleared when the player closes.

## Safety policy

- Only display modes with the **same physical resolution** as the current mode
  are eligible.
- Matching requires an integer refresh-rate multiple, so 24 fps does not accept
  60 Hz and its 3:2 judder cadence.
- Invalid, missing or implausible frame rates cause no forced mode change.
- Common fractional clock families such as 23.976/47.952 and 29.97/59.94 are
  handled with a narrow tolerance.

## Architecture and tests

- Added `AutoFrameRateMode` and the Android-free `FrameRateMatchPolicy`.
- Added `DisplayFrameRateController` as the platform adapter.
- Added JVM tests for mode parsing, invalid rates, 24-vs-60 rejection,
  same-resolution safety and common cinema/broadcast timings.

## Version

- `versionName`: `1.40.22`
- `versionCode`: `66`
