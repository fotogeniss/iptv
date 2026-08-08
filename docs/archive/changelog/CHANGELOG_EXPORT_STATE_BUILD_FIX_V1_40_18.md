# v1.40.18 — Export state build fix

## Fixed

- Replaced the export screen's incompatible `FavoritesUiState` subscription with a dedicated `ExportUiState`.
- `ExportUiState` now exposes exactly the fields required by `ExportScreen`:
  - `favorites`
  - `relayRunning`
  - `relayUrl`
- Added `MainViewModel.exportState` with `distinctUntilChanged()` so relay status updates only recompose the Export/Relay screen.
- Updated the state-slice unit contract to cover relay start/stop changes.
- Removed all remaining project references to the obsolete `favoritesState`, `FavoritesUiState`, and `toFavoritesUiState` names.

## Version

- versionName: `1.40.18`
- versionCode: `62`
