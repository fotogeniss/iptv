# v1.35 — Screen 04 Premium Library / My List

## Scope

The legacy flat Library grids were replaced with independent premium Mobile and
Android TV compositions while keeping favorites, history, watch progress,
playback, repositories and persistence unchanged.

## Shared Library foundation

- Added `PremiumLibraryContent`, `LibraryHubTab`, `LibrarySort` and reusable rail
  policies under `ui/components/library/`.
- Added a single cinematic backdrop, artwork, progress and badge foundation used
  by both form factors.
- The hub combines My List, Continue Watching and History without duplicating
  item identity or progress logic.
- TMDB metadata is loaded only for the currently selected hero item.

## Android TV

- Full-screen cinematic background driven by DPAD focus.
- Library summary, tabs, sorting and management controls.
- Premium-style horizontal rails with scale, brightness and shadow focus.
- Contextual info panel with real metadata, progress, playback and removal
  actions.
- The existing global TV navigation rail remains the only sidebar.

## Mobile

- Touch-first cinematic hero with Play, Details and My List actions.
- Summary cards and horizontally scrollable Library tabs.
- Independent Continue, My List and History rails.
- Real multi-select management mode with batch removal.
- Premium bottom navigation connected to Home, Search, Live, My List and
  Settings.

## Architecture

```text
ui/components/library/
    LibraryFoundation.kt
    LibraryArtwork.kt

ui/mobile/library/
    MobilePremiumLibraryScreen.kt
    MobileLibraryComponents.kt
    MobileLibrarySections.kt

ui/tv/library/
    TvPremiumLibraryScreen.kt
    TvLibraryHeader.kt
    TvLibraryComponents.kt
```

The old inline implementation inside `PremiumLibraryScreen.kt` is now an
adaptive dispatcher. All new production files remain below 300 lines.

## Version

- `versionName`: 1.35.0
- `versionCode`: 38
