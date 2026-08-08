# v1.40.14 — UI State Slices

## Goal

Reduce broad Compose recompositions without changing provider, navigation, playback,
history, refresh, or source behavior.

## Changes

- Added `ui/UiStateSlices.kt` with route-specific read models:
  - `AppShellUiState`
  - `CatalogUiState`
  - `CatalogProgressUiState`
  - `SettingsUiState`
  - `EpgUiState`
  - `FavoritesUiState`
- `MainViewModel` now exposes `StateFlow` projections using
  `map + distinctUntilChanged + stateIn`.
- The root activity observes only playlists, selected source index, and font scale.
- The catalog observes catalog fields only.
- High-frequency source download percentages are collected by the small
  `CatalogLoadingProgress` composable instead of invalidating the full browse route.
- Settings observes source/settings fields only.
- XMLTV dialog observes EPG fields only.
- Export observes favorites only and now uses lifecycle-aware collection.
- Playlist and Xtream source screens accept `AppShellUiState` instead of the full state.
- Details accepts `CatalogUiState` instead of the full state.

## Compatibility

The original `StateFlow<UiState>` remains available as a compatibility stream for
business code. No write path or data model was migrated in this step.

## Version

- `versionName`: `1.40.14`
- `versionCode`: `58`
