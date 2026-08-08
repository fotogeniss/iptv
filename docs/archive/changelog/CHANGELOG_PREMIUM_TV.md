# Premium TV refactor changelog

## Catalog shell

- `ui/PremiumTvNavigation.kt`: compact/expanded DPAD navigation rail.
- `ui/PremiumCatalog.kt`: reusable focus-safe TV rails and cards.
- `ui/PremiumCatalogPolicy.kt`: deterministic catalog section policy.
- `ui/PremiumTvHero.kt`: initial focus lands on Play.

## Integration

- `MainActivity.kt` routes Movies/Series into the new TV catalog while keeping
  Live TV on the existing fast channel/EPG flow.
- Settings and source management are reachable directly from the TV rail.
- Mobile behavior remains unchanged.

## Player/details fixes

- Live playback now exposes previous, play/pause and next as a coherent center
  control group.
- Seek progress uses the product red and a visible TV thumb.
- Series detail state no longer depends on a non-empty seasons response.
- Empty series responses show an explicit provider-data message instead of a
  broken movie Play action.

## Validation performed

- Kotlin parser pass on all changed files (Android symbols unresolved in this
  environment are expected; no syntax errors were reported).
- Executable JVM smoke test for the catalog section policy.
- Structural checks for duplicate EPG manager declarations and ZIP integrity.

A full Android Gradle build still requires an Android SDK/build environment.
