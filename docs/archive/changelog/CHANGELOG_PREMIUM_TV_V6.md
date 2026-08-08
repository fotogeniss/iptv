# Premium TV v6 — Search & Library

## New destinations

- Full-screen unified Search for Android TV.
- Dedicated **My List**, **Continue Watching**, and **History** screens.
- The TV navigation rail now exposes all four destinations directly.
- Mobile users can open My List, Continue Watching, and History from the overflow menu.

## Search behavior

Search runs across content already known to the session:

- the current section,
- other sections loaded during the session,
- persisted favorite snapshots,
- playback history.

Matching includes title, group, genre, year, cast, director, and plot. Results are de-duplicated by the same playback identity used by favorites and resume.

## Persistent My List

Previously favorites stored only an opaque key. After switching section or restarting, the app could know that an item was favorited but could not reconstruct its title, artwork, or stream metadata.

v6 persists a lightweight channel snapshot alongside the existing favorite key. The key remains the source of truth, while the snapshot powers the My List screen. Favorites changed inside the player update the same snapshot store.

## Library actions

- **OK** opens an item.
- **Long OK** removes it from My List, Continue Watching, or History.
- In Search, **Long OK** toggles My List.
- Resume progress is shown on applicable cards.
- Live channels retain a clear LIVE badge.

## Architecture and tests

- Added `LibraryPolicy` for deterministic de-duplication, searching, and favorite filtering.
- Added JVM tests for identity ordering, multi-term metadata search, and favorite filtering.
- History capacity increased from 20 to 60 entries.
- Navigation spacing was compacted to remain usable on 720p Android TV devices.
