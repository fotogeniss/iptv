# v1.40.30 Watch Next Aspect Ratio Compile Fix

## Scope

Targeted Kotlin compile correction on top of v1.40.29. No playback, provider, Multiview, UI, lifecycle, or data-flow behavior was refactored.

## Fix

- `LegacyWatchNextPublisher` now reads poster-art aspect-ratio constants from `TvContract.WatchNextPrograms`.
- Removed invalid references to `TvContract.Programs.ASPECT_RATIO_2_3` and `TvContract.Programs.ASPECT_RATIO_1_1`.
- Bumped application version to `1.40.30` / `versionCode 74`.

The Watch Next column and its allowed values now come from the same concrete Android TV contract class.
