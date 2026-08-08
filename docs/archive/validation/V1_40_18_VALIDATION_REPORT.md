# v1.40.18 validation report

## Reported compiler failure

`ExportScreen.kt` attempted to read `relayRunning` and `relayUrl` from `FavoritesUiState`, which only contained `favorites`. This caused:

- `Unresolved reference 'relayRunning'`
- `Unresolved reference 'relayUrl'`

## Implemented correction

A dedicated `ExportUiState` now carries `favorites`, `relayRunning`, and `relayUrl`. `ExportScreen` collects `MainViewModel.exportState` rather than the old favorites-only slice.

## Checks completed

- Kotlin PSI parse, production sources: `FILES=139 ERRORS=0`
- Kotlin PSI parse, test sources: `FILES=20 ERRORS=0`
- Focused Kotlin compile/runtime contract: `export-state-contract-ok`
- Repository scan confirms no references remain to:
  - `FavoritesUiState`
  - `favoritesState`
  - `toFavoritesUiState`
- Version confirmed as `1.40.18` / `62`.

## Full Android build limitation

A full `:app:compileDebugKotlin` was attempted. It did not reach Android/Kotlin compilation because this environment could not resolve `services.gradle.org` to download Gradle 8.9. The failure is recorded in `gradle_compile_attempt_v40_18.txt`.
