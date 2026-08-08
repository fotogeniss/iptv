# v1.40.12 Validation Report

## Scope

Build fix for `TvPremiumSettingsScreen.kt` plus persisted subtitle-size controls in the player.

## Checks completed

- Kotlin PSI syntax parse of all Kotlin files under `app/src`.
- Result: `FILES=157 ERRORS=0`.
- Verified `OutlinedButton` and `Refresh` have explicit valid imports.
- Verified subtitle-size range is clamped to 70–180%.
- Verified the preference is stored per profile.
- Verified the custom subtitle TextView updates immediately.
- Verified the Media3 `PlayerView.subtitleView` is updated both during player creation and after user changes.
- Verified version bump to `1.40.12 / 56`.
- Verified the old invalid `androidx.compose.foundation.layout.weight` import is absent.

## Environment limitation

A full Android Gradle compile was attempted, but the container could not download the Gradle distribution because outbound DNS/network resolution is blocked. Therefore this report does not claim a successful `compileDebugKotlin` or device-level VLC test.

## VLC embedded-subtitle note

External SRT/OpenSubtitles are rendered by the app and resize on both engines. Media3 embedded tracks resize through `SubtitleView`. libVLC's Android Java API used by this project does not expose an equivalent live subtitle-font-size setter in the current integration, so the control cannot guarantee resizing of VLC-internal embedded subtitle rendering.
