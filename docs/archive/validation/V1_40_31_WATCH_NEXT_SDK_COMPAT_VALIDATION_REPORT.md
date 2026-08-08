# v1.40.31 Watch Next SDK compatibility validation

## Result

The reported `LegacyWatchNextPublisher.kt` unresolved references for `ASPECT_RATIO_2_3` and `ASPECT_RATIO_1_1` were removed.

## Production changes

- `app/src/main/java/com/prelude/iptv/tvhome/LegacyWatchNextPublisher.kt`
  - uses local private constants `POSTER_ASPECT_RATIO_1_1 = 3` and `POSTER_ASPECT_RATIO_2_3 = 4`
  - contains no `TvContract.WatchNextPrograms.ASPECT_RATIO_*` references
- `app/build.gradle.kts`
  - `versionName = "1.40.31"`
  - `versionCode = 75`

## Checks executed

### Focused Kotlin semantic compile — PASS

The real production `LegacyWatchNextPublisher.kt` was compiled with `kotlinc` against a focused Android API stub that intentionally omits `WatchNextPrograms.ASPECT_RATIO_*`. The publisher class and nested classes were emitted successfully.

Evidence: `focused_watch_next_sdk_compat_compile_v1_40_31.txt`.

### Namespace regression scan — PASS

No `TvContract.WatchNextPrograms.ASPECT_RATIO_*` references remain in `LegacyWatchNextPublisher.kt`.

### XML parse — PASS

- XML files parsed: 23
- Parse errors: 0

Evidence: `xml_validation_v1_40_31.txt`.

### Version contract — PASS

- versionName: 1.40.31
- versionCode: 75

### Full Gradle compile — NOT COMPLETED IN THIS ENVIRONMENT

Attempted command:

```text
./gradlew :app:compileDebugKotlin --no-daemon --stacktrace
```

The wrapper failed before Gradle startup because Gradle 8.9 is not cached and the environment cannot resolve/connect to `services.gradle.org`. No Android/Kotlin Gradle compilation result is claimed.

Evidence: `gradle_compile_attempt_v1_40_31.txt`.

## Scope control

No unrelated refactor was performed. The production diff is limited to the two aspect-ratio references, two private constants, and the version bump.
