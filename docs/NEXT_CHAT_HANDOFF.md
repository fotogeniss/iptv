# Prelude+ next-chat handoff

Last verified workspace date: **2026-08-09**
Workspace: `C:\Users\konst\AndroidStudioProjects\chatgptiptv`  
Branch: `main`  
Current documented version: **1.46.0** (`versionCode 115`)  
Baseline app commit when this handoff was created:
`1a7b4f4 feat: add premium live channel transition`

This document is the operational source of truth for continuing the current
Codex collaboration in a fresh chat. Read it together with `README.md`,
`CHANGELOG.md`, `docs/MAINTENANCE.md` and
`docs/ARCHITECTURE_REFACTOR_PLAN.md` before changing code.

## 1. Non-negotiable working agreement

The owner expects the work to be performed with the care, judgment and discipline
of a **30-year senior software engineer**. In practice this means:

1. Work on **one cohesive issue or responsibility at a time**. Do not mix a UI
   redesign, architecture extraction and unrelated bug fixes in one change.
2. For **every visual or layout change**, create a functional HTML preview first
   under `prototypes/`, show it to the owner and wait for explicit approval before
   changing the Android UI.
3. If the requested Android appearance already has an approved HTML preview, reuse
   that design; do not invent a different UI.
4. Make **small patches to the existing implementation**. Never replace or rewrite
   an entire source file just to alter one behavior.
5. Do not create giant files. Put a new responsibility in its own focused file
   when that improves ownership; keep public APIs stable while callers migrate.
6. Preserve existing user changes and unrelated dirty-worktree files. Inspect the
   worktree before editing.
7. Record every user-visible change or bug fix under `Unreleased` in
   `CHANGELOG.md`. Update README or architecture/privacy documents whenever the
   rules in `docs/MAINTENANCE.md` require it.
8. Add focused tests or static contracts for behavior that can regress. TV work
   must explicitly protect DPAD focus and Back-button behavior.
9. **Do not run Gradle, compile, package or build unless the owner explicitly asks
   in that turn.** The owner normally builds in Android Studio and reports the
   result. Static Python audits are allowed.
10. Do not claim runtime success without evidence. Distinguish static validation,
    compilation, emulator checks and physical-device QA.
11. After a cohesive verified change, commit it with an intentional message. Do
    not combine unrelated work in the same commit.
12. Account/cloud synchronization is intentionally deferred until the product has
    revenue. Do not add Supabase, a VPS or another backend without a new explicit
    decision from the owner.
13. The app is a media player and does not provide IPTV content. Preserve that
    wording in onboarding, legal and store material.

## 2. Current confirmed state

- The Git worktree was clean when this handoff was prepared.
- The owner supplied an Android Studio screenshot confirming the latest code
  completed a **successful QA build in approximately 34 seconds** after commit
  `1a7b4f4`.
- Codex did not run that build; it was performed and confirmed by the owner.
- The latest static validation cycle reported:
  - architecture audit: 56 passes, one known size warning for `MainViewModel`, no
    failures;
  - compatibility contracts: 47/47;
  - deep validation: 67 passes, one documented cleartext-HTTP compatibility
    warning, no failures;
  - production-risk inventory: zero critical findings;
  - `git diff --check`: clean.
- Cleartext HTTP remains deliberately supported because many user-provided IPTV
  servers do not support HTTPS. This is documented, not an accidental warning.

## 3. Recent committed engineering work

The current project-local history begins with the following controlled sequence:

| Commit | Result |
|---|---|
| `85de40a` | Established the clean 1.46.0 project baseline, synchronized README/CHANGELOG and archived old root artifacts. |
| `0994033` | Made the owner QA variant separately installable with isolated application data. |
| `092a054` | Replaced blocking provider retry sleep with bounded interruptible backoff. |
| `884fcb4` | Extracted export/relay preparation and lifecycle from `MainViewModel`. |
| `85d8a37` | Extracted category-editor loading, draft state and persistence into a focused coordinator with tests. |
| `aab96af` | Extracted catalog grouping, visibility, favorites, search and sorting into a pure tested presentation policy. |
| `1a7b4f4` | Added the approved premium mobile live-channel transition and slimmed the mobile player scrubber without shrinking its touch target. |
| `2a050af` | Attempted to move the mobile transition trigger from swipe-end to channel publication; later device feedback proved that publication was still too early. |
| `ef62ed4` | Corrected mobile transition ownership and timing around captured outgoing frames, actual first-rendered-frame commitment and overlay-scoped engine lifetime. |
| Current TV parity change | Added the owner-approved restrained TV transition, shared first-frame coordination, fullscreen `SurfaceView` capture and a slim DPAD-safe TV scrubber. |

### Latest mobile live-channel change

- Approved prototype:
  `prototypes/player/LIVE_CHANNEL_WATER_TRANSITION_PREVIEW.html`.
- Android visual implementation:
  `app/src/main/java/com/prelude/iptv/ui/player/MobileLiveChannelTransition.kt`.
- Integration remains in the existing playback overlay; it does **not** create a
  second player or second video surface.
- The old channel-change chevron/arrow flash was removed.
- Swiping to the next and previous live channel drives opposite transition
  directions.
- The mobile scrubber's visible line is 2 dp and thumb is 8 dp, while the invisible
  interaction target remains 26 dp.
- Motion policy tests:
  `app/src/test/java/com/prelude/iptv/ui/player/LiveChannelTransitionMotionTest.kt`.
- Device feedback proved that starting at `channel` publication was still too
  early: provider URL resolution and decoding could outlast the complete effect.
- The corrected flow captures the outgoing mobile `TextureView` frame, resolves
  and opens the requested stream, waits for the engine's actual first-rendered-
  frame counter, then reveals the new video under a directional wavy boundary.
- The capture/open/first-frame ordering now lives in the focused shared
  `LiveChannelTransitionCoordinator`; each overlay keeps only its UI state.
- Failed, timed-out and cancelled requests release their snapshot and do not
  visually commit a transition. Rapid channel changes cancel stale preparation.
- The mobile `PlaybackEngine` is now released only when the complete overlay
  leaves composition, not whenever its `channel` key changes.
- ExoPlayer and LibVLC now both advance the shared rendered-frame signal for each
  new surface frame. This work has static validation but still needs the owner's
  normal Android Studio build and device confirmation.

### Latest manual subtitle search change

- Opening `Χειροκίνητη` now starts the prefilled OpenSubtitles search without a
  magnifying-glass tap.
- Editing the title automatically restarts the search after a 400 ms debounce;
  Compose cancels the previous producer and stale results cannot publish.
- The provider loop checks coroutine cancellation before starting each language,
  so an obsolete query does not unnecessarily continue into the next request.
- Result focus is no longer moved away from the text field while the owner is
  typing. TV/result focus behavior is preserved when the field is not active.
- The magnifying glass remains available as an optional explicit refresh. This
  behavior has static validation and still needs normal device confirmation.

## 4. Delivered product capabilities

The application is currently an advanced beta/release candidate, not an empty
prototype. Important delivered behavior includes:

### Sources and catalog

- M3U URL and local-file playlists.
- Xtream Codes sources.
- Stalker/MAC portals, including handshake and stream resolution.
- Add, edit, delete, refresh and switch between multiple saved sources.
- Large-catalog normalization, progressive loading, source-scoped caches and
  cancellation-aware source switching.
- Search, favorites, watch history and continue watching.
- Functional mobile Home/category visibility and ordering.
- Controlled provider concurrency for large Stalker/MAC catalogs.

### Live TV and EPG

- Category-first mobile and Android TV browsing.
- Mobile channel list/grid switch with list as the default.
- Search within live categories.
- Now/next EPG under channels and full EPG views.
- XMLTV/XMLTV.GZ, Xtream and Stalker/MAC EPG paths.
- Live playback context and channel stepping.
- Separate mobile and Android TV directional transitions during live channel
  changes, committed only after the requested stream renders a frame.
- Android TV Home recommendations, Play Next and My List integration.

### Movies and series

- TMDB posters, backdrops, ratings, genres, cast and localized summaries.
- Episode stills, titles and per-episode descriptions, cached per season.
- Minimal horizontally scrollable season selectors.
- Episode progress, resume playback and automatic next-episode offer.
- Related/recommended rails and quality badges when metadata is trustworthy.
- Descriptions with More/Less behavior and compact nested episode scrolling.
- Recommendations are a stable random cross-category mix and no longer duplicate
  the selected provider group.

### Player

- Media3/ExoPlayer primary engine with libVLC compatibility fallback.
- Embedded video/audio/subtitle track selection.
- OpenSubtitles automatic and editable manual search, result names and match
  percentage.
- Downloaded subtitles can be attached without recreating, pausing or stopping
  the active playback session.
- Global subtitle size, bold and background preferences.
- Aspect-ratio modes and detected-quality indicator.
- Mobile sticky player, Picture-in-Picture, mini player and multiview foundations.
- Complete-page collapse motion into the docked mini player rather than moving
  only the video rectangle.
- Resume position, 10-second seek, live channel step and next-episode prompt.
- Brightness/volume gesture behavior and frame-rate matching foundations.
- TV remote-input policy, seek controller and focus contracts.

### Settings, Premium, diagnostics and legal

- Functional mobile settings for sources, EPG, player preferences, Home and
  category configuration.
- Google Play Billing 9.1 integration for one non-consumable Prelude+ Premium
  product, pending purchase handling, acknowledgement and restore flow.
- FREE is the default unless a verified owned purchase is reported; the previous
  writable local Premium bypass is removed.
- Separately installable owner QA variant with full Premium access for testing.
- Privacy-first crash diagnostics: collection is opt-in and Analytics is not
  enabled.
- Firebase Crashlytics code boundary exists but production connection is deferred
  until the owner supplies `app/google-services.json` and completes the external
  setup.
- Privacy policy, Terms draft and Play Data Safety worksheet live under `docs/`.
- Account/cloud sync remains deliberately deferred.

## 5. Completed current task: Android TV live-transition parity

The owner approved the functional preview at
`prototypes/player/LIVE_CHANNEL_WATER_TRANSITION_TV_PREVIEW.html`, and that design
has now been implemented for Android TV.

### Implementation state

- `TvLiveChannelTransition` owns the restrained large-screen Canvas treatment;
  it has no focus, pointer or key-input modifiers.
- `LiveChannelTransitionCoordinator` is now the shared capture -> URL resolution
  -> engine open -> first-rendered-frame boundary used by both mobile and TV.
- TV CH+/CH- intent supplies opposite directions. Boundary presses expire after
  1.2 seconds so a later list selection cannot inherit stale direction state.
- Rapid channel publication cancels the older `LaunchedEffect`; stale snapshots
  are recycled and cannot publish a late transition.
- Failed URL resolution and playback errors never create a transition request.
- The fullscreen `SurfaceView` remains the only TV video surface. A short-lived
  `PixelCopy` bitmap supplies the outgoing frame without changing TV frame pacing
  or creating another player/surface.
- The TV effect is inserted through `PlayerHost.videoOverlay`, above video but
  below subtitles, errors and focusable chrome.
- The TV playback engine now belongs to the complete overlay and is no longer
  released during each channel-key change.
- The VOD scrubber draws a 2 dp line (3 dp focused) and a 6/8 dp thumb inside the
  existing focus graph; its DPAD target is explicitly 48 dp high.
- Pure motion tests and static contracts cover direction, restrained TV tuning,
  first-frame ownership, one-surface capture, focus isolation, seek ownership and
  scrubber target size.

### Verification status and next action

- Static verification after the Android integration reported:
  - architecture audit: 60 passes, one known `MainViewModel` size warning, no
    failures;
  - compatibility contracts: 51/51;
  - deep validation: 67 passes, one documented cleartext-HTTP compatibility
    warning, no failures;
  - production-risk inventory: zero critical findings;
  - documentation contract and `git diff --check`: clean.
- Codex did not run Gradle, compile or package because the owner did not authorize
  a build in this turn.
- The next action is the owner's normal Android Studio build followed by physical
  Android TV checks for CH+/CH-, rapid stepping, a failed provider URL, VLC
  fallback, Back, DPAD focus and VOD seeking.

## 6. Current approved navigation implementations

### Source onboarding

- The owner approved the functional mobile/TV direction in
  `prototypes/onboarding/SOURCE_ONBOARDING_FLOW_PREVIEW.html`.
- Mobile and TV now use the shared Android-free `PlaylistSourceDraftPolicy` for
  credential detection, normalization, field-level validation and `Playlist`
  construction.
- `submitPlaylistSource` is the single validation -> real provider test -> build
  boundary; saving cannot bypass a failed test.
- Credential drafts and verified results remain transient in memory; only the
  confirmed `Playlist` reaches the Android-Keystore-backed playlist store.
- The mobile screen provides smart pasted-credential detection. Both surfaces use
  plain-language method choices, premium Material iconography, explicit progress,
  a confirmed-success step and direct Live TV continuation.
- The dead account-login action and misleading "Ίσως αργότερα" wording were
  removed from active add-source screens.
- TV focus is explicit across method selection, fields, exit and submit; Back
  cancels an in-flight submission and dialogs/file selection restore focus.
- The QR phone-pairing concept remains prototype-only. It requires a separate
  transport/security decision and no fake production button was introduced.
- Static contracts cover the new ownership and focus boundaries. A normal Android
  Studio build plus phone/TV device QA is still required; Codex did not run Gradle.

### Unified content navigation

- The owner approved
  `prototypes/home/CONTENT_NAVIGATION_FLOW_PREVIEW.html`, and its navigation
  hierarchy is now implemented in the existing Android screens rather than in a
  parallel replacement flow.
- Mobile and TV expose the same five primary destinations in the same order:
  Home, Live, movies, series and Search. Mobile no longer requires opening a
  second Browse panel; the TV rail no longer mixes primary content with library,
  EPG and source-management actions.
- Favorites, Continue and History remain Home-owned library views; EPG remains
  available inside Live; Sources remain the first Settings page. Settings stays
  directly available from mobile screen headers and the bottom of the TV rail.
- `PrimaryContentDestination` is the Android-free ordering and selection
  contract shared by both surfaces. Its policy tests protect route order,
  secondary-view ownership and the selected TV item that receives Back focus.
- Mobile Live cards already opened playback directly. Android TV Live now opens
  the selected channel with one OK while preserving long-OK multiview and exact
  focus restoration after leaving fullscreen playback.
- Automatic Search, movie/series details, seasons/episodes and player return keep
  their existing centralized `BrowseRoute` ownership; this change did not create
  duplicate navigation or playback state.
- Static verification reports architecture 60 pass / 1 known size warning,
  compatibility 58/58, deep validation 67 pass / 1 documented cleartext warning,
  zero critical production-risk findings and a clean diff check. Gradle was not
  run; the owner still needs to compile and perform phone/TV device QA.

### Localization implementation in progress

- The owner approved the functional mobile/TV language flow at
  `prototypes/localization/LOCALIZATION_SETTINGS_FLOW_PREVIEW.html` with System,
  Greek and English choices, immediate effective-language changes, persistence,
  English fallback and TV DPAD movement.
- `docs/LOCALIZATION_ARCHITECTURE.md` defines the approved-API implementation,
  feature-owned resource files, English fallback, Greek parity requirement,
  Kotlin boundaries and staged migration gates.
- The runtime foundation now uses AppCompat per-app locales across all five
  activity hosts. Paired Greek/QA-English resources cover primary navigation,
  the language/appearance settings surface and the complete mobile/TV Home
  experience: hero actions, rails, category exploration, Home editing,
  empty/recovery states, accessibility labels, plurals and locale-aware counts.
- Home display copy is owned at the Android UI boundary. `HomeSection` keeps
  only stable IDs and behavior flags, `CatalogPolicy` receives localized labels,
  and provider category/title data is deliberately left untouched. The static
  localization contract rejects Greek literals in the migrated Home UI files.
- Live TV is now localized across the active phone and TV routes plus the
  retained premium Live components: categories, search, list/grid views,
  preview/player panels, inline programme labels, Multiview guidance/failures,
  PIN/category actions, empty states, accessibility copy, locale-aware progress
  and plurals. Provider channel/group names and EPG content remain data.
- `LiveFilterOption` no longer owns app labels, remaining time is a typed value,
  Multiview launch failures are typed, and the legacy catalog status string is
  classified once through `CatalogStatusPolicy`. Migrated Home/Live surfaces do
  not render that legacy Greek transport text. Full producer-side typed catalog
  status should replace the transitional classifier in a later focused change.
- Movie and series browsing is now localized across the active phone and TV
  routes: app-owned section/synthetic-category labels, search, sorting, counts,
  loading and empty states, refresh/load dialogs, voice-search language and
  accessibility copy. Provider titles, category names and metadata remain raw
  provider data. `CatalogLocalizationResources.kt` owns the synthetic-group and
  locale-aware progress boundaries; the shared locale-aware uppercase helper is
  intentionally no longer Live-owned.
- The next cohesive localization slice is Search, followed by movie/series
  details and seasons/episodes. The full EPG UI remains with the later
  EPG/settings slice; only programme labels shown directly inside Live were
  included there.
- Partial English resources live in the shared `app/src/localizationQa` source
  set used only by debug/QA. Production keeps the Greek unqualified baseline so
  English-system devices cannot receive a mixed-language public UI mid-migration.
- Mobile and TV language pickers are visible in owner QA builds. Public builds
  keep them hidden through `LOCALIZATION_PARITY_COMPLETE=false`; do not flip the
  flag or enable generated locale config until every release surface has matching
  English/Greek resources and the final audit passes.

## 7. Architecture work to continue afterward

Continue splitting legacy files only through small, behavior-preserving
extractions. Do not chase line count by moving arbitrary blocks.

Current largest Kotlin files at handoff time include:

- `MainViewModel.kt`: about 1,841 lines.
- `BrowseRoute.kt`: about 1,298 lines.
- `PlaybackEngine.kt`: about 974 lines.
- `PlayerHost.kt`: about 891 lines.
- `MobilePlaybackOverlay.kt`: about 864 lines.
- `PlaylistStore.kt`: about 772 lines.
- `TvLiveBrowseScreen.kt`: about 738 lines.

The next sensible non-visual ViewModel seam is playlist mutation/management:
`addPlaylist`, catalog-count persistence, playlist update and playlist deletion.
If selected, preserve the public `MainViewModel` API and exact side-effect order.
Focused tests must protect these established rules:

- editing an inactive playlist must not restart the active source;
- deleting an inactive playlist must preserve the visible active catalog;
- shared stable source identities must not delete shared persisted data early;
- deleting the active source must invalidate/cancel it before cleanup;
- local M3U files may be removed only when the last reference is gone;
- replacement-index selection and last-playlist persistence must remain stable.

Do not introduce a `PlaybackEngine` ownership rewrite until connected lifecycle
and fallback instrumentation is strong enough. That is a higher-risk future seam.

## 8. Remaining release gates

These are not all code changes and must not be marked complete without owner or
external-console work:

1. Organized device QA across representative phones, tablets and Android TV
   devices using `docs/DEVICE_QA_MATRIX.md` and
   `docs/DEVICE_VALIDATION_RUNBOOK.md`.
2. Regression tests for source onboarding, large catalogs, playback, subtitle
   switching, EPG, billing, mini player and TV focus.
3. Play Console product configuration, license-test accounts, purchase/refund and
   restore validation on Play-distributed builds.
4. Production signing, store listing, screenshots and final release artifacts.
5. Publisher legal identity, privacy contact and a public non-PDF privacy-policy
   URL.
6. Final Play Data Safety and legal review, including TMDB/OpenSubtitles commercial
   terms for monetized distribution.
7. Production Crashlytics connection only after the owner chooses to enable it and
   provides the Firebase configuration. Consent must remain opt-in.
8. Broad performance/crash validation with genuinely large playlists.
9. Server-side account sync and purchase verification are deferred business
   decisions, not current blockers for the first revenue-oriented release unless
   the chosen store/premium threat model changes.

## 9. Validation and documentation workflow

Before editing:

```powershell
git status --short
git log --oneline -10
```

After a code change, when no Gradle build was authorized, run the applicable
static gates:

```powershell
python scripts/architecture_audit.py
python scripts/compatibility_contracts.py
python scripts/deep_validation_audit.py
python scripts/risk_inventory.py
python scripts/documentation_contracts.py
git diff --check
```

Then inspect the exact diff and confirm no unrelated files changed. If the owner
later reports a successful Android Studio build, record that as owner-provided
build evidence; do not imply Codex ran it.

Documentation rules are defined in `docs/MAINTENANCE.md`. In short:

- behavior/bug fix -> `CHANGELOG.md`;
- capability added/removed -> README feature section plus changelog;
- ownership/architecture boundary -> architecture plan if affected;
- SDK/dependency/device support -> README toolchain;
- privacy, external service, account, analytics, permissions or data fields ->
  privacy and Play Data Safety audit;
- billing behavior -> Terms, privacy and Play Billing declarations audit;
- version bump -> README version and matching CHANGELOG release section.

Do not create a separate root-level validation report for every change. Keep
current documents under `docs/`, prototypes under `prototypes/` and transient
reports under `validation/`.

## 10. Prompt to start the next chat

The owner can paste the following after attaching or referencing this file:

> Read `docs/NEXT_CHAT_HANDOFF.md`, `README.md`, `CHANGELOG.md`,
> `docs/MAINTENANCE.md` and `docs/ARCHITECTURE_REFACTOR_PLAN.md` before acting.
> Continue as a 30-year senior engineer: one careful responsibility at a time,
> small patches only, never rewrite a whole file, avoid giant files, preserve
> public behavior and add focused tests. Every visual change requires a functional
> HTML preview and my approval before Android implementation. Do not run Gradle or
> build unless I explicitly ask. Record changes in CHANGELOG/docs and commit each
> cohesive completed change. The immediate implementation task is the movies
> and series browse localization slice across phone and TV, keeping provider
> titles, categories and metadata untouched while moving app-owned copy to the
> Android resource boundary.
