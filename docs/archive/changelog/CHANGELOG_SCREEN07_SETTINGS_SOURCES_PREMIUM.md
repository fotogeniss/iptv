# Screen 07 — Premium Settings / Sources

Version: 1.38.0 (41)

## UI architecture

- Added `AdaptiveSettingsScreen` as the platform dispatcher.
- Added shared source/settings models and reusable rows under
  `ui/components/settings/`.
- Added independent touch-first Mobile UI under `ui/mobile/settings/`.
- Added independent DPAD-first Android TV UI under `ui/tv/settings/`.
- Removed the obsolete inline settings overview/summary composables from
  `MainActivity.kt`.

## Real source integration

- Uses the existing `Playlist` and `PlaylistType` models directly.
- M3U, Xtream and Stalker add flows continue through the existing
  `AddPlaylistScreen`.
- Opening, editing and deleting a source continue through the existing
  `MainViewModel` contracts.
- Source deletion now has an explicit confirmation before cache and local
  source data are removed.
- Displayed endpoints are host-only. User info, passwords, tokens and MAC
  addresses are never rendered by the settings UI.

## Existing settings preserved

- Player engine selection.
- EPG enable/disable.
- TMDB API configuration and cache clearing.
- OpenSubtitles settings.
- Font scale.
- Profiles and parental PIN.
- Backup/restore and app sharing.

## Navigation

- Android TV uses its own settings sidebar and hides the legacy bottom bar on
  the settings destination.
- Mobile keeps the existing application bottom navigation, avoiding duplicate
  navigation chrome.
