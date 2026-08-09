# Changelog

All notable user-visible changes are recorded here. The detailed historical
implementation notes are preserved in `docs/archive/changelog`.

## Unreleased

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
