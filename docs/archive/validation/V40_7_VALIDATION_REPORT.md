# v1.40.7 Validation Report

## Version

- versionName: `1.40.7`
- versionCode: `51`

## Validation performed

### Kotlin PSI syntax validation

- Parsed all production Kotlin files through Kotlin PSI.
- Files checked: 135
- Syntax errors: 0

### Pure Kotlin catalog contract compilation

Compiled `Models.kt` and the new `CatalogNormalizer.kt` with Kotlin/JVM and a minimal `org.json` compile stub.

Result: PASS

### Runtime contract scenarios

Executed JVM assertions for:

1. Three raw episode rows collapse into one series parent.
2. Seasons are ordered numerically.
3. Episodes are ordered by episode number.
4. Search returns one series and no `series_ep` results.
5. Legacy `vod` episode rows are reclassified as a series in search.
6. A playable Stalker-style row without SxxExx becomes one series with one playable episode.
7. Movies deduplicate by provider stream identity.
8. Live channels deduplicate by provider channel identity.
9. Xtream series containers remain untouched until `get_series_info` is requested.

Result: PASS

### Regression guards

- Forbidden explicit `import androidx.compose.foundation.layout.weight`: 0
- `RowColumnParentData` references: 0
- New production normalization file: 281 lines
- Source-scoped history code remains in `PlaylistStore`.
- Live playback route remains direct.
- Movie and Series card routes remain Details-first.

## Not performed

A full Android `compileDebugKotlin` / APK build was not run in this environment because the project archive does not include `gradle-wrapper.jar` and no Android SDK is installed here. The report therefore does not claim a successful Gradle build.
