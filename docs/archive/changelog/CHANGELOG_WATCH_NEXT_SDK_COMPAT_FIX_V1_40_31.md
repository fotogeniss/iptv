# v1.40.31 — Watch Next SDK compatibility fix

## Scope

Targeted Kotlin compile fix for `LegacyWatchNextPublisher.kt`.

## Change

- Removed direct Kotlin references to:
  - `TvContract.WatchNextPrograms.ASPECT_RATIO_1_1`
  - `TvContract.WatchNextPrograms.ASPECT_RATIO_2_3`
- Added private local contract values used only for `COLUMN_POSTER_ART_ASPECT_RATIO`:
  - `POSTER_ASPECT_RATIO_1_1 = 3`
  - `POSTER_ASPECT_RATIO_2_3 = 4`
- No playback, provider, UI, navigation, database, or lifecycle behavior changed.
- Version bumped to `1.40.31`, `versionCode 75`.

## Reason

The real Android Kotlin compile environment reported the inherited `ASPECT_RATIO_*` symbols as unresolved on `WatchNextPrograms`. Keeping the documented integer contract values local avoids dependence on that SDK/Kotlin symbol exposure while preserving the provider payload.
