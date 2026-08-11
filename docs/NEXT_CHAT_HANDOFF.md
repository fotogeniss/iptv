# Prelude+ next-chat handoff

Last verified workspace date: **2026-08-10**
Workspace: `C:\Users\konst\AndroidStudioProjects\chatgptiptv`  
Branch: `main`  
Current documented version: **1.46.0** (`versionCode 115`)  
Latest completed implementation slice in this handoff:
the six-commit live bug-fix batch described in section 0 (Stalker series
episodes, live-transition flash, Stalker catalog performance and three
smaller fixes), all owner-confirmed on device and committed.

This document is the operational source of truth for continuing the current
Codex collaboration in a fresh chat. Read it together with `README.md`,
`CHANGELOG.md`, `docs/MAINTENANCE.md` and
`docs/ARCHITECTURE_REFACTOR_PLAN.md` before changing code.

## 0. MOST RECENT SESSION — live bug-fix batch (committed, verified)

**Read this first.** The previous session produced a batch of live bug fixes
that sat uncommitted because the sandboxed agent shell was unavailable for
its entire duration. This session ran the static gates, split that work into
six cohesive commits and committed all of them. The owner confirmed on
2026-08-10 that the app builds and behaves correctly on **both mobile and
Android TV**, which covers the first six fixes below.

`e26a5aa` came afterwards, from a bug the owner reported once episodes were
finally playable, and the owner **confirmed it working** on 2026-08-10.

`2aa060c` and `8ec4658` are the newest pair, from the follow-up report that
greeklish-titled Greek series showed the general synopsis on every episode.
They are gate-clean but **not yet device-confirmed** — see the greeklish
section below for exactly what to ask.

Commits, oldest first, on top of `f1f75e0` (`feat: localize library hub`):

| Commit | Slice |
| --- | --- |
| `75dd8fa` | `test: restore PremiumLibraryPolicyTest rail labels argument` |
| `b94043b` | `fix: gate pending series requests on the series generation alone` |
| `81dcbb1` | `fix: load Stalker series episodes from the season descriptor` |
| `7118354` | `feat: localize the subtitle result fallback title` |
| `718965f` | `fix: cover the video surface before the live channel transition starts` |
| `3fa2cd2` | `perf: restore gzip and raise provider concurrency for Stalker catalogs` |
| `e26a5aa` | `fix: give each Stalker episode its own identity key` |
| `2aa060c` | `feat: add a greeklish title matching policy` |
| `8ec4658` | `fix: find greeklish-titled Greek series on TMDB` |
| `147433a` | `fix: stop caching failed TMDB episode lookups in memory` |
| `3ab8810` | `feat: log TMDB title lookup under a TmdbLookup tag` |
| `a389a44` | `feat: add a section navigation history policy` |
| `b76ee91` | `fix: make back return to the previous section everywhere` |
| `486b2b5` | `fix: strip provider decoration from titles before searching TMDB` |

`CHANGELOG.md` under `Unreleased` carries the full mechanism-level writeup
for each of these; it is the authoritative technical record and is not
repeated here. What follows is only what a future session still needs to
know that the changelog does not say.

### The identity-key lesson — read before touching episodes again

`e26a5aa` existed only because `81dcbb1` fixed **one** of the two identity keys
in this codebase and nobody checked for a second one. That is the mistake worth
not repeating.

There are two independent identity functions for a `Channel`, they disagree
about which fields matter, and both are persisted:

| Function | Basis | Used by |
| --- | --- | --- |
| `PlaybackHistoryStore.historyMatchKey` | `streamId`, then `seriesId`, then `chId`, then metadata | history reconciliation and migration |
| `PlaybackQueue.favKey` | `url`, then `cmd`, then `seriesId` — **never `streamId`** | favorites, recents, resume position, subtitle requests, `LibraryPolicy.unique`, `CatalogPolicy.key`, `libraryKey` |

`81dcbb1` made `streamId` unique per episode precisely because
`historyMatchKey` keys off it, and its own changelog entry says so. But
`favKey` — which the entire UI layer actually calls, and which the player uses
for resume position — does not look at `streamId` at all, so it kept falling
through to `cmd`, which Stalker deliberately shares across a whole season.

The general shape of the trap: **a Stalker episode is not identified by its
`cmd` or its `url`.** It is identified by the `series=` number that
`create_link` receives. Any new code that keys, de-duplicates, groups or
persists episodes must go through `PlaybackQueue.favKey` rather than inventing
its own key from `url`/`cmd`, and any change to `favKey` is a stored-data
contract change that needs the reasoning written down. `PlaybackQueueIdentityTest`
now pins both the new episode behavior and the unchanged live/VOD/series/Xtream
keys; if a future change makes it fail, that is the contract talking.

### Greeklish title matching — what to verify and what is still unknown

`8ec4658` is the only uncertain thing in this batch. The diagnosis itself is
solid and came from the owner, not from guessing: Greek series with a **Greek**
title already show correct per-episode synopses, and only **greeklish** titles
fall back to the show-level plot. That isolates the failure to TMDB lookup, not
to the display path — all four episode renderers were already correct.

**What could not be verified without a device or an API key:**

- Whether `GreeklishTitlePolicy.toGreek()` produces a query TMDB actually
  resolves. The transliteration is deliberately approximate (no accents, some
  endings wrong) and relies on TMDB's fuzzy search. The skeleton comparison
  guarantees a *wrong* show is rejected, but not that the *right* one is found.
- Which greeklish convention the owner's provider actually uses. The policy was
  built to be convention-neutral and its tests cover `x`/`h`/`ch` for chi,
  `i`/`h` for eta and `th`/`8` for theta simultaneously, but no real title from
  the owner's list was ever seen. **If this is revisited, ask for ten real
  titles first** — that request was made and the owner chose to proceed without
  them, which is why the design avoids depending on any one convention.

**The owner then supplied a real list title, and it settled the question.**
`486b2b5` is the actual fix. The screenshot showed `To Spiti Dipla Sto Potami #`
— the trailing `#` is the provider's own marker, it survived `cleanTitle`, and
it rode into the search query as `%23`. Everything else in the chain was
already correct: the transliteration produced `το σπιτι διπλα στο ποταμι`, and
the list title's skeleton already equalled the real Greek title's, so
verification would have accepted the match. One stray character was the whole
failure. **This is what the ten sample titles would have revealed immediately.**

**The two things that preceded it**, neither a guess at the transliteration:

- `147433a` fixed a real, provable bug found while investigating: `episodeMeta`
  cached an **empty** result in memory, so one throttled or dropped call marked
  a series unknown for the whole life of the process. Opening a season fires
  many card lookups at once against a semaphore, so this was easy to trigger and
  would mask any lookup fix. It is the same shape of mistake as the favKey one —
  a guard applied to `fetch()` and the disk cache but never to the neighbouring
  in-memory write, with the file's own comment explaining why it was wrong.
- `3ab8810` added the `TmdbLookup` tag, because TMDB search fails silently and
  the screen cannot tell the three failure modes apart.

**Do not touch the transliteration until a `TmdbLookup` capture exists.** The
log distinguishes exactly the cases that need opposite fixes:

| Log line shows | Meaning | Fix direction |
| --- | --- | --- |
| `δεν θεωρήθηκε greeklish` | `looksGreeklish` rejected the title | widen detection, or the title has an English marker word |
| `ερώτημα «…»` then `0 αποτελέσματα` | TMDB found nothing for the Greek query | improve `toGreek`, needs real titles |
| results listed, `ταίριασμα=0` | TMDB found the show but the skeleton comparison rejected it | loosen `isSameTitle` / fix skeleton |
| `tmdbId=` non-zero, `0 επεισόδια` | series resolved but the season number is wrong | the season index passed by the caller, not this policy |

The fourth row is worth stressing: nothing in the greeklish work touches the
season number, and a Stalker season label is free-form provider text.

**Verification method used instead of Gradle:** the policy was prototyped and
run outside the build against 24 Greek/greeklish title pairs spanning four
conventions, then every assertion in `GreeklishTitlePolicyTest` was executed by
reproducing the Kotlin faithfully. All pass. That is static evidence about the
policy's internal consistency and says nothing about TMDB's behavior.

### The `fetchEpisodeDescription` question — probably dead weight

The owner's report implies something the changelog for `81dcbb1` does not yet
admit: if greeklish-titled series show **the same** general synopsis on every
episode, then `Channel.plot` — which `StalkerClient.fetchEpisodeDescription`
fills with one extra HTTP request **per episode** — is carrying the series-level
description, not a per-episode one. If so, that request costs a round trip per
episode and buys nothing.

This was not removed because it is not proven. Confirm it with a Logcat capture
of the `SeriesLoad` tag, line `seriesEpisodes episode detail`, for **two
different episodes of the same season**: identical `description` values settle
it. If confirmed, deleting the per-episode fetch is a clean, self-contained
performance win for every Stalker series load.

### retrodb.gr — investigated, deliberately not used

The owner raised `https://retrodb.gr` as a possible source for Greek series
metadata. It was not adopted, for three recorded reasons:

1. Its content is client-rendered and it exposes no reachable JSON endpoint that
   could be confirmed (`/wp-json/` returns nothing); whether it has any public
   API at all is still unknown.
2. A second database does not solve the actual problem. The failure was title
   *matching*, not missing data — TMDB already has these series with Greek
   per-episode synopses, which is exactly why Greek-titled series work.
3. Adding an external service triggers the privacy and Play Data Safety review
   rules in `docs/MAINTENANCE.md`, which is real cost for no proven benefit.

Revisit only if `8ec4658` is confirmed on device and a genuine gap remains for
Greek series that TMDB does not carry at all.

### Back navigation — the audit and what it changed

The owner asked for a serious flow audit: back must return exactly where they
were, never further, never Home, and the on-screen arrows must agree with the
device button. The audit found one cause behind everything.

**The root cause:** the current catalog section was a single variable
(`mobilePrimaryDestination` / `tvSection`), not a stack. Nothing recorded where
the user came from, so "back" between sections did not exist — what existed
were fixed destinations dressed as back. Everything that *did* work correctly
(details, library, search, EPG, export, pickers) worked because it is a layer
*on top*, not because there was history.

Fixed in `a389a44` + `b76ee91`. Three concrete defects, listed in the changelog
with their mechanisms.

**Two structural facts worth keeping in mind before touching this again:**

- **Declaration order of `BackHandler` is reverse priority.** Compose gives BACK
  to the *last active* handler, so the section handler is declared *before* the
  overlay handler on purpose. Its `enabled` condition is also mutually
  exclusive with that handler, deliberately duplicating the guarantee — a
  silent reordering of composition would otherwise change behaviour with no
  test failing.
- **`MainActivity:266` is `enabled = true` unconditionally** and acts as the
  final catch-all: anything no one else claims becomes "change source?". That
  is why a missing handler shows up as the wrong dialog rather than as a dead
  button, and why adding a screen without a handler is a silent bug.

**TV screens carry no `BackHandler` of their own** — details, library, search
and EPG all rely on the `when` in `BrowseRoute`. That is fine, but it means the
ordering in that `when` is the single source of truth for TV back behaviour.

**Not device-confirmed.** Worth walking on device: Series → Live → back should
give Series; Home → Movies → Series → Movies → back should give Home directly;
the arrow and the device button should behave identically at every step; and at
the root both should still ask to change source.

### Still-open follow-ups

- **`XtreamClient.seriesEpisodes` has the same missing-per-episode-description
  gap** that `81dcbb1` closed for Stalker. It was deliberately left untouched
  because the reported bug and the owner's device testing were specific to
  their Stalker portal. Ask before extending it.
- **Diagnostic logging is intentionally still in place** under the
  `SeriesLoad` tag — `StalkerClient.seriesEpisodes()` logs the raw
  `get_ordered_list` response and the per-episode detail response, and
  `MainViewModel.openSeries()` logs the load outcome, stale-generation
  discards and failures. This is not leftover debug code: three rounds of
  guessing failed on this bug and only raw portal JSON solved it. Remove or
  downgrade it only if the owner asks.
- **The per-episode description field name was originally a guess** (the
  response's `"description"`). The owner's device confirmation says episodes
  and descriptions work, so the guess held for their portal. A portal that
  shapes the field differently will fall back to an empty description rather
  than fail, and the `SeriesLoad` logging above is enough to diagnose it
  without new instrumentation.
- **`MainViewModel.kt` is 1860 lines** and trips the architecture audit's
  known size warning. `81dcbb1` added 14 lines of logging to it. This is the
  one standing `WARN` in the gate output; extraction is tracked in
  `docs/ARCHITECTURE_REFACTOR_PLAN.md`.

### Two environment facts that were wrong in the previous handoff

- **`MobilePlaybackOverlay.kt` was not "a pure line-ending normalization
  diff".** Its working-tree copy had been rewritten with CRLF endings while
  `HEAD` and every other file in the repo use LF, which is why it showed as
  754/754 (later 919/919) fully modified and produced 919 `trailing
  whitespace` errors under `git diff --check`. This session normalized it
  back to LF before committing; the file's real change in `718965f` is 21
  insertions and 6 deletions. If it ever shows as fully modified again,
  check the line endings first — the repo has no `.gitattributes`, so an
  editor or tool writing CRLF will silently reintroduce this.
- **The `.git` write-lock problem is solved, not permanent.** Git writes from
  the sandboxed mount do still leave `.git/HEAD.lock` and
  `.git/objects/maintenance.lock` behind after each command, and `HEAD.lock`
  blocks the *next* command. But the agent can now delete them itself once
  file deletion has been granted for the folder (Cowork's
  `allow_cowork_file_delete`). The working pattern is simply to append
  `rm -f .git/HEAD.lock .git/objects/maintenance.lock .git/index.lock` after
  every git write command. There is no longer any need to ask the owner to
  delete lock files from Windows, and no need to avoid chaining git commands.

### Static gate results at `8ec4658`

All six gates in section 9 pass. `git diff --check` is clean. Two standing,
pre-existing warnings: the `MainViewModel` size warning above, and the
documented global-cleartext compatibility exception in `deep_validation_audit`.

Gradle was **not** run at any point — the owner builds in Android Studio. Their
2026-08-10 reports are the build and device evidence for everything up to and
including `e26a5aa`. `2aa060c` and `8ec4658` have no device evidence yet.

Where Gradle was unavailable, logic was verified by reproducing the Kotlin
outside the build and executing the same assertions:

- `e26a5aa`: a season's four episodes collapse to one key before the change and
  to four after it, with live/VOD/series/Xtream keys byte-identical either way.
- `2aa060c`/`8ec4658`: 24 Greek/greeklish title pairs across four conventions
  all produce matching skeletons, unrelated titles do not collide, and every
  assertion in `GreeklishTitlePolicyTest` passes.

This is static reasoning about internal consistency, not a runtime result, and
in the greeklish case it says nothing about how TMDB responds to the generated
query.

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
12. Account/cloud synchronization is not part of the current local-only product.
    Do not add Supabase, a VPS or another backend without a new explicit decision
    from the owner.
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

- HEAD is at `3fa2cd2` (`perf: restore gzip and raise provider concurrency for
  Stalker catalogs`), the last of the six bug-fix commits listed in section 0,
  which sit directly on top of `f1f75e0` (`feat: localize library hub`).
  Verified from `git log`, not trusted from a prior document. Confirm the
  current HEAD with `git log --oneline -8` rather than trusting a hash typed
  into this file, since these lines are not updated after every future commit.
- The worktree is clean apart from whatever the current session is editing.
  The long-standing "`MobilePlaybackOverlay.kt` shows as fully modified but is
  a pure line-ending normalization" note is **resolved and no longer true**:
  the file had genuinely been rewritten with CRLF endings against an LF repo,
  it was normalized back to LF, and it is committed. See section 0 for the
  detail and for what to check if it ever reappears.
- **`.git` write locking on this sandboxed mount: solved.** Git writes from
  the sandbox still leave `.git/HEAD.lock` and `.git/objects/maintenance.lock`
  behind, and a stale `HEAD.lock` blocks the next command with
  `fatal: cannot lock ref 'HEAD'`. The agent can now delete these itself once
  file deletion has been granted for the folder — in Cowork that is the
  `allow_cowork_file_delete` tool, which only needs approving once per folder.
  **The working pattern:** append
  `rm -f .git/HEAD.lock .git/objects/maintenance.lock .git/index.lock` to
  every git write command. The old workflow of asking the owner to delete lock
  files from Windows, and of never chaining git commands, is obsolete. The
  `warning: unable to unlink '.git/objects/**/tmp_obj_*'` lines that git emits
  on the mount are harmless and do not affect the commit.
- The Git worktree was clean at `4c7ee73` when the profile/account-security
  slice began.
- The owner previously supplied an Android Studio screenshot confirming a
  **successful QA build in approximately 34 seconds** after commit `1a7b4f4`, and
  later reported another localization checkpoint built without errors. Those are
  owner-provided historical build results; they do **not** prove that the current
  profile/account-security slice compiles or passes device QA.
- Codex did not run Gradle for the current localization work because the owner did
  not authorize it. Android Studio compilation and phone/TV checks for the
  current head remain outstanding.
- The owner then reported `:app:compileQaKotlin` failures in
  `BrowseStateComponents.kt`, `DetailRouteHost.kt`, `PlaylistSourcesScreen.kt`,
  `SettingsFieldComponents.kt` and `SettingsPlaybackDialogs.kt`: wildcard imports
  exposed several dependency `R` classes, so unqualified `R.string` was
  ambiguous. The focused fix adds explicit `com.prelude.iptv.R` imports to those
  five files and a static regression contract. The owner must rerun the QA build;
  this document does not claim that rebuild has passed.
- That rebuild advanced to `ProviderImportScreens.kt` and exposed one more stale
  EPG consumer: it still compared typed `EpgStatus` values with Greek strings,
  called string methods on the status, destructured `EpgSourceOption` as an old
  label/URL pair and passed typed objects to `Text`. The focused correction uses
  `EpgStatus` loading identities, `localizedText()`, `localizedLabel()` and the
  source's raw URL, with a regression contract. A new owner QA build is still
  required after this fix.
- The latest static validation cycle for profiles/account security reported:
  - localization contracts: pass, including 58 paired account/security resources
    with matching key/type/placeholder structure;
  - architecture audit: 60 passes, one known size warning for `MainViewModel`, no
    failures;
  - compatibility contracts: 63/63, including profile switch/cleanup ordering,
    parental TTL, backup format/crypto constants and TV dialog focus boundaries;
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
- Account/cloud sync is not part of the current local-only product.

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
- Profiles, parental controls and encrypted backup/restore are localized across
  the active mobile and Android TV routes, including the startup profile gate,
  shared dialogs, SAF completion states and typed backup failures. The active
  account carousel now states that profiles and progress are local and that
  transfer requires a manual encrypted file; it no longer promises an account,
  cloud backend or automatic cross-device synchronization. User-created profile
  names, PIN material, persisted IDs/keys, switch/restart ordering, cleanup,
  TV Home scheduling and backup compatibility remain unchanged.
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
playback/personalization/category surfaces, local profiles, parental controls and
encrypted backup/restore, Billing and Premium, and the active in-app Legal and
Privacy and Diagnostics presentations. Provider-owned, user-owned and raw
diagnostic data is intentionally not translated.

Export/relay surfaces (`ExportScreen`), the catalog-download/relay/EPG-
reminder system notifications, and the Library hub (Favorites/My List,
Continue watching, History) are also migrated at the code/resource and
static-contract level and committed (see section 3/`git log`).

“Complete” here means the code/resource migration and static gates are complete.
It does not mean that the current head has compiled or passed device QA. It also
does not mean public English can be enabled: the remaining slices below still
contain Greek display copy and raw display messages.

#### Completed slice: profiles and account/security

- `MobileAccountSyncScreen` is active from mobile Settings. Its existing
  carousel layout, swipe behavior and controls remain intact, but its three
  typed page identities now describe local profiles, device-local state and
  manual encrypted backup instead of account/cloud synchronization.
- `PremiumProfileGate` and the shared profile/PIN/backup dialogs use paired
  feature resources on mobile and Android TV. The primary profile uses an
  app-owned typed display identity; every non-primary stored name passes through
  unchanged as user data.
- `BackupFailure` and `BackupException` replace producer-owned display sentences.
  The Settings boundary maps known failures to localized copy and no longer
  exposes raw exception messages as the primary error.
- No profile ID/key, PIN handling, JSON field, filename, PBKDF2/AES-GCM parameter,
  legacy import rule, switch/restart step, cleanup step, TV Home schedule or
  DPAD/Back/focus modifier changed.

#### Completed slice: Billing and Premium

- `BillingUiState.message` now carries a sealed `BillingMessage` identity instead
  of producer-formatted Greek sentences. Mobile, TV and the shared Premium gate
  map those identities to paired Greek/QA-English resources at the UI boundary.
- The shared gate's feature titles/explanations, purchase availability, one-time
  offer label and actions are paired resources. The mobile Premium sheet, TV
  account rows and mobile Premium badge use the same staged localization model.
- Unknown nonblank Google Play `debugMessage` details remain provider-owned data;
  blank details receive localized app-owned fallback copy. Play-provided
  `formattedPrice` remains unmodified in every purchase surface.
- BillingClient response branches, product ID/type, pending handling, restore
  semantics, acknowledgement, device verification, entitlement reduction,
  persisted keys/enum values, QA/public gates and UI focus/navigation/layout were
  not changed. No HTML preview was required because this was copy-only.
- The verifier's rejected reason retains its public `String` API but now carries
  a stable non-display diagnostic identity instead of Greek UI copy. The retained
  `SERVER` verification enum remains readable for persisted compatibility; the
  application has no publisher backend and does not promise one.
- Static localization/resource parity, compatibility (63/63), architecture
  (60 passes plus the known `MainViewModel` size warning), deep validation
  (67 passes plus the documented cleartext compatibility warning), risk and
  documentation gates pass. Focused verifier/message-policy tests were added but
  were not executed because the owner did not authorize Gradle. Android Studio
  compilation and mobile/TV QA remain pending owner evidence.

#### Completed slice: Legal and privacy

- The active route is mobile Settings → `MobileLegalPrivacyScreen`; no separate
  Android TV Legal/Privacy screen is currently wired. Existing Back handling,
  sticky tabs, disclosure expansion IDs/order, scrolling and layout are unchanged.
- `MobileLegalTab`, `MobileLegalDisclosure`, `MobileLegalService` and
  `MobileLegalTerm` are typed identities. Their app-owned labels, summaries,
  details, statuses and term copy map to paired Greek/QA-English resources at the
  Compose boundary instead of living as Greek strings in `MobileLegalContent`.
- The five disclosure IDs, five service IDs/badges, seven-term order,
  `1.1-draft` policy version, effective date identity `2026-08-02`, empty
  publisher/privacy-contact placeholders and exact mandatory English TMDB
  attribution remain protected by focused tests/static contracts.
- `docs/PRIVACY_POLICY.md`, `docs/TERMS_OF_USE.md` and
  `docs/PLAY_DATA_SAFETY.md` were read as the canonical sources. Their legal
  substance was not rewritten; publisher/legal review and the public policy URL
  remain release blockers. No HTML preview was required because this was
  copy-only localization.
- Static localization/resource parity, compatibility (63/63), architecture
  (60 passes plus the known `MainViewModel` size warning), deep validation
  (67 passes plus the documented cleartext compatibility warning), risk,
  documentation and diff checks pass. Focused legal-content tests were expanded
  but not executed because the owner did not authorize Gradle. Android Studio
  compilation and mobile QA remain pending owner evidence.

#### Completed slice: Diagnostics and crash reporting

- The active route is mobile Settings → `MobileDiagnosticsScreen`; no separate
  Android TV Diagnostics screen is currently wired. Existing Back handling,
  conditional pending/setup cards, switch/action enablement and list order are
  unchanged.
- `DiagnosticsState.message` now carries a sealed `DiagnosticsMessage` identity.
  Manager feedback and all screen/component copy map to paired Greek/QA-English
  resources at the Compose boundary. Pending-report timestamps use the active app
  locale, and the consent switch has paired accessibility copy.
- `PendingDiagnosticReport.exceptionType`, `summary` and `stackSummary` remain raw
  redacted diagnostic data. Existing stored Greek summaries remain displayable;
  no report schema, SharedPreferences file/key, one-report retention or redaction
  token changed.
- Firebase startup remains explicit after opt-in or “send once”; automatic
  collection stays disabled, turning reporting off still deletes unsent Firebase
  reports, and Analytics/ad IDs remain absent. These behaviors are now protected
  by compatibility contracts in addition to focused redactor/state tests.
- `docs/PRIVACY_POLICY.md`, `docs/PLAY_DATA_SAFETY.md` and
  `docs/FIREBASE_CRASHLYTICS_SETUP.md` were read as canonical behavior sources and
  did not require substance changes. No HTML preview was required because this
  was copy/accessibility-only localization.
- Static localization/resource parity, compatibility (66/66), architecture
  (60 passes plus the known `MainViewModel` size warning), deep validation
  (67 passes plus the documented cleartext compatibility warning), risk,
  documentation and diff checks pass. Focused redactor/state tests were added or
  expanded but not executed because the owner did not authorize Gradle. Android
  Studio compilation and mobile QA remain pending owner evidence.

#### Completed slice: Export/relay surfaces and system notifications

- The active route is `ExportScreen` (opened from `BrowseRoute` via
  `showExport`), backed by the Android-free `ExportRelayCoordinator` and
  `Exporter.kt`. `Exporter.saveToDownloads` was traced and confirmed **dead
  code**: nothing on the active export path calls it (the screen saves through
  the SAF `CreateDocument` launcher instead), so it required no localization
  and was left untouched.
- All app-owned `ExportScreen` copy (back/title, selected-count plural, group
  quick actions, the MAC-portal notice, generate/save/copy/stop/start relay
  action labels and the saved/error/relay-started/copied toast messages) now
  comes from paired `strings_export.xml` resources. Channel/channel-count
  numbers, the raw relay URL, filenames, the `.m3u` extension and the
  `audio/x-mpegurl` MIME type remain unchanged data.
- The three active notification producers found by tracing
  `NotificationChannel(`/`setContentTitle(`/`setContentText(` across the whole
  `app/src/main/java` tree — `CatalogDownloadService`, `RelayService` and
  `ReminderScheduler`/`ReminderReceiver` — now build their channel
  names/descriptions, titles and progress/body text from paired
  `strings_notifications.xml` resources via `getString(...)`/`ctx.getString(...)`
  (the same non-Compose Android-boundary pattern already used by
  `SubtitleWiring.kt`). Notification channel IDs, notification IDs,
  `PendingIntent` flags, foreground-service lifecycle/`START_*` semantics, the
  relay URL text and the reminder deep-link intent are unchanged.
- `scripts/localization_contracts.py` gained a hardcoded-Greek-literal audit
  for `ExportScreen.kt` and the three notification producers, in addition to
  the pre-existing global Greek/English resource-parity checks that already
  cover the two new `strings_export.xml`/`strings_notifications.xml` files.
- Static verification after this slice: localization contracts pass;
  compatibility contracts 66/66; architecture audit 60 passes plus the known
  `MainViewModel` size warning; deep validation 67 passes plus the documented
  cleartext-HTTP compatibility warning; production-risk inventory 0 critical
  findings (7 categories checked, run against a temporary read-only copy of
  `app/src/main/java` because the mounted workspace's I/O latency made the
  script exceed this session's per-command time budget — the copy was
  read-only source, not a different codebase); documentation contract passes;
  `git diff --check` is clean for every file this slice touched. Gradle,
  compilation and device QA were not run.
- Committed as `feat: localize export/relay surfaces and system
  notifications` (see `git log` for the exact hash — it was amended once to
  remove Library-slice contract code that had been accidentally staged
  alongside it before either slice was committed).

#### Completed slice: Library hub (Favorites/My List, Continue watching, History)

- The active route is `PremiumLibraryScreen` (`app/src/main/java/com/prelude/iptv/ui/PremiumLibraryScreen.kt`),
  reached from `BrowseLibraryLayer.kt`; it delegates to `TvPremiumLibraryScreen`
  or `MobilePremiumLibraryScreen` depending on device type. This entire
  feature area (headers, tabs, sort/manage, rails, cards, info panel, hero
  copy, empty states) had **no** prior localization pass — it was found
  during the reconnaissance sweep for the next audit item, not re-touched
  work.
- `LibraryHubTab` and `LibrarySort` (`LibraryFoundation.kt`) no longer carry a
  `val label: String`; a new `LibraryLocalizationResources.kt` adds
  `labelRes()` mappings for both, plus `LibraryDestination.eyebrowRes()` for
  the uppercase "eyebrow" label shown above the selected title in the TV info
  panel and the mobile hero.
- `libraryRails()` no longer builds Greek rail titles/subtitles itself; it now
  takes a caller-supplied `LibraryRailLabels` bundle (titles, description
  subtitles for the "All" tab, and locale-aware pluralized count subtitles for
  the single-tab views). Both `TvPremiumLibraryScreen` and
  `MobilePremiumLibraryScreen` build that bundle via the new
  `libraryRailLabels(content)` composable helper.
- `libraryDescription()` now returns `String?` (null when neither TMDB nor the
  provider expose a description) instead of embedding a hardcoded Greek
  fallback sentence; both call sites (`TvLibraryComponents.kt`,
  `MobileLibraryComponents.kt`) apply the localized fallback with `?:`.
  Provider/TMDB titles, descriptions, genres and stored favorite/continue-
  watching/history entries remain unchanged data; only app-owned labels,
  actions, headers and empty/fallback copy moved to paired
  `strings_library.xml` (Greek baseline) / QA-English resources.
- Static verification: localization contracts pass; compatibility contracts
  66/66; architecture audit 60 passes plus the known `MainViewModel` size
  warning; deep validation 67 passes plus the documented cleartext-HTTP
  compatibility warning; production-risk inventory 0 critical findings (same
  read-only-copy technique as the export/notifications slice, for the same
  mounted-workspace I/O reason); documentation contract passes; `git diff
  --check` is clean for every file this slice touched. Gradle, compilation and
  device QA were not run.
- Committed as `feat: localize library hub` (see `git log` for the exact
  hash).

#### Next slice: Final release-surface hardcoded-string audit

A reconnaissance sweep this session (`rg` for quoted Greek-Unicode-range
literals under `app/src/main/java`) found **57 files with 330 occurrences**
before the Library slice above; Library accounted for 7 of those files (~43
occurrences) and is now done, leaving **50 files** still to triage. This list
was not yet individually classified file-by-file — do that first, in this
order, before editing:

1. **Final release-surface audit.** Search all active manifests, Kotlin and XML,
   not only files whose names contain “settings”. Classify each remaining literal
   as app copy, invariant brand/protocol text, provider/user data, diagnostic data
   or developer comment. Migrate only app copy, add contracts for every completed
   surface and verify Greek/English keys, placeholders and plurals. Re-run the
   same `rg` sweep first since file contents have moved on since this count.
   Known already-audited/allowed exceptions from earlier slices (do not
   re-flag): the two classifier literals in `LiveFoundation.kt`, the two in
   `MobileEpgGuide.kt`, the two in `PlaylistSourceDraft.kt`, the two in
   `PlaylistConnectionMessagePolicy.kt`, and the documented literals in
   `MobilePlaybackOverlay.kt`/`TvPlaybackOverlay.kt`. Treat remaining files
   such as `MainViewModel.kt`, `BrowseRoute.kt`, `SettingsRoute.kt`,
   `DetailRouteHost.kt`, `ProviderImportScreens.kt`, `Shell.kt`,
   `SettingsShellComponents.kt`, `SourceOnboardingComponents.kt`,
   `SourceCardComponents.kt`, `AddPlaylistDialog.kt`, `TextEntryDialog.kt`,
   `HeroShowcase.kt`, `MobileV2Components.kt`, `MobileMediaRail.kt`,
   `TvMediaRail.kt`, the activities (`MainActivity.kt`, `MultiviewActivity.kt`,
   `TvHomePlaybackActivity.kt`), `LegacyMyListChannelPublisher.kt`,
   `PlayerEpgPanelController.kt`, `CatalogStatusPolicy.kt`,
   `CatalogPresentationPolicy.kt`, `CatalogLoadCoordinator.kt`,
   `PlaylistStore.kt`, `MainUiState.kt`, and the data/source/net layer files
   (`Http.kt`, `StalkerClient.kt`, `Repository.kt`, `M3uParser.kt`,
   `TmdbClient.kt`, `XtreamClient.kt`, `SubtitleSearchPolicy.kt`,
   `SubtitleResultNamePolicy.kt`, `EpgManager.kt`, `DiagnosticRedactor.kt`,
   `VlcBackend.kt`, `PlaybackEngine.kt`, `CatalogNormalizer.kt`, `PinHasher.kt`)
   as unclassified — each needs its own read-and-classify pass; several of
   these (data/source/net layer, `DiagnosticRedactor.kt`) are likely to be
   mostly protocol/diagnostic data rather than app copy, but confirm per file
   rather than assuming.
2. **Parity inversion and public picker activation.** Only after the full audit,
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
9. Account sync and publisher-server purchase verification are not part of the
   current local-only product or current release plan.

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

> Read `docs/NEXT_CHAT_HANDOFF.md` **section 0 first** — it records the six
> bug-fix commits that closed out the previous session and the follow-ups they
> left open. Then read the rest of this file, `README.md`, `CHANGELOG.md`,
> `docs/MAINTENANCE.md`, `docs/LOCALIZATION_ARCHITECTURE.md` and
> `docs/ARCHITECTURE_REFACTOR_PLAN.md` before acting. Inspect
> `git status --short` and `git log --oneline -10` first; do not trust an old
> completion claim without tracing the active code path.
>
> The live bug-fix batch is **committed and owner-confirmed on mobile and
> Android TV**, through `e26a5aa` — do not re-verify or re-commit any of it.
> The exception is the greeklish pair `2aa060c`/`8ec4658`, which is committed
> and gate-clean but **not device-confirmed**: ask the owner to open a Greek
> series whose list title is in Latin characters and compare two episodes'
> descriptions. If they are still identical, log the generated query and the
> TMDB response — do not adjust the transliteration blindly. Section 0 also
> lists the open follow-ups (whether `fetchEpisodeDescription` is dead weight,
> Xtream's matching missing-description gap, the intentionally retained
> `SeriesLoad` logging, why retrodb.gr was rejected, and `MainViewModel`'s size
> warning); raise those only if the
> owner asks or the work naturally touches them. Read section 0's
> identity-key lesson before writing any code that keys, groups or persists
> episodes.
>
> **Resume the localization audit as the primary task.** Profiles, parental PIN
> and encrypted backup/restore, billing and Premium, legal and privacy,
> diagnostics and crash reporting, export/relay and system notifications, and
> the Library hub are all complete at the code/resource and static-contract
> level and committed; each still awaits the owner's normal Android Studio
> build and phone/TV QA. Continue with the final release-surface
> hardcoded-string audit across every active manifest, Kotlin and XML file — a
> reconnaissance sweep already found 50 files with unclassified Greek literals
> outside Library (file list in the handoff body). Classify each remaining
> literal as app copy, invariant brand/protocol text, provider/user data,
> diagnostic data or developer comment; migrate only app copy. Only after that
> audit plus compilation and phone/TV QA proceed to parity inversion and public
> picker activation.
>
> Continue as a 30-year senior engineer: one careful responsibility at a time,
> small patches only, never rewrite a whole file, avoid giant files, preserve
> public/storage behavior and add focused tests/contracts. Extend focused files
> and feature resources instead of collecting everything in one giant file.
> Every visual/layout/navigation/focus change requires a functional HTML preview
> and my approval before Android implementation; a copy-only resource migration
> or a timing-only fix to an already-approved effect does not. Do not run
> Gradle, compile or build unless I explicitly ask. Never claim runtime success
> from static checks. Run the section 9 static gates and `git diff --check`,
> inspect the diff, record behavior changes in CHANGELOG/docs, update this
> handoff, and commit each cohesive slice separately.
>
> Git writes from the sandboxed agent workspace leave `.git/HEAD.lock` and
> `.git/objects/maintenance.lock` behind, and a stale `HEAD.lock` blocks the
> next command. Grant file deletion for the folder once, then append
> `rm -f .git/HEAD.lock .git/objects/maintenance.lock .git/index.lock` to every
> git write command. You do not need to ask me to delete lock files from
> Windows any more.
