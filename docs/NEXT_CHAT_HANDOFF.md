# Prelude+ next-chat handoff

Last verified workspace date: **2026-08-08**  
Workspace: `C:\Users\konst\AndroidStudioProjects\chatgptiptv`  
Branch: `main`  
Current documented version: **1.46.0** (`versionCode 115`)  
Last repository commit: `1a7b4f4 feat: add premium live channel transition`

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
- Mobile premium directional transition during live channel changes.
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

## 5. Immediate next task: Android TV parity for live transitions

The last open discussion was whether the mobile premium live-channel behavior
should also exist on Android TV. The decision was **yes, adapted to TV**, but no TV
implementation or new TV prototype has been made yet.

### Required sequence

1. Inspect the existing TV player, remote-channel-step routing, focus policy and
   the approved mobile transition implementation.
2. Create a functional TV HTML prototype first, preferably:
   `prototypes/player/LIVE_CHANNEL_WATER_TRANSITION_TV_PREVIEW.html`.
3. The TV prototype should demonstrate both next-channel and previous-channel
   directions, without an arrow, and should use a more restrained intensity for a
   large screen.
4. Show the HTML to the owner and wait for explicit approval.
5. Only after approval, add a focused TV transition component and integrate it
   above the existing video surface without introducing another player/surface.
6. Direction must come from next/previous channel intent or queue delta, not a
   mobile swipe gesture:
   - next channel -> one direction;
   - previous channel -> the opposite direction.
7. Apply the already approved slimmer progress-bar visual logic to TV only if it
   preserves a generous focus/interaction area. The visible bar may be thin; the
   focus target must remain suitable for DPAD use.
8. Keep Back, DPAD, seek-bar ownership, overlay auto-hide and focus restoration
   unchanged unless a focused test proves a necessary adjustment.
9. Add pure motion/direction tests and extend the TV focus/static contracts.
10. Update `CHANGELOG.md`, run static audits, review the diff and commit. Do not run
    Gradle unless the owner explicitly requests it.

### TV acceptance criteria

- No old chevron or directional arrow appears.
- Rapid channel stepping cannot leave a stale transition on screen.
- Failed stream resolution does not visually commit the failed channel.
- Only the committed playback surface remains visible.
- The animation cannot consume DPAD events or receive focus.
- Seek-bar focus, left/right seeking, confirm, Back and overlay dismissal retain
  current behavior.
- No focusable control is clipped at TV overscan-safe edges.

## 6. Architecture work to continue afterward

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

## 7. Remaining release gates

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

## 8. Validation and documentation workflow

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

## 9. Prompt to start the next chat

The owner can paste the following after attaching or referencing this file:

> Read `docs/NEXT_CHAT_HANDOFF.md`, `README.md`, `CHANGELOG.md`,
> `docs/MAINTENANCE.md` and `docs/ARCHITECTURE_REFACTOR_PLAN.md` before acting.
> Continue as a 30-year senior engineer: one careful responsibility at a time,
> small patches only, never rewrite a whole file, avoid giant files, preserve
> public behavior and add focused tests. Every visual change requires a functional
> HTML preview and my approval before Android implementation. Do not run Gradle or
> build unless I explicitly ask. Record changes in CHANGELOG/docs and commit each
> cohesive completed change. The immediate task is the Android TV HTML preview for
> the premium directional live-channel transition and slim but DPAD-safe progress
> bar described in the handoff.

