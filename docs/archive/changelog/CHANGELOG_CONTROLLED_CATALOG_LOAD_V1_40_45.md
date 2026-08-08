# Changelog — v1.40.45 Controlled Catalog Load Coordinator

## Architecture

- Added `CatalogLoadCoordinator` as the single serialized provider boundary for catalog category discovery and section loading.
- Moved M3U, Xtream and Stalker dispatch out of `MainViewModel`.
- Moved progressive and final catalog normalization into the coordinator.
- Removed the legacy `fetchChannels` function and `providerLoadMutex` ownership from `MainViewModel`.
- Kept all existing public `MainViewModel` methods and UI routes unchanged.
- Existing series-detail provider calls now share the coordinator's provider mutex, preserving the one-provider-request-at-a-time contract.

## Behavior preserved

- Category selection remains source- and section-scoped.
- Live TV, Movies and Series continue to load progressively.
- The “Όλα” flow still loads sections sequentially and publishes the first available section immediately.
- M3U group filtering, Xtream endpoints and Stalker session reuse are unchanged.
- Source-generation guards remain in `MainViewModel`; stale source results are still rejected before state publication.
- Successful refresh commits category choices only after the final catalog succeeds.

## Bug fixed

Progressive publication introduced a transactional regression: during refresh, partial fresh items replaced the visible catalog; if the provider then failed, the UI stayed on the incomplete partial result even though the stored group selection had correctly remained unchanged.

The refresh flow now captures the exact visible catalog before network work and restores on failure:

- content type
- channels/items
- groups
- favorites
- selected visible group

Normal initial loads are not rolled back, and source-switch cancellation never restores data into a newer source because generation checks run before rollback.

## Validation

- Coordinator semantic compile with dependency stubs: passed.
- Actual focused test source semantic compile with JUnit/coroutine-test stubs: passed.
- Runtime coordinator harness: 8 assertions passed.
- Compatibility contracts: 21 pass, 0 fail.
- Architecture contracts: 35 pass, 2 known size warnings, 0 fail.
- Deep validation contracts: 47 pass, 1 documented cleartext warning, 0 fail.
- Kotlin changed-file syntax scan: 0 parser findings.
- XML parse: 24 files, 0 errors.
- Full Gradle gate attempted but blocked before Gradle startup because Gradle 8.9 could not be downloaded in the current environment.
