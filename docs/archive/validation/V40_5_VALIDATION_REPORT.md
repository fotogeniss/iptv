# v1.40.5 Validation Report

## Scope
This patch addresses the player audio-track selector and the catalog/settings/source-selection navigation behavior.

## Source checks
- Kotlin parser check: 132 production Kotlin files, 0 syntax diagnostics.
- Audio sheet invokes the option action before dismissing/removing the clicked row.
- Sheet panel consumes touches and prevents fall-through to the fullscreen player/scrim.
- VLC options use each `TrackDescription.id`, compare against the current `audioTrack`, and report the post-selection track id.
- ExoPlayer options derive selection from `Tracks.Group.isTrackSelected(index)` and re-enable audio before applying an override.
- Settings opened from the catalog returns to the catalog on Back.
- Catalog Back / TV Sources asks for confirmation before source selection.
- No explicit `androidx.compose.foundation.layout.weight` import and no `RowColumnParentData` reference.

## Version
- `versionName = 1.40.5`
- `versionCode = 49`

## Limitation
A full Android Gradle build and on-device VLC playback test were not run in this environment because it does not contain a usable Android SDK and Gradle wrapper JAR. The changes were validated through Kotlin syntax parsing and source-contract checks; the final runtime confirmation must come from the user's device/build environment.
