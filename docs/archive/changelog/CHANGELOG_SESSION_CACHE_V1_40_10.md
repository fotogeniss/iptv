# v1.40.10 — Session Catalog Cache

## Scope

This release implements only Performance Stabilization Step 1. It does not split UI state or change the visual design.

## Behavior

- Live, VOD and Series catalogs are cached only in process memory.
- The cache key includes source identity, content type and category selection.
- Returning to an already loaded tab restores the catalog without another provider request.
- No Live/VOD/Series catalog is persisted to disk.
- Manual Refresh invalidates the complete session catalog for the active source and performs a real provider download.
- Changing category selection invalidates only that source section after the new selection is confirmed.
- M3U download/parse is reused during the current app session and is never written as a catalog cache.
- Stalker/MAC commands are resolved with a source-bound provider session at Play time. If the portal token has expired, one fresh connection is attempted.
- Series episode lists loaded from Details are merged back into the in-memory Series snapshot.

## Memory bounds

- Maximum catalog snapshots: 3, using LRU eviction.
- Maximum full parsed M3U payloads: 1 source.
- History, resume positions and category choices remain in their existing stores and stay isolated per source.

## Changed files

- `app/src/main/java/com/prelude/iptv/data/SessionCatalogCache.kt` — new bounded process-memory cache.
- `app/src/main/java/com/prelude/iptv/ui/MainViewModel.kt` — cache restore/invalidation and source-bound Stalker resolution.
- `app/src/main/java/com/prelude/iptv/ui/route/PlaybackLaunchers.kt` — resolve URL through the ViewModel at Play time.
- `app/src/test/java/com/prelude/iptv/data/SessionCatalogCacheTest.kt` — cache isolation, signature and LRU tests.
- `app/build.gradle.kts` — version `1.40.10`, versionCode `54`.
- `SubtitleClient.kt` — user-agent version updated to `1.40.10`.
