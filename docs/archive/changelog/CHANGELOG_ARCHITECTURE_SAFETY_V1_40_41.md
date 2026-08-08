# v1.40.41 — Architecture Safety, Routing & TV Focus Hardening

## Scope

This release is the first controlled decomposition of the two largest production files. It deliberately avoids a full rewrite. Public behavior and entry points remain stable while state ownership, player launch routing, EPG work, cache work and remote-key decisions gain explicit boundaries.

## PlayerActivity

- Added `PlayerLaunchRequest`, the single typed contract for all routes that open the player.
- Migrated catalog, catch-up, reminder, direct-stream and TV Home player launches to the typed contract.
- Added `PlayerRemoteInputPolicy`, an Android-free and testable TV/media-key routing table.
- Added `AndroidPlayerKeyAdapter`, leaving Android key-code translation outside the policy.
- Moved the player EPG menu, XMLTV URL dialog and schedule rendering to `PlayerEpgPanelController`.
- Added bounded TV focus restoration after resume, dialog dismissal or transient focus loss.
- Kept Android focus navigation active when controls are visible; DPAD arrows are not swallowed.
- PlayerActivity reduced from 3,383 to 3,236 lines.

## MainViewModel

- Moved `UiState` to `MainUiState.kt`.
- Moved EPG request lifetime, source guards, persistence and public-source lookup to `MainEpgCoordinator`.
- Moved bounded session catalogs, M3U LRU payloads and live/series working sets to `CatalogSessionStore`.
- Fixed coroutine cancellation paths that could otherwise publish stale provider, EPG, series or metadata results.
- Added observable profile state instead of a remembered one-time snapshot.
- MainViewModel reduced from 2,182 to 1,821 lines.

## Routing and lifecycle fixes

- Centralized the profile-gate intent key in `AppRouteContract`.
- Profile switching and settings restore no longer terminate the process with `Runtime.exit(0)`.
- Clean task relaunch is used instead.
- Main tab and browse state use `rememberSaveable` to survive activity recreation.
- Player launch metadata fetches preserve `CancellationException`.
- Public EPG directory search cannot publish a late result after closing the dialog or changing source.

## Android TV focus and shortcuts

- Added reusable retrying focus requests across lazy-layout frames.
- Applied retrying focus to Home, EPG, refresh/load dialogs, search fields and text-entry dialogs.
- Back-to-top waits for scrolling before restoring focus.
- Player overlay focus retries over several frames rather than making one fragile request.
- Added focused tests for hidden/visible controls, VOD seek-bar ownership, live zapping, channel panel behavior, media keys and missing-focus recovery.

## Compatibility

- No non-private `PlayerActivity` or `MainViewModel` function was removed.
- No database or manifest schema change.
- No provider, playback format, Multiview, refresh or EPG user workflow was intentionally removed.
