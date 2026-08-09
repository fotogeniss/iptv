# Prelude+ next-chat handoff

Last verified workspace date: **2026-08-09**
Workspace: `C:\Users\konst\AndroidStudioProjects\chatgptiptv`  
Branch: `main`  
Current documented version: **1.46.0** (`versionCode 115`)  
Latest completed implementation commit before this documentation handoff:
`e892e2a feat: localize settings experience`

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
14. Read the full current instructions and the files named at the top of this
    handoff before acting. Do not infer that a feature works merely because an
    old changelog entry says it was implemented; inspect the active route and
    verify the actual code path.
15. Diagnose ownership before editing. Do not apply blind project-wide string
    replacements, do not move responsibilities only to reduce line count, and
    do not duplicate an existing policy, coordinator, player or navigation state.
16. Localization must preserve user/provider data and storage contracts. Never
    translate source/profile names, credentials, URLs, channel/programme/catalog
    titles, filenames, raw provider diagnostics or persisted protocol keys.
    App-owned display copy belongs in feature resources; Android-free layers
    expose stable values or typed states rather than localized sentences.
17. Copy/resource-only localization does not require an HTML preview when layout
    and interaction remain identical. Any visual hierarchy, spacing, motion,
    focus or navigation change still requires the preview-and-approval workflow.

## 2. Current confirmed state

- The Git worktree was clean immediately after commit `e892e2a` and before this
  documentation-only handoff update began.
- The owner previously supplied an Android Studio screenshot confirming a
  **successful QA build in approximately 34 seconds** after commit `1a7b4f4`, and
  later reported another localization checkpoint built without errors. Those are
  owner-provided historical build results; they do **not** prove that the current
  `e892e2a` Settings slice compiles or passes device QA.
- Codex did not run Gradle for the current localization work because the owner did
  not authorize it. Android Studio compilation and phone/TV checks after
  `e892e2a` remain outstanding.
- The owner then reported `:app:compileQaKotlin` failures in
  `BrowseStateComponents.kt`, `DetailRouteHost.kt`, `PlaylistSourcesScreen.kt`,
  `SettingsFieldComponents.kt` and `SettingsPlaybackDialogs.kt`: wildcard imports
  exposed several dependency `R` classes, so unqualified `R.string` was
  ambiguous. The focused fix adds explicit `com.prelude.iptv.R` imports to those
  five files and a static regression contract. The owner must rerun the QA build;
  this document does not claim that rebuild has passed.
- The latest static validation cycle at `e892e2a` reported:
  - localization contracts: pass, including 208 paired Settings keys with
    matching structure and placeholders;
  - architecture audit: 60 passes, one known size warning for `MainViewModel`, no
    failures;
  - compatibility contracts: 58/58;
  - deep validation: 67 passes, one documented cleartext-HTTP compatibility
    warning, no failures;
  - production-risk inventory: zero critical findings;
  - documentation contract and `git diff --check`: clean.
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
| `eea9fbd` | Added the owner-approved restrained TV transition, shared first-frame coordination, fullscreen `SurfaceView` capture and a slim DPAD-safe TV scrubber. |
| `a37b9fd` | Rebuilt mobile/TV source onboarding around truthful choices, validation and provider verification before saving. |
| `47c22df`, `e3138e1` | Fixed the reported QA compilation failures: internal Compose `weight` access and duplicate TV onboarding helper names. |
| `707bd9c`, `f445063` | Prototyped and implemented unified Home/Live/movies/series/Search navigation. |
| `6f21a44`, `2b23930` | Approved and implemented the staged per-app localization foundation and QA-only language picker. |
| `0a9ab48` | Localized Home on mobile and TV. |
| `3e78af6` | Localized Live TV on mobile and TV. |
| `7b44577` | Localized movie and series browsing. |
| `ad2ea4f` | Localized global Search. |
| `a61f853` | Localized movie/series details, seasons and episodes. |
| `33bca82` | Localized shared player controls, audio/subtitle surfaces and playback errors. |
| `1d86198` | Localized source onboarding, validation, editing and management. |
| `c1d96ff` | Localized the complete EPG experience and EPG settings. |
| `e892e2a` | Localized the active Settings shell, playback/personalization preferences, category editor and directly opened sheets. |

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

## 5. Previously completed: Android TV live-transition parity

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
- Physical Android TV evidence is still required for CH+/CH-, rapid stepping, a
  failed provider URL, VLC fallback, Back, DPAD focus and VOD seeking. Do not
  confuse the later localization work with proof of those device behaviors.

## 6. Current approved navigation implementations

### Source onboarding

- The owner approved the functional mobile/TV direction in
  `prototypes/onboarding/SOURCE_ONBOARDING_FLOW_PREVIEW.html`.
- Mobile and TV now use the shared Android-free `PlaylistSourceDraftPolicy` for
  credential detection, normalization, field-level validation and `Playlist`
  construction.
- All app-owned source onboarding, file-import, connection, edit and management
  copy now comes from paired feature-owned Greek/QA-English resources. The same
  resource boundary is used by mobile, TV and the retained legacy edit route.
- Validation and pasted-source detection expose typed reasons/kinds;
  connection testing, submission and M3U import expose typed failures; settings
  source cards expose typed status. Pure policies no longer choose a display
  language or own localized fallback names.
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
- Static contracts cover the ownership, localization and focus boundaries. A
  normal Android Studio build plus phone/TV device QA is still required; Codex
  did not run Gradle.

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

### Staged localization: completed scope and exact remaining work

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
- Global Search is now localized across the active phone and TV routes:
  filters, typed headings/categories, result counts, empty states, featured
  actions and accessibility copy. `SearchUiPolicy` no longer owns localized
  labels or fallback sentences; `SearchLocalizationResources.kt` performs the
  Android mapping. `SearchKeyboardPolicy` keeps stable input actions and starts
  the TV keyboard from the active app language while retaining Greek, Latin and
  numeric layouts. Provider suggestions, titles, categories and metadata remain
  data.
- Movie/series details, seasons and episode browsing are now localized across
  the active phone and TV routes: hero actions, tabs, descriptions, metadata
  labels, season selectors, episode cards/counts, progress and accessibility.
  `DetailPresentation` carries a typed TMDB-notice flag rather than localized
  text, provider season labels remain state identities and are parsed only for
  localized display, and `WatchProgressPolicy` exposes typed `WatchRemaining`
  data instead of Greek formatting.
- Player localization is implemented across the shared mobile and Android TV
  chrome, audio/subtitle panels, automatic and editable manual OpenSubtitles
  flows, subtitle appearance controls, next-episode surfaces, player-context
  recommendations, inline programme states and playback/subtitle failures.
  `TrackLabelPolicy` no longer owns Greek language/fallback copy: both playback
  backends resolve language display names with the active app locale and obtain
  the numbered track fallback through Android resources. `SubtitleWiring`
  remains the single network/file boundary but resolves every user result via
  the caller's locale-aware `Context`. Provider titles/groups, track labels,
  subtitle filenames, OpenSubtitles result names and URL diagnostics remain
  untranslated data.
- Source onboarding and source management localization is implemented across
  mobile and TV, including the retained edit route, connection/file failures,
  statuses, counts and accessibility copy. `PlaylistSourceDraftPolicy`,
  `PlaylistConnectionMessagePolicy`, `PlaylistSourceSubmission` and
  `M3uFileImporter` return typed identities; `SourceLocalizationResources.kt`
  is their Android mapping boundary. Provider/source names, URLs, credentials,
  provider responses and stable method IDs remain untranslated data, and the
  existing validation/submission, encrypted persistence and TV focus/Back
  behavior were preserved.
- Full EPG localization is implemented across the active mobile and Android TV
  guide routes plus mobile EPG settings and the TV EPG settings row. Filters,
  programme actions, empty/accessibility states, discovery/download feedback,
  source labels, time and duration formatting now come from paired
  Greek/QA-English resources. `EpgStatus`, `EpgSourceOption` and
  `EpgLoadFailure` keep presentation copy out of state, coordination and data
  layers. Provider programme titles/descriptions, channel names, identifiers,
  URLs and hosts remain untranslated data. Live/player navigation and the
  existing TV focus/Back graph were not structurally changed.
- The active Settings shell is localized across mobile and Android TV, including
  overview/status copy, directly opened source/Premium/help sheets,
  playback/AFR/buffer dialogs, subtitle/audio preference pickers and category
  editing. `PlayerModeOption`, `AutoFrameRateOption`, `BufferProfile`, language
  codes and `CategoryEditorFailure` keep persisted identities and presentation
  copy separate. Existing preference keys, immediate persistence, TV focus and
  dialog Back behavior were not changed.
- The next cohesive localization slice is Profiles and the account/security
  dialogs. Audit `MobileAccountSyncScreen`, `SettingsAccountDialogs` and their
  profile/PIN/backup producers before editing. Move app-owned state messages to
  typed identities, keep names/PIN material and exported user data untouched,
  preserve encryption and source-scoped favorites/history, and do not mix the
  later Billing, Legal or Diagnostics migrations into that slice.
- Settings-slice static verification completed with 208 paired Settings
  resource keys and matching plural/placeholder structure, localization
  contracts passing, compatibility contracts 58/58, architecture audit 60
  passes plus the known `MainViewModel` size warning, deep validation 67 passes
  plus the documented cleartext compatibility warning, zero critical
  production-risk findings, documentation parity and a clean diff check. Codex
  did not run Gradle, compile or package; Android Studio compilation plus phone
  and TV behavior remain unverified until the owner supplies that evidence.
- Partial English resources live in the shared `app/src/localizationQa` source
  set used only by debug/QA. Production keeps the Greek unqualified baseline so
  English-system devices cannot receive a mixed-language public UI mid-migration.
- Mobile and TV language pickers are visible in owner QA builds. Public builds
  keep them hidden through `LOCALIZATION_PARITY_COMPLETE=false`; do not flip the
  flag or enable generated locale config until every release surface has matching
  English/Greek resources and the final audit passes.

#### Localization work that is genuinely complete

The following vertical slices have paired Greek-baseline/QA-English resources,
typed or stable presentation boundaries where needed, and static localization
contracts: runtime/primary navigation, Home, Live TV, movie/series browsing,
global Search, details/seasons/episodes, shared Player and subtitle/audio flows,
source onboarding/management, full EPG and the active Settings shell plus
playback/personalization/category surfaces. Provider-owned and user-owned data is
intentionally not translated.

“Complete” here means the code/resource migration and static gates are complete.
It does not mean that the current head has compiled or passed device QA. It also
does not mean public English can be enabled: the remaining slices below still
contain Greek display copy and raw display messages.

#### Immediate next slice: profiles and account/security dialogs

Work on this slice alone. Inspect these active boundaries before editing:

- `app/src/main/java/com/prelude/iptv/ui/mobile/settings/MobileAccountSyncScreen.kt`
- `app/src/main/java/com/prelude/iptv/ui/route/SettingsAccountDialogs.kt`
- `app/src/main/java/com/prelude/iptv/ui/coordinator/ProfileSettingsCoordinator.kt`
- `app/src/main/java/com/prelude/iptv/ui/route/SettingsRoute.kt`
- profile/PIN defaults and persistence in `PlaylistStore` and their focused tests

Important product truth: cloud/account synchronization is deferred and there is
no publisher backend. `MobileAccountSyncScreen` currently contains aspirational
cross-device/synchronization claims. Do not merely translate those claims and do
not add a backend. First trace whether the screen is active, then keep the flow
truthful to the implemented local profiles, source-scoped favorites/history and
encrypted file backup. If correcting that promise changes the approved product
flow or visual hierarchy, stop for owner approval and provide an HTML preview.

Preserve these contracts:

- profile names entered by the user are raw user data and must not be translated;
- existing persisted profile names/IDs and the primary profile cannot be silently
  renamed or migrated;
- PIN material never enters resources, logs, analytics or display diagnostics;
- protected-profile entry and parental unlock TTL behavior stay unchanged;
- switching profile keeps the existing restart/order semantics and TV Home sync;
- deletion keeps profile-scoped favorites/history cleanup and never affects a
  different profile;
- TV dialogs retain deterministic initial focus, DPAD traversal and exact Back
  restoration.

App-created fallback/default names such as the primary profile and unnamed
profile fallback are localization debt, but persisted user-visible values make
them migration-sensitive. Model a stable identity or inject display copy at the
UI boundary; do not rewrite stored names globally. Replace app-owned dialog,
toast, accessibility and error sentences with paired resources. Prefer typed
failure/state identities when a coordinator or producer currently owns display
text.

#### Remaining localization order after profiles

Complete one commit and verification cycle per item; do not combine them:

1. **Encrypted backup/import/export UI and failures.** Finish the backup branch
   in `SettingsAccountDialogs.kt` and `SettingsRoute.kt`, then audit `Backup.kt`,
   `PortableBackupCrypto.kt`, `Exporter.kt` and `ExportScreen.kt`. Preserve the
   AES/password format, SAF flow, filenames, JSON schema and existing backup
   compatibility. Provider/user data stays raw. Do not expose raw exception
   messages as the primary UI error; map known failures to typed app copy while
   keeping diagnostic causes internal.
2. **Billing and Premium.** Audit `BillingModels.kt`, `PlayBillingRepository.kt`,
   `PremiumState.kt`, `PremiumRequiredDialog.kt`, `MobileSettingsSheets.kt` and
   the mobile/TV Settings consumers. `BillingUiState.message` is known remaining
   presentation debt. Preserve BillingClient response handling, pending-purchase
   rules, acknowledgement, device verification and Play-provided formatted
   prices. A billing behavior change requires Terms/privacy/Play declaration
   review; localization alone must not change entitlement behavior.
3. **Legal and privacy.** Audit `MobileLegalPrivacyScreen.kt`,
   `MobileLegalComponents.kt` and especially `MobileLegalContent.kt`, whose model
   currently owns long Greek display copy. Keep publisher placeholders, policy
   version/effective date, URLs, service names and mandatory TMDB attribution
   accurate. Localization does not authorize rewriting legal meaning. Any legal
   substance change requires owner/publisher review and the documentation duties
   in `docs/MAINTENANCE.md`.
4. **Diagnostics and crash reporting.** Audit `MobileDiagnosticsScreen.kt`,
   `MobileDiagnosticsComponents.kt`, `DiagnosticsManager.kt` and diagnostic
   result producers. Preserve opt-in consent, redaction, one local pending report,
   no Analytics/ad ID, and disconnected Firebase configuration. Raw diagnostic
   details are not normal UI copy and must remain redacted.
5. **System notifications and remaining service copy.** Audit at least
   `CatalogDownloadService.kt`, `RelayService.kt`, exported/share surfaces and
   reminder/download notification producers. Notification channel names, titles,
   progress/errors and accessibility copy follow the app locale; protocol data,
   provider titles and user content remain raw.
6. **Final release-surface audit.** Search all active manifests, Kotlin and XML,
   not only files whose names contain “settings”. Classify each remaining literal
   as app copy, invariant brand/protocol text, provider/user data, diagnostic data
   or developer comment. Migrate only app copy, add contracts for every completed
   surface and verify Greek/English keys, placeholders and plurals.
7. **Parity inversion and public picker activation.** Only after the full audit,
   compilation and phone/TV QA: move English to unqualified `main/res/values`,
   Greek to `main/res/values-el`, change `unqualifiedResLocale` from `el` to `en`,
   enable the generated locale config and flip
   `LOCALIZATION_PARITY_COMPLETE=true`. Do not perform this as a mechanical file
   move without a dedicated plan, review and rollback-safe commit.

#### Required verification for every remaining localization slice

- Add paired feature-owned Greek and QA-English resources; avoid turning
  `strings_settings.xml` or legacy `strings.xml` into a universal giant file.
- Check key/type/plural/placeholder parity and unresolved `R.string` references.
- Extend `scripts/localization_contracts.py` to reject hardcoded app copy and
  presentation text leaking back into Android-free/state producers.
- Add focused unit tests for stable persisted identities and typed fallbacks.
- Run all allowed static gates and `git diff --check`, inspect the exact diff,
  update this handoff plus `CHANGELOG.md` when behavior changed, then commit the
  single cohesive slice.
- Never report compile, emulator or device success unless the owner supplies that
  evidence or explicitly authorizes the corresponding run.

## 7. Architecture work to continue afterward

Continue splitting legacy files only through small, behavior-preserving
extractions. Do not chase line count by moving arbitrary blocks.

Current largest Kotlin files at handoff time include:

- `MainViewModel.kt`: about 1,842 lines.
- `BrowseRoute.kt`: about 1,338 lines.
- `PlaybackEngine.kt`: about 978 lines.
- `PlayerHost.kt`: about 906 lines.
- `MobilePlaybackOverlay.kt`: about 904 lines.
- `PlaylistStore.kt`: about 772 lines.
- `TvLiveBrowseScreen.kt`: about 743 lines.

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
python scripts/localization_contracts.py
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
> `docs/MAINTENANCE.md`, `docs/LOCALIZATION_ARCHITECTURE.md` and
> `docs/ARCHITECTURE_REFACTOR_PLAN.md` completely before acting. Inspect
> `git status --short` and recent commits first; do not trust an old completion
> claim without tracing the active code path.
> Continue as a 30-year senior engineer: one careful responsibility at a time,
> small patches only, never rewrite a whole file, avoid giant files, preserve
> public/storage behavior and add focused tests/contracts. Extend focused files
> and feature resources instead of collecting everything in one giant file.
> Every visual/layout/navigation/focus change requires a functional HTML preview
> and my approval before Android implementation; a copy-only resource migration
> does not. Do not run Gradle, compile or build unless I explicitly ask. Never
> claim runtime success from static checks. Record behavior changes in
> CHANGELOG/docs and commit each cohesive completed slice. The immediate task is
> Profiles and
> account/security-dialog localization across phone and TV. Audit
> `MobileAccountSyncScreen`, `SettingsAccountDialogs` and the profile/PIN/backup
> producers first. The app has no account/cloud-sync backend: do not translate or
> preserve false cross-device promises and do not add Supabase/VPS/backend. Trace
> the active flow and keep it truthful to local profiles and encrypted file
> backup; stop for approval if that requires a product-flow or visual change.
> Keep user profile names, PIN material, persisted IDs/keys and exported user data
> untouched; move app-owned labels, states and errors to paired Greek/QA-English
> resources through typed identities. Preserve encryption, profile/source-scoped
> favorites/history, restart ordering, TV Home sync, TV DPAD focus and exact Back
> restoration. Do not mix Billing, Legal or Diagnostics into this slice. Run the
> static gates, inspect the diff, update the handoff, and commit only when the
> slice is cohesive and clean.
