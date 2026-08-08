# v1.40.14 Validation Report

## Scope

Route-level UI state slicing and recomposition-boundary reduction.

## Static validation

- Kotlin PSI syntax parser:
  - Files parsed: 139
  - Syntax errors: 0
- Full-state Compose collectors after migration: 0
- Slice collectors: 6
- Production Kotlin files: 139
- Test Kotlin files: 20
- No provider, parser, playback, history, refresh, or navigation write logic changed.

## Contract validation

A focused Kotlin contract harness compiled and ran successfully:

- Source progress changes do not change `CatalogUiState`.
- Source progress changes do change `CatalogProgressUiState`.
- Catalog/favorite/status changes do not change `AppShellUiState`.
- Added four JUnit projection-isolation tests in `UiStateSlicesTest.kt`.

Result: `STATE_SLICE_CONTRACTS=PASS`

## Gradle status

Attempted:

```text
./gradlew :app:compileDebugKotlin --stacktrace
```

The wrapper stopped before Android/Kotlin compilation because the environment could
not resolve/connect to `services.gradle.org` to download Gradle 8.9
(`UnresolvedAddressException`). Therefore this report does not claim a successful
full Android Gradle build.

## Expected performance effect

- Download percentage updates recompose only the progress component, not the full
  catalog route.
- Settings/EPG/favorites updates no longer invalidate the root app shell.
- Catalog updates no longer invalidate source-selection screens.

Device profiling is still required to quantify frame-time and recomposition changes.
