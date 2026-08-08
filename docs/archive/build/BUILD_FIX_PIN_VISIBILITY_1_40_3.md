# Build Fix 1.40.3 — Cross-file dialog visibility

## Reported compiler error

`Cannot access 'fun PinDialog(...)': it is private in file.`

The build also pointed at callers in `BrowseRoute.kt`, `PlaylistSourcesScreen.kt`, `SettingsAccountDialogs.kt`, and `SettingsPlaybackDialogs.kt`.

## Root cause

During the Phase 13A file split, three top-level composables remained file-private even though their callers moved into different Kotlin files inside the same `com.prelude.iptv.ui.route` package:

- `PinDialog`
- `StepBtn`
- `DeleteConfirmDialog`

Kotlin top-level `private` visibility is limited to the declaring file, not the package.

## Fix

Changed only the required declarations from `private` to `internal`:

- `SettingsFieldComponents.kt`
  - `StepBtn`
  - `PinDialog`
- `ProviderImportScreens.kt`
  - `DeleteConfirmDialog`

No dialog behavior, state, navigation, source loading, playback, history, or cache policy was changed.

## Validation performed

- Audited every top-level private declaration in `ui/route` for cross-file references.
- Confirmed zero remaining cross-file calls to file-private declarations.
- Ran Kotlin PSI syntax validation across all production Kotlin files: zero syntax errors.
- Confirmed the previous forbidden explicit `foundation.layout.weight` import and `RowColumnParentData` reference are absent.
- Verified ZIP integrity after packaging.

## Version

- `versionName`: `1.40.3`
- `versionCode`: `47`

A full Android Gradle build was not run in this environment because the archive does not contain a usable Gradle wrapper JAR and the Android SDK is unavailable here.
