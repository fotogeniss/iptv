# v1.40.13 — Provider Request Control

## Goal

Second performance-stabilization step after the session-only catalog cache. The app now treats Live, VOD and Series loading as latest-request-wins work instead of allowing obsolete provider requests to keep running after a tab or source change.

## Changes

- Added dedicated catalog and series `Job` ownership in `MainViewModel`.
- Switching content type, refreshing, changing source, editing/deleting a source or closing series details cancels the obsolete coroutine.
- Added a `Mutex` around high-level provider catalog work so only one catalog/category/episode request is active at a time.
- Kept Stalker page fetching internally bounded and parallel, but prevents a second high-level catalog load from running alongside it.
- Added a dedicated OkHttp client for provider traffic.
- `Http.cancelProviderRequests()` cancels catalog/provider calls without cancelling subtitles, TMDB metadata or unrelated app traffic.
- Stalker clients now expose `cancelPendingRequests()` and stop category/page loops after cancellation.
- Xtream retries stop immediately when OkHttp reports cancellation instead of trying the remaining User-Agent/HTTP variants.
- Existing generation checks remain in place so a late result can never overwrite the currently selected source or section.
- Refresh still invalidates session snapshots and performs a fresh provider download.
- No persistent Live/VOD/Series catalog cache was reintroduced.

## Version

- `versionName`: `1.40.13`
- `versionCode`: `57`
