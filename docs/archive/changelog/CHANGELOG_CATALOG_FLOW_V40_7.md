# v1.40.7 — Catalog Entity & Playback Flow Fix

## Scope

End-to-end normalization for Series, Movies and Live without changing provider credentials, playback engines or source-scoped history ownership.

## Series

- M3U/Stalker rows such as `Chernobyl S01 E01` and `Chernobyl S01 E02` are collapsed into one `Chernobyl` series card.
- Episode rows are grouped by season and sorted by episode number.
- Search exposes one series result instead of one result per episode.
- Legacy episode rows previously stored as `vod` are recognized and collapsed during search.
- Series Details can load episodes for Xtream, M3U and Stalker.
- `Start series` resolves the first playable episode and uses the complete ordered episode queue.
- When a synthetic M3U/Stalker series is reopened later, its Series catalog is downloaded again instead of restoring stale stream URLs.
- The primary series button stays disabled and shows a loading label until a playable episode is available.

## Movies

- Duplicate movie rows are collapsed by provider stream identity, URL or command.
- Movie cards still open Details; playback remains available only through the Details/Hero action.
- Fresh provider URLs remain the source of truth after refresh.

## Live

- Duplicate live rows are collapsed by stream ID, channel ID, URL, command or exact EPG/name identity.
- Live cards and live search results still start playback directly.
- Live is not added to VOD/Series history.

## History compatibility

- Source/profile isolation remains unchanged.
- Resume entries survive the corrected `vod/series -> series_ep` classification through provider transport identity matching.
- No Live/VOD/Series catalog is persisted by this change.

## Main files

- `data/CatalogNormalizer.kt` — new pure normalization contract.
- `ui/MainViewModel.kt` — normalization, search and multi-provider series drill-down.
- `source/StalkerClient.kt` — correct Series kind/identity fields.
- `data/PlaylistStore.kt` — history migration across corrected content kinds.
- Mobile/TV detail hero — safe loading/disabled primary series CTA.
