# Prelude+ next-chat handoff

- **Date:** 2026-08-13
- **Workspace:** `C:\Users\konst\AndroidStudioProjects\chatgptiptv`
- **Branch:** `main` · **HEAD:** 1.66.0 exact-preview correction is ready to commit; verify with `git log --oneline -5`
- **Version on disk:** 1.66.0 (`versionCode 138`) · owner-confirmed QA build

> This header has been wrong twice. Run `git log --oneline -5` and
> `git status --short` and believe those, not this line.

This document is the operational source of truth for continuing in a fresh
chat. **Section 0 is written to be read first and on its own** — it says where
things stand, what is still broken, and what to do first. Read the rest of this
file together with `README.md`, `CHANGELOG.md`, `docs/MAINTENANCE.md` and
`docs/ARCHITECTURE_REFACTOR_PLAN.md` before changing code.

`CHANGELOG.md` under `Unreleased` is the authoritative mechanism-level record
for every change; this document does not repeat it. What lives here is the
reasoning, the open questions and the traps that the changelog does not carry.

## 0. START HERE — state, what is open, what to do first

Current session: **2026-08-13**. Slice 2 landed separately as `9a1e787`
(1.64.0), and the first functional slice 3 landed as `f673175` (1.65.0).
The owner correctly rejected its presentation because it treated the approved
HTML as inspiration: it kept a full-screen Apply picker and placed the TV action
in the navigation rail. The uncommitted 1.66.0 correction now treats
`prototypes/full_load_then_choose.html` as the exact contract: compact three-tab
sheet, immediate local changes, per-section quick-action chips and the full
three-section progress surface. The owner confirmed that the QA build compiles;
Gradle was not run by Codex.

**Next action is mobile/TV device validation of 1.66.0-qa**, including the
matrix below. The compile gate has passed, but the exact visuals and focus have
not yet been confirmed.
The 1.63.0 APK was installed and its Settings version confirmed, but the actual
bar wording remains visually unconfirmed on both mobile and TV.

### Status at a glance

| Area | State |
| --- | --- |
| Stalker series episodes load and play | **Owner-confirmed on mobile + TV** |
| Episode switching (next-episode card, episode list) | **Owner-confirmed** |
| Live-transition "flash" | **Owner-confirmed** |
| Stalker catalog load speed (gzip + concurrency) | **Owner-confirmed** |
| Series open time (was 4.5 s, now one request) | **Owner-confirmed** |
| Provider `N/A` / raw air date in the metadata row | **Owner-confirmed fixed** |
| Back navigation returning to the previous section | **Owner-confirmed** |
| One "＋" on Sources, straight to add-source | **Owner-confirmed** |
| Bottom navigation bar focus on Android TV | **Owner-confirmed fixed** |
| Per-episode descriptions | Absent by design — see below |

### The version drift is fixed. Keep it that way.

`versionName`/`versionCode` had been frozen at 1.46.0/115 while 24 commits
accumulated, so an installed build could not be told apart from the source. That
cost a full diagnosis round: a screenshot showed three "unfixed" defects that
were all already fixed in the tree, and only a leftover "#" in a title revealed
the APK was stale. **Bump the version on every change and check the Settings
screen against it before believing any device report.**

### 2026-08-13 — WHERE WE ARE AND WHAT TO DO FIRST

The 2026-08-12 pile is **committed and closed**. `81724e2` landed 1.54.1→1.61.0
as one commit and `71c6d6b` landed 1.62.0. Both passed all six gates. The
"twelve uncommitted releases" panic in the previous handoff was wrong on the
facts: three commits had already landed unnamed, so the pile was eight, not
twelve. That is why the header now tells you to trust `git log` over this file.

**1.63.0 (`versionCode 135`) compiles.** The owner built it and the Settings
screen reports `1.63.0-qa`, so the APK on the device matches the tree. All six
gates pass and `git diff --check` is clean. Four files: `strings_catalog.xml`
(both languages), `CatalogLocalizationResources.kt`, `BrowseRoute.kt`.

**Still unreported: whether the bar itself reads correctly.** Ask for it —
during "Update content" it must read `45% · Λήψη σειρών…` and change section as
the load moves, and on Android TV the same text in the floating overlay with
D-pad movement unchanged. The owner's first report after this build was that the
pre-load category screen was unchanged, which is correct and expected: 1.63.0
does not touch it. Do not read that as a failure of 1.63.0.

**Slices 2+3 are committed; their exact visual correction is 1.66.0.** Its QA
build compiles. The remaining evidence is the mobile/TV matrix, not another
speculative UI change.

#### The pile that is now closed, for reference:

| Version | What |
| --- | --- |
| 1.50.0 | Episode metadata uses the provider's `tmdb_id`; no title search when present |
| 1.51.0 | `CatalogRankingPolicy`; `Channel.addedAt` + `Channel.rating`; Top by rating, New by add-date |
| 1.52.0 | "Top rated movies/series" rails; New rails use `added` |
| 1.53.0 | Same four rails on Android TV via `buildCatalogRailSections` |
| 1.54.0 | Home draws from every loaded section, not just the active one |
| 1.54.1 | Fix: that union leaked into Movies and Series screens |
| 1.54.2 | `CatalogLoad` sample line per section |
| 1.55.0 | New sections land in their default position, not at the bottom |
| 1.56.0 | Home layout editor is **per destination** (Home/Live/Movies/Series) |
| 1.57.0 | Multi-category selection: each ticked category becomes its own rail |
| 1.58.0 | Load summary log; page-cap warning for silent truncation |
| 1.59.0 | Page pool 6 → 12 |
| 1.60.0 | Failure counter; kind filtering on Movies/Series; live hidden on Home by default |
| 1.61.0 | **Page pool reverted to 6** — see below |

### The portal degrades under load. This is the most important finding.

Raising the page pool to 12 halved load time (27.1s → 14.3s) and **corrupted the
data**. The portal did not return errors; it returned stubs. Same film, same
request, twice:

```
6 workers : rating_imdb=6.7  tmdb_id=1284465  description="Na een leven…"  time=123
12 workers: rating_imdb=N/A  tmdb_id=""       description=N/A              time=1
```

Plus 30 silently failed pages on movies. The pool is back at 6. **Never raise it
by reading the timing alone** — the failure counter catches dropped pages but
cannot catch stub rows, which look well-formed until "N/A" reaches the screen.
Some of the original "N · A" report was probably this, not app code.

### PARKED 2026-08-13: the failure counter counts our own cancellations

Deliberately stopped mid-diagnosis to start the new loading model. Resume here.

**What is proven.** 1.62.0 made each lost page state its kind. On the first real
run two pages failed with `SocketException: Software caused connection abort`,
three seconds *after* the section had already logged `0 ΑΠΟΤΥΧΙΕΣ`, on different
threads. The owner confirmed he had closed the app at that moment. So those two
were **our own teardown**, recorded as provider data loss.

**Why the existing guard missed it.** Cancellation is declared in two places that
do not agree: `Http.cancelProviderRequests()` is global (`dispatcher.cancelAll()`,
every instance), while `StalkerClient.requestCancelled` is per-instance and only
set by whoever holds that instance. `MainViewModel.onCleared()` cancels `stalker`,
but `SeriesLoadCoordinator` builds its own temporary clients (`pendingStalker`,
`connectedStalker`) and the load-all-sections path has its own job. Any instance
not told individually sees its sockets die without knowing why. `rethrowIfCancelled`
anticipated this and missed by a hair: it matches OkHttp's `IOException("Canceled")`
by message text, and this arrives as `SocketException` with a different message.

**Why it matters more than it looks.** The 14 failures behind 1.60.0's conclusion
that "the portal drops pages under load" were measured with this same counter. If
some of them were self-inflicted, the premise of 1.59.0/1.60.0/1.61.0 is partly
wrong and three releases have been chasing it. **Not established — do not act on
it as fact.** It has also never been retested on the 271-category source; the
clean runs since were a different source with 6 series categories.

**Proposed fix, not yet approved or written.** Give `Http` a cancellation epoch
incremented by `cancelProviderRequests()`. `StalkerClient` records the epoch when
a load starts and `rethrowIfCancelled` compares epochs instead of matching message
text. Exact rather than time-based, and the same shape as the existing
`SourceGenerationGate`. The alternative — hunting down which instance was not
notified — fixes today's case and lets the next one recur. `Http.kt` is shared
with EPG, TMDB and subtitles, so the change must be additive.

**Do not write a retry for dropped pages** until this is settled. On the evidence
so far the failures are cancellations, and a retry would re-request pages that we
cancelled on purpose.

### THE ACTIVE WORK: full load, then choose. Slices 2+3 written.

The owner approved `prototypes/full_load_then_choose.html` on 2026-08-13, and
answered the two structural questions: **the pre-load category picker goes away
entirely**, and **"everything" includes live** (live still must never be written
to disk). Selection moves to the quick actions of each section, after the load.

Agreed slice order, deliberately smallest-risk first:

| # | Slice | State |
| --- | --- | --- |
| 1α | Loading bar names the section | **1.63.0 installed; visual confirmation still open** |
| 2 | Load everything; publish only complete sections | **committed separately: `9a1e787`, 1.64.0** |
| 3 | Selection in quick actions, names/counts, local filter | **functional base `f673175`; exact approved UI corrected in uncommitted 1.66.0** |
| 1β | Progress stage and three-section surface | **included in 1.66.0; owner validation next** |
| 4 | Disk cache, movies and series only | last |

**The owner reordered on 2026-08-13, deliberately, after seeing 1.63.0 change
nothing he could feel.** He built it, still met the pre-load category screen, and
said so. He is right that 1α is invisible on its own; slices 2 and 3 are the
product. They are taken together because **3 cannot ship without 2** — remove the
pre-load picker while the loader still expects a category list and the app has
nothing to download.

The cost of merging them is stated so it is not discovered later: if the next
build misbehaves, the cause could be the loading flow or the navigation, and
there is no way to tell them apart from one report. Mitigate it by keeping the
two commits separate even though they ship in one build, and by asking the owner
for the `CatalogLoad` summary lines alongside any visual report.

**Why 1β is separate from 1α.** The rich stage text already exists —
`StalkerClient:859` emits `onProgress(pct, "Λήψη σειρών · κατηγορία 45/271")` and
`SourceLoadProgress` carries it intact to the UI, where `CatalogLoadingProgress`
throws it away. But it is a **Greek sentence built inside the data layer**.
Rendering it verbatim would hardcode Greek into a release surface, which rule 16
forbids and the parity inversion would later break. So the stage must become a
typed value (section + done/total) composed into a sentence at the Compose
boundary. `SourceProgressCallback` has roughly thirty call sites across
`StalkerClient`, `XtreamClient` and `Repository`; that migration is mechanical
and compiler-checked, and was kept out of 1.63.0 so a build failure would have
one obvious cause. Note `SourceLoadProgress.stage` is **already rendered raw** in
the mobile and TV Settings source cards, so the debt exists there too and 1β
should clear both.

**Why slice 2 is the real one.** Publishing a section only when it is complete is
the actual fix; the percentage is only the instrument. Every symptom the previous
releases chased — empty rails, the home union, live appearing under Movies,
silent backfill — came from `state.channels` holding whichever section finished
first while the screen rendered anyway. Live finishes in under four seconds and
can open immediately; the rule is not "wait a minute", it is "never half a
section".

**Slice 4 last, and not before the counter is honest.** Caching a load that
silently dropped pages freezes the holes onto disk. See the parked cancellation
item above.

### The original statement of the change of approach

The owner proposed, and it was agreed, that the current progressive model is
wrong. Empty rails, missing sections, the home union, the silent backfill and
"live showing under Movies" were all **one** cause: the app renders before the
data exists, and each mechanism added to guess what to show in the meantime
created the next problem.

**New model: download the whole list once, with honest progress, then let the
user choose what to see.** Three conditions, none optional:

1. **Real progress** — percentage, section, category. `onProgress` already
   reports all of it and the UI throws it away.
2. **Disk cache, or it is worse than today.** Movies and series only. **Never
   live**: their `cmd` is often the final tokenised URL and a cached one is a
   dead stream. VOD and series `cmd` are plain descriptors
   (`{"type":"movie","stream_id":"1813440"}`) with no token — verified from a
   real response. Owner's proposed TTL was left at one day, and "Update content"
   must always bypass the cache.
3. **Category selection moves to after the download**, where the user can see
   names and counts instead of choosing blind. This is the real win, not speed.

### Do this first

0. **Owner builds 1.66.0-qa and confirms that exact version in Settings.** Do not
   infer the APK from screenshots or logs.
0b. Validate one source from a clean open: Live appears only after Live is
   complete; Movies and Series never show partial or cross-section data; the
   overall load continues after Live opens.
0c. On mobile and TV, open Categories from Live, Movies and Series. Confirm names
   and bare counts, zero-count rows grey/non-focusable, the three tabs and the
   immediate visibility change with no provider download. There must be no
   filter field and no Apply/Cancel buttons.
0d. On TV, confirm initial tab focus, Back returns to the Categories quick-action
   chip (not the navigation rail), and D-pad movement elsewhere is unchanged.
0e. Separately capture the still-open 1.63.0 bar evidence on mobile and TV:
   section wording must change with the active download.
0f. If 1.66.0 passes, disk cache remains last. The page-pool
   question stays settled at 6, and the cancellation counter remains parked.
   Decide whether it ships.

1. **Decide what happens to "play a single stream".** `SingleStreamDialog` in
   `ProviderImportScreens.kt` lost its only entry point when `AddMenuSheet` was
   removed. It is deliberately left in the tree, unreferenced, with a comment
   saying so. Either delete it or give it a door (Settings row, or a fifth tab in
   add-source). Do not leave it drifting a third session.
2. **Confirm the `tmdb_id` route on device (1.50.0).** Code-complete, not yet
   device-verified. Open a Greek series whose list title is greeklish and check
   whether episodes now carry their own synopses and stills. One Logcat line on
   `TmdbLookup` settles it: `episodeMeta ... -> tmdbId=N (από τον πάροχο, χωρίς
   αναζήτηση)` means the id was used. If it says "από αναζήτηση τίτλου", the
   provider did not send one for that series and the old path applies — that is
   not a defect, but it means greeklish still matters for those rows.
3. **Then `SectionPublicationCoordinator`** — the owner approved this scope at
   the start of the session and urgent work displaced it. See "MainViewModel" in
   the follow-ups.
4. The localization audit (section 6) comes after those: the final
   release-surface hardcoded-string sweep, 50 files still unclassified.

### Per-episode descriptions: what is true now

Not a bug and not pending. Stalker portals ignore `episode_id` and `season_id`
on `get_ordered_list` — proven by a capture where episodes 25, 28, 33 and 39 all
returned byte-identical responses carrying the **series** description. The extra
request per episode was removed, so an episode with no synopsis of its own now
shows nothing rather than repeating the show's. Real per-episode text can only
come from TMDB, which is what item 2 above unlocks.

### What last session did, grouped

**Stalker series episodes — the original bug, now working.**
`81dcbb1` found that a Stalker season shares one `cmd` and episodes differ only
by the `series=` number sent to `create_link`. `e26a5aa` then fixed episode
switching: `PlaybackQueue.favKey` fell through to `cmd`, so every episode of a
season collapsed onto one identity key — the next-episode card appeared dead and
every episode resumed at the previous one's timestamp. `b94043b` fixed an
unrelated race that discarded in-flight episode fetches.

**Greeklish titles — three fixes, still not working.**
`2aa060c`/`8ec4658` added `GreeklishTitlePolicy` and wired it into TMDB lookup.
`486b2b5` stripped provider decoration (`To Spiti Dipla Sto Potami #`) that was
riding into the search query. `a7a634e` stopped `searchId` accepting the first
Latin result blindly, which had been silently preventing the Greek query from
running at all. `147433a` stopped a failed lookup being cached in memory for the
life of the process. `3ab8810`/`b75eca8` added the `TmdbLookup` diagnostics.

**Back navigation — audited and rebuilt.**
`a389a44`/`b76ee91`. See "Back navigation" below.

**Provider placeholders.** `df4eb4f`. See below.

**Smaller, all owner-confirmed.** `75dd8fa` (test compile fix), `7118354`
(subtitle fallback string), `718965f` (transition flash), `3fa2cd2` (gzip and
per-host concurrency).

### The greeklish problem — the one thing still open

Greek series whose list title is written in Latin characters show the show-level
synopsis on every episode. Greek-titled series work correctly, which isolates
the failure to **TMDB lookup**, not to the display path — all four episode
renderers already read `tmdbEpisode.overview` and fall back to `Channel.plot`.

Three fixes have landed and none has been confirmed to help. What is known:

- The API key works. The owner confirmed it in-app, and a Greek-titled hero
  shows genres, an 8,5 rating, a Greek synopsis and a backdrop.
- The transliteration itself is right: `To Spiti Dipla Sto Potami` produces
  `το σπιτι διπλα στο ποταμι`, whose skeleton equals the real Greek title's.
- What is **not** known is whether TMDB actually resolves that query. The
  skeleton comparison guarantees a wrong show is rejected; it guarantees nothing
  about the right one being found. No real title from the owner's list was seen
  until late, and the one that was (`... #`) exposed a defect immediately.

**Read the `TmdbLookup` capture before changing anything:**

| Log line | Meaning | Fix direction |
| --- | --- | --- |
| `ΔΕΝ ΥΠΑΡΧΕΙ TMDB API KEY` | no key configured | settings, not code |
| `δεν θεωρήθηκε greeklish` | `looksGreeklish` rejected the title | widen detection |
| `ερώτημα «…»` then `0 αποτελέσματα` | TMDB found nothing for the Greek query | improve `toGreek` — **ask for ten real titles first** |
| results listed, `ταίριασμα=0` | TMDB found the show, comparison rejected it | loosen `isSameTitle` / fix the skeleton |
| `tmdbId=` non-zero, `0 επεισόδια` | series resolved, season number wrong | the caller's season index — **not** this policy |

The last row matters: nothing in the greeklish work touches the season number,
which comes from a Stalker season label, free-form provider text.

**retrodb.gr was investigated and deliberately not adopted.** Its content is
client-rendered with no reachable JSON endpoint, a second database does not fix
a *matching* problem, and a new external service triggers the privacy and Play
Data Safety rules in `docs/MAINTENANCE.md`. Revisit only if a real gap remains
for series TMDB does not carry at all.

### Codebase traps — read before writing code

**1. Two identity functions that disagree, both persisted.**

| Function | Basis | Used by |
| --- | --- | --- |
| `PlaybackQueue.favKey` | `url`, then `cmd`, then `seriesId` — **never `streamId`** | favorites, recents, **resume position**, subtitle requests, `LibraryPolicy.unique`, `CatalogPolicy.key`, `libraryKey` |
| `PlaybackHistoryStore.historyMatchKey` | `streamId`, then `seriesId`, then `chId`, then metadata | history reconciliation only |

`e26a5aa` existed only because an earlier fix corrected one and not the other.
**A Stalker episode is identified by its `series=` number, not by `cmd` or
`url`.** Anything that keys, groups, de-duplicates or persists episodes must go
through `favKey`. Changing `favKey` is a stored-data contract change.

The same trap recurred in `df4eb4f`: `year` and `duration` feed the fallback
identity keys in `CatalogNormalizer` and the persisted `localSeriesId`, so the
placeholder cleanup deliberately leaves them alone. Before normalising any
field, check whether it appears in an identity function.

**2. `BackHandler` declaration order is reverse priority.** Compose gives BACK to
the *last active* handler. The section handler in `BrowseRoute` is declared
*before* the overlay handler on purpose, and its `enabled` condition is
additionally mutually exclusive — the guarantee is duplicated because a silent
reordering would change behaviour with no test failing.

**3. `MainActivity:266` is `enabled = true` unconditionally** and acts as the
final catch-all: anything no one claims becomes the "change source?" dialog.
A screen that forgets a handler shows up as the wrong dialog, not a dead button.

**4. TV screens carry no `BackHandler` of their own.** Details, library, search
and EPG all rely on the `when` in `BrowseRoute`, making its ordering the single
source of truth for TV back behaviour.

**5. Diagnostic logging is intentional, not leftover.** `SeriesLoad` in
`StalkerClient`/`MainViewModel` and `TmdbLookup` in `TmdbClient`. Raw provider
JSON is what solved the Stalker bug after three rounds of wrong guesses. Never
log a URL from `TmdbClient`: every TMDB URL carries the API key.

**6. Evidence before theory.** This project has now cost several rounds to the
same mistake — reasoning about a subsystem before confirming it was reachable,
or before seeing one real input. Two screenshots and one real title resolved
more than a day of inference. Ask for a real sample first.

### Still-open follow-ups

- **`fetchEpisodeDescription` — SETTLED AND REMOVED.** The capture asked for
  arrived: requests for episodes 25, 28, 33, 39 and the rest returned identical
  responses carrying the series description, so the portal ignores `episode_id`
  and `season_id` at that depth. 81 requests, 4.5 seconds, no information. Gone.
  Per-episode text now comes only from TMDB.
- **Player start latency — measured, buffering is NOT the cause. Parked.** The
  owner switched the buffer profile from Normal (`forPlaybackMs = 2_500`) to Low
  (`1_500`) and reported no noticeable difference, which rules out the initial
  buffer as the dominant cost and means retuning `BufferPolicy` would buy little.
  What remains on the path, in order: the `create_link` round trip (unavoidable
  for VOD/episodes — the catalog `cmd` carries an expiring token and returns 404
  without it); a full `connect()` handshake plus `get_profile` whenever the
  portal session has lapsed, and a **second** full `connectFresh()` retry inside
  `resolvePlayableUrl` when `create_link` comes back empty; and
  `setAllowedVideoJoiningTimeMs(0)`, which is a deliberate lip-sync decision that
  costs start time and must not be reverted without asking.
  Next step when this is picked up: instrument tap -> first frame before changing
  anything, then look at keeping the portal session warm. Note that removing
  `fetchEpisodeDescription` freed up to 81 concurrent requests that were
  competing with `create_link` on the same portal, so re-measure first.
- **`tmdb_id` is now used for EPISODE metadata, but not for the series itself.**
  `TmdbClient.fetch` — the call behind the hero poster, backdrop, rating and
  show-level synopsis — still resolves by title. It works today, which is why it
  was left alone, but it is the last place a greeklish title can still pick the
  wrong show. Threading the id into `fetch` is a small, obvious follow-up; do it
  once the episode route is device-confirmed, so the two are not diagnosed
  together.
- **Xtream and M3U do not populate `tmdbId`.** Only `StalkerClient` reads it.
  Xtream's series info payload may well carry one; nobody has looked. Check a
  real response before adding a parser.
- **`seriesId` doubling — SETTLED, NOT A DEFECT. Do not "fix" it.** The suspicion
  was that the app built `movie_id=42504:42504` itself. The 2026-08-12 capture
  shows the portal's own catalog row arriving as `"id":"58875:58875"`, so the app
  is passing through exactly what it was given. There is nothing to correct, and
  `seriesId` participates in `PlaybackQueue.favKey` — changing it would be a
  stored-data contract change for no gain. This also means it is **not** the
  reason the season/episode filters were ignored; that was settled separately
  (the portal ignores `episode_id`/`season_id` at that depth).
- **`XtreamClient.seriesEpisodes` has the same missing-description gap** and was
  left untouched, since the reported bug was Stalker-specific. Ask first.
- **`MainViewModel.kt` is 1860 lines** and trips the architecture audit's known
  size warning. Extraction is tracked in `docs/ARCHITECTURE_REFACTOR_PLAN.md`.

### Back navigation

The root cause was that the current section was a single variable, not a stack,
so "back" between sections did not exist — the controls that looked like back
were fixed destinations. Three defects: the Live arrow always went Home; at the
root of Live the device button fell through to the change-source dialog while
the arrow went Home (same gesture, two outcomes); and the legacy top bar's arrow
was wired straight to the change-source dialog.

`SectionNavigationPolicy` now owns the history. Revisiting a section collapses
to its existing entry rather than pushing a duplicate, so Home → Movies →
Series → Movies leaves two entries and one back reaches Home. Changes that are
not user navigation (source load, state restore) replace the top entry instead
of writing history. At the root the policy reports "not my decision" and the
change-source confirmation is unchanged, as the owner asked.

### Verification method, and its limits

Gradle was **not** run at any point — the owner builds in Android Studio. Where
a change could not be built, its logic was verified by reproducing the Kotlin
outside the build and executing the same assertions: 24 Greek/greeklish title
pairs across four spelling conventions, the episode-identity collapse, the
navigation stack, and the placeholder cleanup all pass.

That is static evidence about internal consistency. It says nothing about how
TMDB responds, and it is exactly why the greeklish item is still open.

### Environment

- **`.git` locks are solved.** Git writes from the sandbox leave `HEAD.lock` and
  `objects/maintenance.lock` behind and a stale `HEAD.lock` blocks the next
  command. Grant file deletion for the folder once, then append
  `rm -f .git/HEAD.lock .git/objects/maintenance.lock .git/index.lock` to every
  git write. The old "ask the owner to delete from Windows" workflow is obsolete,
  as is avoiding chained git commands. The `tmp_obj_*` unlink warnings are
  harmless.
- **Line endings.** `MobilePlaybackOverlay.kt` was once rewritten as CRLF against
  an LF repo, producing 919 phantom `trailing whitespace` errors. There is no
  `.gitattributes`. If a file shows as fully modified, check line endings first.
- **Gates** are listed in section 9. Two standing warnings are expected:
  `MainViewModel` size, and the documented global-cleartext exception.

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

> **Current state, environment facts and the `.git` lock workflow now live in
> section 0 and are not repeated here.** Everything below this line is older
> history kept for context. Where it disagrees with section 0, section 0 wins,
> and `git log` wins over both.

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

> The **most recent** 24 commits are summarised in section 0, grouped by theme.
> The table below is the older history that precedes them.

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

**The mounted workspace is too slow for these scripts** — running them in place
times out. Copy the tree to local disk first and run there; it takes seconds:

```bash
rm -rf /tmp/aud && mkdir -p /tmp/aud
tar cf - --exclude=.git --exclude=build --exclude=.gradle . | (cd /tmp/aud && tar xf -)
cd /tmp/aud && for s in architecture_audit compatibility_contracts \
  deep_validation_audit risk_inventory; do python3 scripts/$s.py | tail -3; done
```

**PowerShell has no heredoc.** `git commit -F- <<'MSG'` fails and scatters the
message across the shell as commands. Write the message to
`.git/COMMIT_MSG.txt` with the file tools, then `git commit -F .git/COMMIT_MSG.txt`
and delete it. `.git/` is never tracked.

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

> Read `docs/NEXT_CHAT_HANDOFF.md` **section 0 first and in full** — it states
> the current state, what is still broken and what to do first. Then read the
> rest of that file, `README.md`, `CHANGELOG.md`, `docs/MAINTENANCE.md`,
> `docs/LOCALIZATION_ARCHITECTURE.md` and `docs/ARCHITECTURE_REFACTOR_PLAN.md`
> before acting. Run `git status --short` and `git log --oneline -12` first and
> trust those over anything written in a document.
>
> **ΞΕΚΙΝΑ ΑΠΟ ΤΙΣ ΦΕΤΕΣ 2+3 — αυτό περιμένω να δω.** Όταν ανοίγω πηγή να
> κατεβαίνουν ΟΛΑ (και τα ζωντανά), με πραγματική πρόοδο, και η επιλογή
> κατηγοριών ΠΡΙΝ τη λήψη να καταργηθεί εντελώς: μεταφέρεται στα quick actions
> κάθε ενότητας, ΜΕΤΑ τη λήψη, με ονόματα και πλήθη μπροστά μου. Εγκεκριμένο
> preview: `prototypes/full_load_then_choose.html`. Το 3 δεν γίνεται χωρίς το 2 —
> αν φύγει ο επιλογέας ενώ ο loader περιμένει λίστα κατηγοριών, δεν έχει τι να
> κατεβάσει. Πάνε μαζί στο ίδιο build αλλά σε **ξεχωριστά commits**.
>
> **Η φέτα 2 είναι η ουσιαστική, όχι το ποσοστό:** μια ενότητα δημοσιεύεται μόνο
> όταν ολοκληρωθεί ολόκληρη. Όλα τα συμπτώματα που κυνηγήσαμε επί τρεις εκδόσεις —
> άδειες ράγες, η ένωση της αρχικής, ζωντανά κάτω από τις Ταινίες — ήταν το ίδιο
> πράγμα: η `state.channels` κρατούσε όποια ενότητα πρόλαβε πρώτη. Τα ζωντανά
> τελειώνουν σε 4 δευτερόλεπτα και ανοίγουν αμέσως· ο κανόνας δεν είναι «περίμενε
> ένα λεπτό», είναι «ποτέ μισή ενότητα».
>
> **Μετά από αυτά, η φέτα 1β:** τυποποίηση του stage ώστε η μπάρα να δείχνει
> «κατηγορία 45 από 271». Το κείμενο υπάρχει ήδη στον `StalkerClient:859` αλλά
> είναι **ελληνική πρόταση φτιαγμένη στο data layer** — δεν τυπώνεται ως έχει,
> γίνεται τυποποιημένη τιμή και συντίθεται στο Compose. ~30 σημεία κλήσης,
> μηχανικά, ΜΟΝΑ ΤΟΥΣ σε δικό τους commit. Το ίδιο `stage` εμφανίζεται ήδη ωμό
> και στις κάρτες πηγών των Ρυθμίσεων, σε κινητό και τηλεόραση — καθάρισέ τα μαζί.
>
> **Το 1.63.0 (135) χτίζει** — το επιβεβαίωσα, οι Ρυθμίσεις γράφουν `1.63.0-qa`.
> Αλλάζει ΜΟΝΟ το κείμενο της μπάρας φόρτωσης («45% · Λήψη σειρών…»), όχι την
> οθόνη επιλογής. Δεν έχω επιβεβαιώσει ακόμα αν η μπάρα διαβάζεται σωστά σε
> κινητό και τηλεόραση — ζήτα μου το.
>
> **Παρκαρισμένο, με στοιχεία, στην ενότητα 0:** ο μετρητής αποτυχιών χρεώνει στον
> πάροχο τις ΔΙΚΕΣ ΜΑΣ ακυρώσεις. Αποδείχθηκε: δύο `SocketException` καταγράφηκαν
> τρία δευτερόλεπτα μετά το «0 ΑΠΟΤΥΧΙΕΣ», επειδή έκλεισα την εφαρμογή.
> **Μη γράψεις retry για χαμένες σελίδες** πριν λυθεί αυτό, και μην εμπιστευτείς
> τον αριθμό «14 αποτυχίες» του 1.60.0 ως απόδειξη ότι μας κόβει το portal.
>
> **Λυμένα, μην τα ξανανοίξεις:** το pagePool=6 δικαιώθηκε στη συσκευή (η ίδια
> ταινία γύρισε με `rating_imdb=6.7`, `tmdb_id=1284465`, περιγραφή και αφίσα). Το
> διπλό `seriesId` (`58875:58875`) το στέλνει έτσι ο πάροχος — δεν είναι δικό μας
> bug και συμμετέχει στο `favKey`, οπότε είναι αποθηκευμένο δεδομένο.
>
> **Ακόμα ανοιχτό:** οι περιγραφές επεισοδίων για ελληνικές σειρές με λατινικό
> τίτλο. Αν ξαναπροκύψει, ζήτα μου Logcat σε `TmdbLookup` και χρησιμοποίησε τον
> πίνακα της ενότητας 0 — οι τρεις αιτίες θέλουν αντίθετες διορθώσεις. Μετά από
> όλα αυτά έρχεται ο τελικός έλεγχος καρφωμένων αλφαριθμητικών (50 αρχεία).
>
> **Πριν γράψεις κώδικα, διάβασε τις «Codebase traps» της ενότητας 0.** Δύο
> συναρτήσεις ταυτότητας που διαφωνούν και έχουν ήδη κοστίσει δύο bugs· η σειρά
> δήλωσης των `BackHandler` είναι ανάποδη προτεραιότητα· το logging σε `SeriesLoad`
> και `TmdbLookup` είναι σκόπιμο.
>
> Δούλεψε ως 30 χρόνια senior: μία ευθύνη τη φορά, μικρά patches, ποτέ ξαναγράψιμο
> ολόκληρου αρχείου, στοχευμένα τεστ. **Θέλω να έχεις άποψη** — αν δεις ότι
> διορθώνουμε συμπτώματα, πες το και πρότεινε αλλαγή προσέγγισης πριν γράψεις.
> Απόδειξη πριν από θεωρία: ζήτα μου log ή screenshot πριν συμπεράνεις για
> υποσύστημα που δεν έχεις επιβεβαιώσει ότι εκτελείται. Αλλαγές σε εμφάνιση,
> διάταξη, πλοήγηση ή focus θέλουν HTML preview στο `prototypes/` και έγκρισή μου·
> μεταφορές κειμένου σε resources δεν θέλουν. **Η τηλεόραση θέλει σωστό focus σε
> κάθε αλλαγή — μη το ξεχάσεις ποτέ.** Αύξησε έκδοση και γράψε CHANGELOG σε ΚΑΘΕ
> αλλαγή. Μην τρέχεις Gradle· χτίζω εγώ. Ποτέ μην ισχυριστείς επιτυχία από στατικό
> έλεγχο. Μίλα μου στα ελληνικά.
>
> Πρακτικά, για να μη χαθεί χρόνος: το mounted workspace είναι πολύ αργό για τα
> scripts — αντίγραψέ το σε τοπικό δίσκο και τρέξε εκεί (εντολή στην ενότητα 9).
> Το PowerShell ΔΕΝ έχει heredoc: γράψε το commit message σε
> `.git/COMMIT_MSG.txt` και κάνε `git commit -F .git/COMMIT_MSG.txt`. Τα git
> writes από το sandbox αφήνουν `.git/HEAD.lock` — πρόσθεσε
> `rm -f .git/HEAD.lock .git/objects/maintenance.lock .git/index.lock` σε κάθε
> εντολή εγγραφής.
