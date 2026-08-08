# v1.40.2 Validation Report

## Scope

Validation covers the fresh-catalog behavior, provider refresh path, source-scoped VOD/series history and resume positions, and preservation of the previous Compose `weight` build fix.

## Automated checks completed

- Kotlin PSI syntax parse: **148 Kotlin files, 0 syntax errors**.
- Focused Kotlin compilation with Android/JSON/Stalker-compatible stubs: `Models.kt`, `PlaybackQueue.kt`, `PlaylistIdentity.kt`, and `PlaylistStore.kt` compiled successfully.
- Runtime identity smoke checks passed:
  - renaming a source keeps the same namespace,
  - different accounts on the same Xtream server remain isolated,
  - generated identifiers do not expose raw usernames/secrets,
  - identity length is stable.
- Source audit confirmed there are no catalog cache save/load calls and no old `loadedCache` or `seriesCache` maps.
- Source audit confirmed all recent/resume calls receive a source ID.
- Source audit confirmed the real refresh route reaches `MainViewModel.refresh()`, invalidates transient provider state, resets Stalker/M3U session data, and requests fresh network data.
- HTTP audit confirmed no-cache/no-store request headers.
- Compose regression audit confirmed there is no explicit `androidx.compose.foundation.layout.weight` import and no `RowColumnParentData` reference.
- Version audit confirmed `1.40.2 / 46`.

## Behavior contracts

- Persistent Live/VOD/Series catalog cache: **disabled**.
- Old catalog cache cleanup: **enabled at startup**.
- VOD/series history: **kept**.
- Live history/resume: **not stored**.
- History isolation: **source + profile**.
- Source deletion: **clears only that source's history/resume**.
- Section open: **fresh provider request**.
- Explicit refresh: **new provider session/request**.
- Series episode open: **fresh episode API request**.

## Build limitation

A full Android `clean assembleDebug` was not executed in this environment because the project archive does not include a real `gradle/wrapper/gradle-wrapper.jar`, and an Android SDK is not available here. The report therefore does not claim a complete Android Gradle build; the checks above are syntax, focused compilation, runtime policy smoke tests and source-contract audits.
