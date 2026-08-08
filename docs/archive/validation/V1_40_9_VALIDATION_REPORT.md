# v1.40.9 Validation Report

## Scope

1. Mobile Home «Προβολή όλων».
2. Structured OpenSubtitles queries for movies and series episodes.
3. Subtitle identity preservation during episode prev/next and autoplay.

## Source checks

- Production Kotlin PSI syntax: `137 files, 0 errors`.
- Test Kotlin PSI syntax: `18 files, 0 errors`.
- No explicit `androidx.compose.foundation.layout.weight` imports.
- No `RowColumnParentData` references.
- All `MobilePremiumHomeRail` call sites updated for `onViewAll`.
- The single `DetailHost` call site updated for subtitle request maps.

## Focused compilation/runtime checks

- `SubtitleSearchPolicy.kt` compiled with Kotlin/JVM stubs.
- `SubtitleClient.kt` compiled with controlled Android/JSON/HTTP stubs.
- Movie normalization runtime check:
  - `GR: 4K Color Book (2024) [MULTI]` → `Color Book`, year `2024`.
- Episode normalization runtime check:
  - `Chernobyl S01 E02` → title `Chernobyl`, season `1`, episode `2`.
- API parameter contract:
  - `query=Chernobyl`
  - `type=episode`
  - `season_number=1`
  - `episode_number=2`
- Premium rail policy runtime check:
  - preview `20` items / full category `34` items.

## Full Gradle build

Attempted:

```text
./gradlew :app:compileDebugKotlin --stacktrace
```

The wrapper could not download Gradle because this environment cannot resolve external network addresses (`UnresolvedAddressException`). Therefore a real Android SDK/Gradle compile was not completed here. Run `scripts/verify-debug.bat` or `scripts/verify-debug.sh` in the local Android development environment.
