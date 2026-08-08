# v1.40.2 — Fresh Catalogs, Source-Scoped History & Working Refresh

## Catalog freshness

- Live, VOD and Series catalogs are no longer read from or written to persistent catalog caches.
- Obsolete `chcache` and `seriescache` directories from older releases are purged on startup.
- Opening Live, Movies or Series downloads the selected section again from the active source.
- Opening an Xtream series downloads its seasons and episodes again; episode lists are not persisted.
- HTTP catalog/API requests send `Cache-Control: no-cache, no-store, max-age=0` and `Pragma: no-cache`.
- Category choices remain saved because they are user preferences, not catalog/stream data.

## Refresh repair

The Browse menu refresh now performs a real source reload:

- invalidates the Xtream authentication/session marker,
- discards the active Stalker client/token and performs a new handshake,
- discards the in-memory M3U parse result,
- clears the visible catalog and downloads the active section again,
- reuses only the user's saved category selection,
- rejects stale in-flight results when the source or section changes.

## History and resume isolation

- VOD and series/episode recents are stored per source and per profile.
- Resume positions are stored per source and per profile.
- Live channels are never added to history and never receive resume positions.
- Source namespaces use a SHA-256-derived identifier; provider passwords, tokens and MAC addresses are not exposed in preference keys.
- Fresh provider items reconcile saved history snapshots so rotated stream URLs do not destroy resume progress.
- Deleting one source deletes only that source's history/resume data.
- A compatibility migration imports an old global history item only when it can be matched to an item downloaded from the current source.

## Other

- Xtream episodes now retain both their episode stream ID and parent series ID for stable history reconciliation.
- Removed the unused Gson dependency that existed only for the deleted catalog caches.
- Version: `1.40.2` (`versionCode 46`).
