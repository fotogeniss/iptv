# Phase 13A — Architecture Split

Version: 1.40.0 (versionCode 44)

## Scope

This wave changes UI ownership only. Data repositories, source parsing, TMDB, EPG, history, favorites, search behavior, ExoPlayer and VLC playback were not replaced.

## MainActivity split

`MainActivity.kt` was reduced from 2,801 to 278 lines and now owns only:

- Activity lifecycle and theme installation
- Profile gate
- Root tab/browse state
- Top-level route presentation
- Add/edit/source modal routing

The previous top-level UI declarations were moved to `ui/route/`:

- source management and provider import screens
- browse route and browse components
- details route host
- settings route and settings dialogs
- playback launch helpers

## Shell split

`ui/Shell.kt` was reduced from 523 to 129 lines. Its public API is preserved while implementations were separated into:

- `SourceOnboardingComponents.kt`
- `SettingsShellComponents.kt`
- `TvInteraction.kt`
- `TextEntryDialog.kt`

## Settings state hoisting

Large settings dialogs were extracted from the route coordinator into:

- `SettingsAccountDialogs.kt`
- `SettingsPlaybackDialogs.kt`

The route keeps the launcher/state ownership and passes explicit state/callbacks to the dialog components.

## Build fix retained

No source file imports `androidx.compose.foundation.layout.weight`, and there are no `RowColumnParentData` references.

## Remaining Phase 13A hotspots

The following files intentionally remain for the next architecture wave:

- `PlayerActivity.kt`
- `MainViewModel.kt`
- `ui/route/BrowseRoute.kt`
- `AddPlaylistScreen.kt`

