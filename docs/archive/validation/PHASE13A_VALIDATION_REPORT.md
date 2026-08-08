# Phase 13A Architecture Split — Validation Report

## Build target

- Base project: 1.39.1 / versionCode 43
- Output project: 1.40.0 / versionCode 44
- Scope: first production architecture split

## Structural results

- `MainActivity.kt`: 2,801 → 278 lines
- `ui/Shell.kt`: 523 → 129 lines
- New `ui/route/` package: 14 files
- 13 of the 14 route files are below 300 lines
- `SettingsRoute.kt`: 143 lines
- Settings dialog state is explicitly hoisted through `MutableState` and callbacks
- All 36 original MainActivity top-level function names remain available after extraction
- Two new settings dialog composables were added

## Validation performed

- Kotlin compiler PSI parser executed against all 131 production Kotlin files: zero syntax diagnostics.
- Original/new declaration inventory comparison: no original MainActivity top-level functions missing.
- Explicit Compose weight-import audit: zero `androidx.compose.foundation.layout.weight` imports.
- Internal Compose parent-data audit: zero `RowColumnParentData` references.
- Route integration audit: every function called by `MainActivity` has exactly one internal declaration in `ui/route/`.
- Version audit: 1.40.0 / versionCode 44.
- ZIP integrity check performed after packaging.

## What was not claimed

A full Android Gradle build was not executed in this environment. The supplied project still has a placeholder `gradlew`, no `gradle-wrapper.jar`, and this runtime has no Android SDK. Android Studio or CI must run `clean :app:compileDebugKotlin` and the normal test/lint pipeline.

## Remaining architecture work

This is Phase 13A wave 1, not the complete Phase 13 Definition of Done. Remaining high-priority UI hotspots are:

- `PlayerActivity.kt` — 2,950 lines
- `MainViewModel.kt` — 1,317 lines
- `ui/route/BrowseRoute.kt` — 809 lines
- `AddPlaylistScreen.kt` — 490 lines

The next wave should split the browse coordinator and player overlay/controller while preserving engine behavior.
