# Safe Architecture Refactor Plan

The codebase now has explicit seams, but the two legacy files are still large. Further splitting should remain incremental and build-backed; a one-shot rewrite would create unacceptable regression risk.

## Completed foundation - v1.40.41

- Typed player launch boundary.
- Testable player remote-input policy.
- Player EPG panel controller.
- Main EPG coordinator.
- Bounded catalog session store.
- Separate app UI state model.
- Reactive profiles and centralized app-route constants.
- Repeatable architecture/focus/routing audit script.


## Completed controlled session extraction - v1.40.42

- Added `PlayerSessionController` as the single owner of current playback identity, source identity, queue index and asynchronous zap generation.
- Separated committed playback from pending channel presentation so rapid-zap failures return to the last stream that actually started.
- Resume-position persistence now follows committed playback while a provider URL is still resolving.
- Failed channel resolution no longer restarts the old player or adds a failed target to recents.
- Added frozen v1.40.41 public-API contracts and a repeatable compatibility audit.

## Next safe PlayerActivity seams

1. **PlayerSessionController**
   - Own current channel, queue index, source identity, resume key and stale-request generation.
   - No View ownership.

2. **PlaybackEngine abstraction**
   - Small interface for prepare/play/pause/seek/position/tracks/release.
   - Separate ExoPlayer and VLC adapters.
   - Introduce only after instrumentation coverage for lifecycle and fallback.

3. **SubtitleController**
   - External subtitle download/parser, offset, size and preview state.
   - Keep UI callback-based to avoid Activity references.

4. **PlayerChromeController**
   - Overlay visibility, auto-hide, focus restoration and status messages.
   - Remote policy remains pure and shared.

## Next safe MainViewModel seams

1. **CatalogLoadCoordinator**
   - Provider loading, progressive partial publication and transactional group refresh.

2. **SourceSwitchCoordinator**
   - Generation cancellation, old-source invalidation and automatic initial load.

3. **SeriesCoordinator**
   - Seasons/episodes expansion and cache updates.

4. **ProfileSettingsCoordinator**
   - Profile and parental-control state, with observable flows.

## Completed deep validation gate - v1.40.43

- Added frozen compatibility, architecture, deep-validation and production-risk gates to CI.
- Added Compose instrumentation foundations for TV dialog/category focus and deterministic DPAD movement.
- Added exhaustive source-deletion/index tests and large-catalog stress fixtures.
- Hardened source editing/deletion so inactive entries do not cancel or blank the active catalog.
- Added strict TV Home route parsing and TV Provider row-ownership validation.
- Centralized provider cancellation preservation across TMDB, subtitles, M3U probing, Xtream and Stalker fallback boundaries.
- Removed remaining production non-null assertions and null-stream SAF crash paths.
- Added player Handler teardown for both named and anonymous delayed callbacks.
- Added repeatable physical-device runbook and TV focus/shortcut contract matrix.

## Next controlled extraction order after the device/build gate

1. **PlayerChromeController** - overlay visibility, timers, focus restoration and transient status; keep engine ownership in Activity.
2. **CatalogLoadCoordinator** - progressive provider loading and transactional group refresh; keep public ViewModel methods stable.
3. **SourceSwitchCoordinator** - source generation, cancellation and visible-state invalidation.
4. **SeriesCoordinator** - season/episode retrieval and cache ownership.
5. **PlaybackEngine abstraction** - only after connected lifecycle/fallback instrumentation passes.
6. Multiview refinement, then DVR foundation, then timeshift, then localization/accessibility.

## Required guardrails

- Keep public ViewModel methods stable until all callers migrate.
- One responsibility per release; no mixed UI redesign during architecture work.
- Every extraction requires focused policy tests plus a real Gradle compile when available.
- Add instrumentation tests before moving ExoPlayer/VLC lifecycle ownership.
- Preserve source-scoped state and coroutine cancellation semantics.
- Keep Android TV focus boundaries and shortcut contracts in the automated audit.

## Completed controlled chrome extraction - v1.40.44

- Added `PlayerChromeController` as the single owner of overlay visibility, auto-hide scheduling, transient player status and bounded TV focus recovery.
- Removed chrome-specific `Runnable` and retry counters from `PlayerActivity`.
- Kept Android `View` ownership inside the Activity through a narrow host adapter; the controller itself is Android-free and deterministic.
- Preserved the legacy subtitle-lift matrix for live/VOD and fullscreen/windowed playback.
- Added focused tests for timer replacement, bounded focus retries, existing-focus preservation, status replacement, teardown and subtitle positioning.
- Playback engines, subtitles, EPG and routing were intentionally left unchanged.

## Next controlled extraction

1. **CatalogLoadCoordinator** - progressive provider loading and transactional refresh.
2. **SourceSwitchCoordinator** - source generation, cancellation and visible-state invalidation.
3. **SeriesCoordinator** - season/episode retrieval and cache ownership.
4. **PlaybackEngine abstraction** - only after connected lifecycle/fallback instrumentation passes.

## Completed controlled catalog extraction - v1.40.45

- Added `CatalogLoadCoordinator` as the single serialized boundary for category discovery and section loading across M3U, Xtream and Stalker.
- Moved progressive and final catalog normalization out of `MainViewModel`.
- Kept all public `MainViewModel` methods stable; state persistence and source-generation checks remain at the ViewModel boundary.
- Fixed a transactional regression: a failed refresh after partial publication now restores the exact catalog, group and favorites that were visible before refresh.
- Series detail loads share the same provider mutex, preserving the one-provider-request-at-a-time contract.
- Added focused tests for section-scoped categories, progressive normalization, request serialization and refresh rollback.

## Next controlled extraction

1. **SourceSwitchCoordinator** - source generation, cancellation and visible-state invalidation.
2. **SeriesCoordinator** - season/episode retrieval and cache ownership.
3. **PlaybackEngine abstraction** - only after connected lifecycle/fallback instrumentation passes.

## Completed controlled source-switch extraction - v1.40.46

- Added `SourceGenerationGate` as the single owner of catalog and series freshness tokens.
- Added `SourceSwitchCoordinator` as the ordered transaction boundary for playlist selection.
- Reduced `MainViewModel.selectPlaylist()` to a stable public delegation method.
- Added `SourceSwitchStatePolicy` to clear stale loading, status, search, group, category-dialog, EPG and series-detail state before the target source is shown.
- Fixed a rapid-switch regression where a target without a remembered choice could inherit `loading=true` and other source-bound UI from the previous playlist.
- Added focused generation, ordering and state-preservation tests plus a semantic callback-wiring harness.

## Next controlled extraction

1. **PlaybackEngine abstraction** - only after connected lifecycle/fallback instrumentation passes.
2. Multiview refinement, DVR foundation, timeshift, localization and accessibility.

## Completed controlled series extraction - v1.42.0

- Added `SeriesLoadCoordinator` as the provider boundary for cached, Xtream and
  rebuilt M3U/Stalker season expansion.
- Kept `MainViewModel.openSeries()` and `closeSeries()` stable while moving
  provider selection, serialization and temporary Stalker ownership out of the
  ViewModel.
- Added a pure `SeriesLoadPolicy` for stable-id/title matching and the
  non-persistent playable-row fallback.
- Ensured stale results cannot publish or retain a Stalker connection after a
  source/series generation change.
- Added focused policy tests and architecture contracts for delegation and
  cancellation ownership.

## Completed mobile live-transition correction - Unreleased

- Added `MobileLiveChannelTransitionCoordinator` as the ordered boundary for
  outgoing-frame capture, provider URL resolution, engine opening and confirmed
  first-frame handoff.
- Kept `MobilePlaybackOverlay` responsible for UI state while the focused
  transition renderer owns the directional reveal and snapshot disposal.
- Preserved one playback engine and one video surface; the engine now lives for
  the complete mobile overlay instead of being released on every channel key.
- Unified ExoPlayer and LibVLC first-frame counter semantics so stale, failed and
  cancelled channel requests cannot visually commit a transition.
