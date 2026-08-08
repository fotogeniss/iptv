# v1.40.29 TV Provider Compile Fix

## Scope

Targeted Kotlin compile fix for the Android TV My List preview channel and Watch Next publishers. No provider, playback, Multiview, UI, or lifecycle behavior was refactored.

## Fix

- `LegacyMyListChannelPublisher` now uses program-type constants from `TvContract.PreviewPrograms`.
- `LegacyWatchNextPublisher` now uses program-type constants from `TvContract.WatchNextPrograms`.
- Removed invalid references to `TvContract.Programs.TYPE_TV_EPISODE`, `TYPE_CHANNEL`, and `TYPE_MOVIE`.
- Bumped application version to `1.40.29` / `versionCode 73`.

The column and the value constants now come from the same Android TV contract class, matching the Android framework API surface used by each publisher.
