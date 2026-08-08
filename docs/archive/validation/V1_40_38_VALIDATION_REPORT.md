# Ultimate Playlist Loader v1.40.38 Validation Report

## Version

- `versionName`: `1.40.38`
- `versionCode`: `82`
- Baseline: `v1.40.37`

## Implemented scope

### Section chooser

- Added `Όλα` before Live TV, Movies and Series.
- Bulk import order is Live TV → Movies → Series.
- Provider access is sequential.
- The first available section is published immediately; remaining sections continue in the background.

### Progressive browsing

- Existing content remains composable while `loading == true`.
- Xtream selected categories are fetched sequentially and publish accumulated immutable snapshots.
- Stalker selected categories publish accumulated snapshots after each category.
- Partial callbacks are generation/source guarded before updating UI state.

### Source switching

- Cancels active source jobs.
- Clears visible channels/groups and source session snapshots.
- Clears old EPG state.
- Invalidates the target provider session and requests fresh data when a remembered selection exists.
- The previous playlist is not shown while the new source loads.

### Mobile overlay/insets

- Mobile bottom navigation is hidden while content chooser, category picker, load-mode or refresh-mode overlays are open.
- Category picker applies navigation-bar padding.

### Multiview display

- Both 50/50 `TextureView` panes now use `resize_mode="fit"` to preserve the full video frame instead of cropping it.

### Public EPG discovery

- Downloads the static public iptv-org guide directory.
- Matches candidate XML/XML.GZ sources locally using live-channel `tvg-id` values.
- Sends no playlist URL, provider URL, username, password or MAC address to the directory endpoint.
- Candidate list is deduplicated and ranked by exact channel-id matches.
- Directory metadata is cached in memory for six hours.

## Checks completed

- Provider semantic compilation with Kotlin stubs:
  - `XtreamClient.kt`
  - `StalkerClient.kt`
  - `Repository.kt`
- EPG directory semantic compilation with Kotlin stubs.
- MainViewModel parser/syntax-risk scan: no parser diagnostics.
- 14 focused source/behavior contract assertions: passed.
- XML parse: 24 files, 0 errors.
- Version contract: passed.
- Patch whitespace check: no diagnostics.
- ZIP integrity: passed.
- SHA-256 generated.

## Gradle status

Command attempted:

```text
./gradlew :app:compileDebugKotlin --stacktrace
```

The wrapper stopped before Gradle/Android compilation because Gradle 8.9 was not cached and `services.gradle.org` could not be resolved (`ConnectException` / `UnresolvedAddressException`). Therefore this report does **not** claim a full Android Kotlin compile, lint run, APK build or device test.

## Device verification still required

- Switch between two real playlists and confirm no previous-source catalog is visible.
- Start `Όλα`; browse Live while Movies/Series continue loading.
- Verify category picker on a phone with gesture and three-button navigation.
- Verify Multiview frame geometry with channels that previously appeared cropped.
- Open EPG search on a playlist with valid `tvg-id` values and test a matched public guide.
