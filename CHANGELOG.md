# Changelog

All notable user-visible changes are recorded here. The detailed historical
implementation notes are preserved in `docs/archive/changelog`.

## Unreleased

- Fixed Greek series whose titles are written in Latin characters ("greeklish")
  showing the show's general synopsis on every episode instead of that
  episode's own. The per-episode display path was already correct everywhere
  (`MobileEpisodeCard`, `MobilePlayerContextContent`, `TvEpisodeInfoPanel` and
  the TV episode rail all read `tmdbEpisode.overview` and fall back to
  `Channel.plot`), and Greek-titled series already worked. The failure was
  purely in lookup: TMDB knows only the Greek title, the list supplies
  `To Kafe tis Xaras`, nothing matched, `episodeMeta` came back empty, and the
  fallback correctly showed the provider's series-level plot on every episode.
  `TmdbClient.searchId` now makes one final attempt for such titles using the
  new `GreeklishTitlePolicy`: it sends a transliterated Greek query and accepts
  a result only when the result's title reduces to the same convention-neutral
  skeleton as the list's title.
  - The attempt runs **last**, after every existing candidate form has failed,
    so the two extra requests are charged only to lookups that already returned
    nothing. English and already-Greek titles are excluded outright.
  - Existing candidates keep taking the first search result exactly as before —
    they are the provider's real title. Only the transliterated query, which is
    a guess, must prove itself, because an unverified match would attach a
    completely unrelated show's episode synopses to the series. Verification
    checks `name`, `original_name`, `title` and `original_title`, since a Greek
    query can still come back with an English `name`.
  - Matching tolerates a dropped leading article (lists write `Kafe tis Xaras`
    for `Το Καφέ της Χαράς`) but requires the shorter skeleton to be at least
    six characters and 60% of the longer, so a one-word title cannot match
    everything containing it.
  - Nothing here is ever displayed or persisted: the transliteration is used
    only as a search query and a comparison key, and the title shown to the
    user remains exactly what the provider supplied.
- Fixed Stalker/Ministra series episodes being impossible to change: pressing
  the "next episode" card did nothing, and picking any episode from the list
  under the video jumped to a seemingly random position in what looked like
  the same episode. One root cause behind both symptoms.
  `PlaybackQueue.favKey` — documented in place as *the* single identity key for
  favorites, recents and resume position — falls back to `Channel.cmd` when
  `url` is empty. On this portal every episode of a season deliberately shares
  one `cmd` (the season descriptor); episodes are distinguished only by the
  `series=` value that `create_link` receives, which
  `StalkerClient.buildEpisodeChannel` stores in `chId`/`streamId`. So every
  episode of a season collapsed onto one key:
  - `NextEpisodePolicy.nextAfter` locates the current episode with
    `indexOfFirst { keyOf(it) == currentKey }`, which always matched position
    0, so "next" was always the season's *second* episode. While that second
    episode was playing, "next" resolved to the episode already on screen, the
    route state was reassigned an equal `Channel`, Compose saw no change, and
    the button appeared dead.
  - Resume position is stored under the same key, so every episode reopened at
    the timestamp left behind by whichever episode was watched last — the
    "random point" symptom. Picking an episode did switch the stream (the URL
    is built from `chId`, which was always correct), but it resumed mid-way
    into the previous episode's position, which reads as "nothing happened".
  - Favorites, recents, `LibraryPolicy.unique` de-duplication and
    `PlaybackQueue.subtitleRequest` lookups collapsed a whole season into one
    entry for the same reason.
  `favKey` now returns `"<cmd>|ep|<streamId>"` for `series_ep` items that have
  no `url`, and is byte-for-byte unchanged for everything else. Xtream episodes
  carry their own per-episode `url` and are unaffected; live, VOD and series
  keys are untouched, which the new contract test pins down.
  **Stored-key note:** Stalker episode favorites and resume positions saved
  between the episode-loading fix and this one land under the old shared key
  and are not migrated. No migration is possible or meaningful — the old key
  mapped many episodes onto one entry, so there is no correct target to move it
  to, and the merged value was wrong by construction. Stalker episodes could
  not load at all before that fix, so at most a few days of episode-level
  entries are affected; movie, live and series entries are untouched.
  Added `PlaybackQueueIdentityTest` covering per-episode key uniqueness across
  seasons, key stability across rebuilt catalog instances, the unchanged
  live/VOD/series/Xtream keys, and next-episode advancement including the
  season boundary.
- Investigated reported slow Stalker/Ministra list loading compared to other
  IPTV apps on the same portal. Code reading found two concrete asymmetries
  specific to Stalker traffic, both in `StalkerClient`/`Http.kt`:
  - `StalkerClient.headers()` explicitly sent `Accept-Encoding: identity` on
    every request, which disables OkHttp's transparent gzip entirely (OkHttp
    only auto-negotiates and auto-decompresses gzip when the caller does
    *not* set that header itself). No other client in the app (Xtream, M3U,
    TMDB, subtitles) sets this header, so those already received gzip while
    every Stalker category/page response — the bulk of catalog traffic —
    downloaded fully uncompressed. No changelog/doc history explained why
    `identity` was originally forced, so this is a considered removal based
    on the asymmetry, not a documented prior fix being reverted; flagged for
    on-device confirmation in case some portal turns out to send broken
    gzip (would surface as a JSON parse error, not silent bad data).
  - `Http`'s shared `providerClient` used OkHttp's default `Dispatcher`,
    which caps concurrent requests to the same host at 5 regardless of
    application-level thread pool size. `StalkerClient` already parallelizes
    up to 3 categories × 6 pages (~9 concurrent requests, see
    `categoryPool`/`pagePool`), so the extra requests were silently queuing
    inside OkHttp with no error — quietly capping real network concurrency
    below what the existing pool design intended. `providerClient` now uses
    its own `Dispatcher` with `maxRequestsPerHost = 16` / `maxRequests = 32`.
  - Both changes are scoped to `providerClient` only (catalog/provider
    traffic); the general-purpose `client` used for subtitles/TMDB/EPG is
    unchanged.
  - Owner-confirmed on 2026-08-10: the app builds and the Stalker catalog
    loads correctly on both mobile and Android TV with gzip restored. No
    regression tests were added since this changes wire-level HTTP behavior
    rather than parseable logic; if some other portal ever misbehaves under
    gzip it would surface as a JSON parse error (not silent bad data), and
    reverting just the `Accept-Encoding` line is enough.
- Fixed a brief unwanted "flash" right before the live-channel directional
  transition effect on mobile and TV. Root cause: the transition overlay
  (`MobileLiveChannelTransition`/`TvLiveChannelTransition`) previously only
  came into existence *after* the new channel's first frame was confirmed
  rendered — `LiveChannelTransitionCoordinator.open()` captured the outgoing
  frame internally but only handed it to the caller inside the final
  `Opened(transition = ...)` result. That left the real video surface fully
  uncovered for the entire resolve-URL + `engine.open()` + first-frame-wait
  window, during which the surface itself briefly shows a black/decoder
  artifact frame as it switches sources — visible as a stray flash before the
  deliberate wavy effect began. Fix: `open()` now takes an
  `onOutgoingFrameCaptured` callback invoked immediately once the frame is
  captured, before URL resolution even starts; both overlays use it to cover
  the surface right away with a static, non-animating "held" request
  (`LiveChannelTransitionRequest.startReveal = false` — at that phase the
  frozen frame already spans the full width per `edgeFraction`, so coverage
  is complete with no seam) and only flip `startReveal = true` to start the
  reveal animation once the coordinator confirms the new frame committed. If
  resolution/opening fails or times out, the held frame is dropped instead of
  animating. Added regression tests
  (`a held transition request defaults to not revealing yet`,
  `outgoing frame fully covers the screen at the held phase`) in
  `LiveChannelTransitionMotionTest.kt`. No visual design changed (same wave/
  colors/timing as the already-approved prototype) — only when the existing
  effect starts covering the surface — so no new HTML preview was made; this
  restores the previously approved effect's intended seamless behavior.
  Owner-confirmed on 2026-08-10 on both mobile and Android TV.
- Localized the OpenSubtitles result fallback title (shown when a result has
  no usable filename/release/title and the search query itself is blank).
  `SubtitleResultNamePolicy.displayName` now takes a caller-supplied
  `noTitleFallback` instead of owning a hardcoded Greek string; the mobile/TV
  shared `SubtitleWiring.search` call site resolves it from the new
  `player_subtitle_result_fallback_title` Greek/QA-English resource.
- Fixed Stalker/Ministra (MAC portal) series always showing "0 seasons" with
  no error, and then showing only one episode once episodes started loading.
  Diagnosed against a live portal end-to-end via raw-JSON logging (tag
  `SeriesLoad` in `StalkerClient`/`MainViewModel.openSeries`):
  - The series category listing (`get_ordered_list`) never carries episode
    data in this API — every row comes back `"cmd":""`/`"series":[]` — and
    separately left `"series_id"` blank while the real id was under `"id"`,
    so the normalizer fell back to a `local:` hash that could never be
    matched again when the details screen re-fetched. `seriesId` now prefers
    `id` over `series_id`, matching the existing `stableKey`/`streamId`
    priority.
  - Episodes need one dedicated request:
    `get_ordered_list?movie_id=<id>&category=*&season_id=0&episode_id=0`.
    Its response rows are seasons, not episodes: each row's own `cmd` is a
    base64 season descriptor (`"has_files":0`), not a playable stream, and
    its `"series":[1,2,3,...]` array lists that season's episode numbers.
    Every episode plays through the *same* season `cmd`, with the episode
    number passed as create_link's `series=` parameter — added
    `StalkerClient.seriesEpisodes(seriesId)` to build every season's full
    episode list from that one response, and `SeriesLoadCoordinator` now
    calls it directly for any Stalker series with a real provider id
    (mirroring the existing Xtream direct path) instead of reloading the
    whole category list, which never had episodes to find.
  - Episode identity had to be split in two: `Channel.streamId` now carries
    a value unique across the whole series (`"<season row id>:<episode
    number>"`) because `PlaybackHistoryStore`/favorites key off it
    permanently and the raw episode number alone repeats every season (would
    have merged S01E01 and S02E01 history/favorites); the raw episode number
    itself travels in `Channel.chId` (unused for `series_ep` elsewhere)
    purely so `resolve()`/`Repository.playableUrl`/`RelayHub.resolve` can
    still pass it as `series=`.
  - The earlier, disproven single-level `"series"`-array read on the
    category-listing row was removed from `StalkerClient.getVodLike`/`append`
    (confirmed dead: that field is always empty at that level). The full
    response is still logged (`SeriesLoad` tag) in case some other portal
    shapes this differently.
- Added per-episode descriptions for Stalker/Ministra series. The season-level
  `get_ordered_list?...&season_id=0` response used above only has a
  show/season-level description, not one per episode, so
  `StalkerClient.seriesEpisodes` now issues one additional
  `get_ordered_list?movie_id=<id>&category=*&season_id=<season row
  id>&episode_id=<episode number>` request per episode (parallelized on the
  existing `categoryPool` thread pool, same one used for category-page
  fetches, so a season with many episodes doesn't serialize) and fills
  `Channel.plot` from its `"description"` field. Owner-confirmed working on
  2026-08-10 against their live portal on mobile and Android TV; the response
  is still logged under the same `SeriesLoad` tag
  (`"seriesEpisodes episode detail"`) so a portal that shapes this field
  differently can be diagnosed without new instrumentation. On any failure or
  unexpected shape it silently falls back to an empty description (episode
  still loads/plays normally either way).
  `XtreamClient.seriesEpisodes` has the same missing-description gap and was
  intentionally left untouched — the reported bug was specific to the
  Stalker portal; ask before extending this to Xtream.
- Fixed series episodes silently failing to load and staying stuck on the
  loading spinner (across every source type) when the currently browsed
  section reloaded in the background (e.g. re-picking categories) while a
  series-details flow was still open. `SourceGenerationGate.isCurrent` was
  gating the pending series request on the general catalog load generation as
  well as its own series generation, so an unrelated list reload silently
  discarded the just-fetched episodes with no error shown. Completion is now
  gated on the series generation alone; switching or removing the source still
  cancels an in-flight series request via `invalidateAll`. Added a regression
  test (`catalogReloadAloneDoesNotRejectAnOpenSeriesRequest`).
- Localized the Library hub (Favorites/My List, Continue watching and History)
  across mobile and Android TV with paired Greek and QA-English resources:
  headers, tabs, sort/manage actions, rail titles/subtitles with locale-aware
  counts, info-panel eyebrow labels, empty states, hero copy and the
  description fallback sentence. `LibraryHubTab` and `LibrarySort` no longer
  own display labels, and `libraryRails()`/`libraryDescription()` receive or
  return typed/localized values instead of hardcoded Greek text. Provider
  titles, metadata and stored favorite/history/continue-watching data remain
  unchanged.
- Localized export/relay and system notification copy with paired Greek and
  QA-English resources: the Export/Relay screen (labels, group actions, MAC
  notice, save/copy/stop/start actions and save/copy/relay toast messages) and
  the catalog-download, relay and EPG-reminder notification channel
  names/descriptions, titles and progress/body text. Notification channel IDs,
  notification IDs, PendingIntent behavior, foreground-service lifecycle,
  export file format/MIME type, SAF save behavior, relay URLs and provider
  channel/programme data remain unchanged.
- Localized the opt-in Diagnostics and crash-reporting screen with paired Greek
  and QA-English resources, typed status feedback, locale-aware pending-report
  timestamps and localized switch accessibility copy. Consent defaults,
  redaction, local pending-report storage and Firebase initialization/deletion
  behavior remain unchanged.
- Localized the in-app Legal and Privacy presentation with paired Greek and
  QA-English resources. Tabs, disclosures, service statuses and summarized terms
  now cross typed resource boundaries while publisher placeholders, policy
  version/effective date, service identities and mandatory TMDB attribution stay
  unchanged in meaning.
- Localized Billing and Premium purchase, restore, pending, entitlement and
  feature-gate copy across mobile and Android TV. Billing producers now expose
  typed message identities while Play-formatted prices and provider debug details
  remain unchanged data; purchase handling, acknowledgement, device verification
  and persisted entitlement values are unchanged.
- Localized local profiles, parental PIN and encrypted backup flows across
  mobile and Android TV. Removed inactive account/cloud-sync promises, kept
  user profile names and persisted storage unchanged, and mapped backup
  failures through typed Greek/QA-English UI copy without exposing raw errors.
- Fixed QA Kotlin compilation in the retained provider EPG dialog by consuming
  typed EPG statuses and source options through their localized UI mappings.
- Fixed QA Kotlin compilation for localized shared routes by explicitly resolving
  the application `R` class instead of relying on ambiguous wildcard imports.
- Localized the active mobile and Android TV Settings shell, playback/AFR/buffer
  dialogs, subtitle and audio preferences, category editor, source sheets and
  overview/status surfaces. Persisted preference values remain stable while
  player modes, frame-rate modes, buffer profiles, language codes and category
  failures now cross typed/resource-owned presentation boundaries.
- Localized the full EPG experience across mobile and Android TV, including
  guide filters, programme actions, empty/accessibility states, locale-aware
  time and duration formatting, XMLTV discovery/loading and active EPG settings.
  EPG status, source labels and load failures now cross typed boundaries while
  provider programme/channel content, identifiers, URLs and hosts remain data.
- Localized source onboarding, connection validation, file import, editing and
  source management across mobile and Android TV. Pure source policies now emit
  typed validation, detection, connection, submission and status identities;
  the Android boundary maps them to paired Greek/QA-English resources while
  provider names, URLs, credentials, responses and stable method IDs remain data.
- Localized the shared mobile/Android TV player chrome, audio/subtitle panels,
  editable OpenSubtitles search, next-episode prompts, inline programme states
  and playback/subtitle failures. Playback track labels now use the active app
  locale while provider titles, filenames, track metadata and diagnostic URL
  details remain untranslated data.
- Localized movie/series details, seasons and episode browsing across mobile
  and Android TV, including hero actions, tabs, metadata labels, descriptions,
  season selectors, episode counts/cards, progress and accessibility copy.
  TMDB/provider titles, plots, cast roles and season identities remain data;
  TMDB guidance and remaining-time presentation now cross typed UI boundaries.
- Localized global Search across the active mobile and Android TV routes,
  including filters, headings, result counts, empty states, featured actions,
  accessibility copy and a TV keyboard that starts with the active app
  language while retaining explicit Greek, Latin and numeric layouts. Search
  policy models now expose typed identities instead of owning display copy;
  provider suggestions, titles and metadata remain unchanged.
- Localized movie and series browsing across the active mobile and Android TV
  routes, including app-owned section/category labels, search and sorting,
  counts, loading/empty states, refresh dialogs, voice-search language,
  accessibility copy and locale-aware uppercase/progress formatting. Provider
  titles, category names and metadata remain unchanged.
- Localized Live TV across the active mobile and Android TV flows, including
  categories, search, list/grid views, channel preview, programme labels,
  Multiview guidance and typed failures, parental-category actions, empty
  states, accessibility copy, locale-aware progress and pluralized counts.
  Provider channel/group names and EPG content remain unchanged.
- Localized the complete Home experience for mobile and Android TV in the
  staged QA flow, including rails, hero actions, category exploration, Home
  editing, empty/recovery states, accessibility labels, plurals and
  locale-aware counts. Provider-owned titles remain unchanged, while pure Home
  and catalog policies now receive or expose stable IDs instead of owning UI
  copy.
- Implemented the approved localization foundation across all five activity
  hosts, with AndroidX per-app locale persistence, paired English/Greek
  navigation and settings resources, localized mobile/TV primary navigation,
  and QA-gated mobile/TV language pickers for System, Greek and English. Public
  builds preserve the existing Greek baseline, while English routing and picker
  exposure remain blocked until full translation parity.
- Added a functional mobile/TV localization-settings preview for System, Greek
  and English, covering immediate app-wide updates, persistence, safe English
  fallback, mobile bottom-sheet behavior and TV DPAD selection.
- Implemented the approved unified content navigation on mobile and Android TV:
  Home, Live, movies, series and automatic Search are now direct primary
  destinations, secondary library/EPG/source tools remain inside their owning
  screens, settings stay one action away, TV Back focus returns to the owning
  destination, and a TV Live channel opens with one OK instead of two.
- Added a functional premium mobile/TV content-navigation prototype that unifies
  Home, Live TV, movies, series and automatic search while preserving direct
  playback, detail hierarchy, DPAD focus and predictable Back behavior.
- Fixed QA Kotlin compilation failures caused by Compose importing its internal
  `layout.weight` symbol and by duplicate TV onboarding helper type names.
- Rebuilt source onboarding on mobile and Android TV around plain-language source
  choices, premium iconography, mobile smart credential detection, field-level
  errors, one atomic provider-test-and-add action, clear progress and confirmed
  success before saving; successful additions now continue directly to Live TV,
  and the dead account-login promise and misleading "maybe later" action are gone.
- Added a functional mobile/TV source-onboarding prototype covering plain-language
  method selection, smart credential detection, inline validation, one-step
  connection testing and saving, clear progress, success counts, direct Live TV
  entry and the proposed TV-to-phone pairing path. Pairing remains a design proposal
  pending a separate security and architecture decision.
- Added a maintained next-chat engineering handoff covering the verified project
  state, working constraints, delivered capabilities, release gates and the next
  approved design task.
- Made the owner QA build separately installable as `com.prelude.iptv.qa`, with
  isolated app data and the existing visible `Prelude+ QA` label.
- Replaced the provider download retry `Thread.sleep` with a bounded,
  interruptible backoff component and added focused policy tests.
- Extracted relay lifecycle and resolved-M3U export preparation from
  `MainViewModel` into a focused, tested coordinator while preserving its API.
- Extracted category-editor loading, draft state and normalized persistence from
  `MainViewModel` into a focused coordinator with state and save-flow tests.
- Extracted catalog group ordering, parental visibility, favorites filtering,
  search and sorting into a pure policy with focused regression tests.
- Replaced the mobile live-channel swipe arrow with a subtle directional
  refraction transition, and slimmed the visible player scrubber while retaining
  its larger touch target.
- Reworked the mobile live-channel transition to wait for the new stream's first
  rendered frame, then directionally reveal it beneath a captured outgoing frame;
  failed or stale channel loads no longer trigger a false visual commit.
- Kept the mobile playback engine alive across channel changes instead of
  releasing it while the next stream and first-frame transition were starting.
- Made manual OpenSubtitles search run automatically when opened and after an
  edited title settles, with cancellation of stale keystroke searches and no
  required magnifying-glass tap.
- Added the approved restrained directional live-channel transition to Android
  TV, committed only after the requested stream renders its first frame, and
  slimmed the TV scrubber while retaining a 48 dp DPAD focus target.

## 1.46.0 - versionCode 115

- Added bounded parallel Stalker/MAC category loading for Live TV, movies and
  series while preserving provider category order.
- Kept page loading and category loading on separate executor pools to avoid
  thread starvation and nested-pool deadlocks.
- Added cancellation propagation for outstanding category work and category-based
  progress reporting.
- Preserved the detailed per-version implementation notes under
  `docs/archive/changelog`.

## Product changes delivered through 1.45.2

- Extracted series/episode provider loading from `MainViewModel` into a tested,
  cancellation-aware coordinator without changing its public UI contract.
- Added privacy-first mobile crash diagnostics: collection defaults off,
  Firebase starts only after consent, one redacted local pending report can be
  sent or deleted, Analytics is excluded, and publisher setup remains external.
- Updated the Billing 9.1 integration for nullable one-time offer tokens and
  removed the contradictory minified-plus-debuggable QA build configuration.
- Replaced the short mobile legal bottom sheet with a full Privacy, Terms and
  Third-party Services center, including local-data, HTTP transport and release
  identity disclosures.
- Added an owner-only QA variant with release-like shrinking, full Premium
  access and a separate application ID for safe side-by-side installation.
- Added Google Play Billing 9.1 with a single non-consumable Prelude+ Premium
  product, Play-provided pricing, purchase/restore UI on mobile and TV, pending
  purchase handling, acknowledgement, reactive entitlement state and tests.
- Removed the old local `FULL` premium default and the writable local tier bypass;
  without a verified owned purchase, premium features now resolve to the free tier.
- Fixed downloaded OpenSubtitles files not appearing and made switching between
  embedded and downloaded captions seamless, without reloading or pausing the
  active video.
- Changed the mobile player collapse gesture so the complete player page follows
  the swipe and settles into the docked mini player, rather than moving only the
  video rectangle.
- Fixed a mobile playback crash caused by applying an unsupported background
  drawable to the TextureView, and tied video-surface detachment to its UI
  lifecycle.
- Fixed the mobile subtitle/audio panel so its final settings scroll fully above
  the Android navigation bar and keyboard.
- Changed Home recommendations to a stable random, cross-category mix instead of
  duplicating the currently selected provider group.
- Added the unified mobile navigation menu and compact collapsed behavior.
- Reworked mobile Live TV around provider categories, search, EPG and list/grid
  switching.
- Added sticky player context for live channels, movies, series and episodes.
- Added the combined subtitle/audio panel, embedded-track listing, editable
  OpenSubtitles search, match percentage and global subtitle styling.
- Added TMDB episode stills, titles and summaries with one cached request per
  season.
- Added quality hints to details, episode cards and recommendations when the
  provider exposes reliable metadata.
- Added functional mobile settings for sources, EPG, player preferences, Home and
  category layouts.

## 1.42.0 - versionCode 97

- Replaced the old mobile Live TV preview with category-first browsing.
- Removed redundant Live/Movies/Series tiles from Home.
- Added functional search, category navigation and EPG routing for large channel
  catalogs.
- Improved source switching, controlled loading and session restoration.

## Earlier releases

See [`docs/archive/changelog`](docs/archive/changelog) for the original detailed
release and phase notes.
