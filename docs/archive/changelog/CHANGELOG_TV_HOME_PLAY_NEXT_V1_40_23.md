# v1.40.23 — Android TV Home / Play Next

## User-visible changes

- Added an opt-in **Android TV Home** switch under TV Settings → Playback.
- Eligible unfinished movies and episodes can now appear in the system **Play Next** row.
- Selecting a launcher card resumes the same item through the app's internal player.
- The feature is disabled by default and is never enabled on touch/mobile devices.

## Eligibility and ranking

- Publishes only traditional movies and real episode items; live channels and series
  containers are excluded.
- Movies become eligible after the earlier of 3% watched or 2 minutes.
- Episodes become eligible after 2 minutes.
- Items at or beyond 95%, or with 3 minutes or less remaining, are removed.
- Locked parental-control groups are excluded.
- Results are newest-first, deduplicated, limited to five, and contain at most one
  episode from each series.

## Privacy and playback safety

- Launcher rows never contain provider URLs, Stalker commands, usernames,
  passwords, MAC addresses or playlist credentials.
- Each launcher intent contains only a random opaque token.
- The complete playback payload is kept in Android Keystore-backed encrypted
  storage and resolved inside the app.
- Deep-link playback revalidates the active profile, source existence and current
  parental locks before resolving a playable URL.
- Missing or stale sources fail closed with a user-facing message.

## Synchronization and lifecycle

- Added a unique expedited WorkManager job for reliable reconciliation with the
  TV Provider.
- Sync is requested after player exit, app startup, source/history/progress
  deletion, profile changes and parental-lock changes.
- Existing rows are updated by stable internal identity; stale rows are removed.
- A launcher removal is honored through
  `ACTION_WATCH_NEXT_PROGRAM_BROWSABLE_DISABLED` and remains suppressed until
  the user actually plays that item again.
- The launcher-owned `browsable` column is treated as read-only.

## Architecture

- `TvHomeEligibilityPolicy`: Android-free selection rules.
- `TvHomeCatalogRepository`: profile/source-scoped resume aggregation.
- `TvHomeEntryStore`: encrypted token-to-playback mapping and removal suppression.
- `TvContinueWatchingPublisher`: publisher abstraction for a future Engage SDK
  implementation.
- `LegacyWatchNextPublisher`: current Android TV Watch Next adapter.
- `TvHomePlaybackActivity`: validated opaque-token deep-link bridge.
- `TvHomeSyncWorker`: reliable asynchronous reconciliation.

## Deliberate scope boundary

This release publishes Continue Watching only to the system Play Next row. It
intentionally does not duplicate resume items in an app preview channel. A future
branded “My List” preview channel should first migrate favorite snapshots to a
source-scoped identity so every launcher card can resolve the correct provider.

## Version

- `versionName`: `1.40.23`
- `versionCode`: `67`
