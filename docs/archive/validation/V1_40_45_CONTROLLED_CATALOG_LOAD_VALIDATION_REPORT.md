# v1.40.45 Controlled Catalog Load Coordinator — Validation Report

## Scope

This release extracts one responsibility only: provider catalog loading and normalization. It does not change player engines, navigation, TV focus, EPG behavior, source identity, persistence schema or public ViewModel entry points.

## Changed production files

- `app/src/main/java/com/prelude/iptv/ui/coordinator/CatalogLoadCoordinator.kt` — new provider/catalog boundary.
- `app/src/main/java/com/prelude/iptv/ui/MainViewModel.kt` — delegates category and section loading, progressive normalization and shared provider serialization.
- `app/build.gradle.kts` — version `1.40.45`, versionCode `89`.

## Changed tests and gates

- `CatalogLoadCoordinatorTest.kt` — five focused tests.
- `scripts/compatibility_contracts.py` — catalog delegation and rollback contracts.
- `scripts/architecture_audit.py` — coordinator ownership and legacy-dispatch removal contracts.
- `scripts/deep_validation_audit.py` / `risk_inventory.py` — release version updated.

## Compatibility

Frozen v1.40.41 API contracts remain intact:

- `PlayerActivity`: 10/10 public members preserved.
- `MainViewModel`: 89/89 public members preserved.
- No public route or callback signature was removed.

## Functional contracts

### Provider boundary

- One mutex serializes high-level catalog and series provider requests.
- M3U, Xtream and Stalker category discovery use the same coordinator entry point.
- M3U, Xtream and Stalker section loading use the same coordinator entry point.
- Progressive snapshots and final snapshots pass through the same `CatalogNormalizer` boundary.

### Refresh transaction

- Forced refresh captures the visible catalog before partial publication.
- Refresh with new groups captures the visible catalog before partial publication.
- A successful refresh publishes the fresh normalized result and commits the selected group IDs.
- A failed refresh restores channels, groups, favorites and selected visible group exactly.
- A stale/cancelled source cannot roll back over a newer source because `loadGen` is checked first.

## Automated results

- Focused runtime assertions: **8/8 passed**.
- Focused actual test-source semantic compile: **passed**.
- Coordinator production semantic compile: **passed**.
- Compatibility contracts: **21 pass, 0 fail**.
- Architecture audit: **35 pass, 2 warnings, 0 fail**.
- Deep validation audit: **47 pass, 1 warning, 0 fail**.
- Production critical-risk categories: **0 failures**.
- Changed Kotlin syntax scan: **0 findings**.
- XML parse: **24 files, 0 errors**.

The two architecture warnings are unchanged legacy size warnings:

- `PlayerActivity.kt`: 3,228 lines.
- `MainViewModel.kt`: 1,844 lines, reduced from 1,894 in v1.40.44.

The deep-validation warning remains the documented global cleartext compatibility exception for user-provided HTTP IPTV endpoints.

## Gradle/build status

The following real gate was attempted:

```text
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --stacktrace --no-daemon
```

The wrapper failed before Gradle startup while downloading Gradle 8.9 from `services.gradle.org` with `ConnectException` / `UnresolvedAddressException`.

Therefore this report does **not** claim:

- complete Android compilation
- execution of the full JUnit suite through Gradle
- Android lint execution
- APK generation
- emulator/device verification

## Next controlled seam

`SourceSwitchCoordinator`: generation cancellation, old-source invalidation and automatic initial-load routing, while preserving all existing public `MainViewModel` methods.
