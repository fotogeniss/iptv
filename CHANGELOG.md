# Changelog

All notable user-visible changes are recorded here. The detailed historical
implementation notes are preserved in `docs/archive/changelog`.

## Unreleased

- CI: disabled Gradle wrapper validation, which had been failing every run
  within seconds and never reaching the project's own checks or build. The
  repository carries a hand-written `gradlew` and a 6.8KB bootstrap
  `gradle-wrapper.jar` added in 1.40.8 because the official files could not be
  downloaded at the time; those can never match the published Gradle checksums
  that `gradle/actions/setup-gradle` verifies by default. Restoring the real
  wrapper with `gradle wrapper --gradle-version 8.9` is the proper fix and lets
  the validation be turned back on. No application code is affected.

## 1.77.0 - versionCode 149

- Removed the three to four second black gap before live channels reappeared in
  the mini player. The 1.75.0 fix told libVLC about the smaller surface by
  tearing the video output down and rebuilding it, and a live MPEG-TS stream
  cannot resume until the next keyframe, so the strip sat black for a whole
  group of pictures. Recorded VOD did not show it because it resumes from the
  current position.
- The surface change is now reported with a window-size update on the live
  output instead, which does not touch the decoder, so the picture continues
  without a break. The full re-attach is kept only as a safety net: if libVLC
  reports no active video output shortly after the resize, the old path runs so
  the strip can never be left permanently black.
- No layout, size, wording, gesture, icon or TV behavior changed. Gradle was not
  run; the owner QA build is pending.

## 1.76.0 - versionCode 148

- Added a temporary QA-only readout inside the mini player's video area,
  showing the active engine, the frame counter for the current surface, the
  measured surface size, the identity of the attached surface and the reported
  aspect ratio. It is gated on the QA build flag and never reaches a public
  release.
- Its purpose is to end the guessing about the strip playing sound without
  picture. Git history cannot help: the mini player, the engine's surface
  attach/detach and the surface composable are byte-identical to this
  repository's root commit, and the earlier uncommitted work never touched the
  player. A photograph of the strip now answers the question that three builds
  of reasoning could not.
- The frame counter is the decisive value. It increments each time the current
  surface renders its first frame, so `f=0` means no frame ever reached the
  strip's surface, while a non-zero count with a black strip means frames
  arrive and something above them is hiding the picture. Those two answers need
  opposite fixes.
- Nothing else changed, and the readout will be removed once the cause is
  found. Gradle was not run; the owner QA build is pending.

## 1.75.0 - versionCode 147

- Fixed the mini player still losing the picture for streams played by libVLC.
  Bare MPEG-TS sources, which is most provider live TV and a large part of
  provider VOD, are routed to libVLC by policy, so the defect survived the
  1.74.0 shared-surface work while ExoPlayer content played correctly. That is
  why the failure looked intermittent: it followed the stream, not the timing.
- libVLC configures its video output for the surface dimensions it sees at
  attach time, and the attach call deliberately ignores a surface it is already
  bound to. Once the same surface was shared between the full screen and the
  strip, its identity stopped changing, so libVLC was never told the surface had
  dropped from full screen to 121x68dp and kept drawing to geometry that no
  longer existed.
- The libVLC surface now re-attaches when its measured size changes materially,
  with a threshold so ordinary reflows do not rebuild the output. ExoPlayer is
  unaffected; it handles the same resize through the shared surface without a
  re-attach.
- No layout, size, wording, gesture, icon or TV behavior changed. Gradle was not
  run; the owner QA build is pending.

## 1.74.0 - versionCode 146

- The collapsed mini player now keeps the picture because it no longer builds a
  second video surface. The full-screen layout and the strip previously each
  created their own TextureView, so collapsing destroyed one and created the
  other on top of a player that was already running; which of the two ended up
  owning the video output depended on the order Compose applies insertions and
  disposals, on the SurfaceTexture lifecycle and on whether the codec accepts a
  live output swap. When it lost, audio continued and the picture disappeared.
- The surface is now a single `movableContentOf` owned by the overlay and
  handed to whichever layout is showing. The same nodes move between parents,
  so the TextureView is never destroyed and the player's video output never
  changes. The 1.71.0 libVLC detach guard remains correct and still applies to
  the engine's own teardown.
- Added a `PlayerSurface` diagnostic log at the attach/detach boundary,
  recording the surface identity, the active engine and whether a detach was
  applied or ignored. The previous round cost a build cycle to a plausible but
  unproven cause; this makes the next report decisive.
- No layout, size, wording, gesture, icon or TV behavior changed. The strip is
  the same 121x68dp video, the same controls and the same position. Gradle was
  not run; the owner QA build is pending.

## 1.73.0 - versionCode 145

- The mobile player's progress bar is thinner at rest and reacts slightly to
  touch: the line goes from 1.5dp to 3dp and the handle from 7dp to 11dp over
  130ms while a finger is down, then returns. The change follows the finger,
  not the result, so simply holding the bar shows the response even when the
  position is not moved.
- The 26dp touch area is deliberately unchanged. A thinner line that was also
  harder to grab would be a worse control, not a nicer one.
- Nothing else in the player moved: colors, spacing, times, buttons, gestures
  and the Android TV scrubber are untouched, and no arrow affordance was added
  anywhere.
- The frozen architecture contract for the mobile scrubber was updated to the
  new animated values; it still enforces a slim line and the preserved touch
  target. Follows `prototypes/player_scrubber_slim.html`, approved by the owner
  before this edit. Gradle was not run; the owner QA build is pending.

## 1.72.0 - versionCode 144

- The phone's Back button now collapses the mobile player into the mini strip
  instead of ending playback, so the most common exit gesture no longer kills
  the stream. Pulling the page down still does the same thing, and closing
  stays an explicit action through the strip's close button. In fullscreen,
  Back still leaves fullscreen first, and while the strip is docked Back still
  belongs to the page underneath.
- Follows `prototypes/mini_player_back_button.html`, approved by the owner
  before this edit. No layout, size, wording, icon or TV behavior changed.
  Gradle was not run; the owner QA build is pending.

## 1.71.0 - versionCode 143

- Fixed the collapsed mini player playing sound with no picture whenever the
  libVLC engine was in use. Collapsing composes the small surface before the
  full-screen one leaves, so the departing full-screen surface called an
  unconditional libVLC detach and tore down the strip's freshly attached video
  layout; audio kept running because only the video output was removed.
- libVLC detach now verifies that the layout being released is still the active
  one, which is the identity guard the ExoPlayer surface path already had. The
  two engines now behave the same way at the same boundary.
- Nothing about the strip's layout, size, controls, wording or gestures
  changed. Gradle was not run; the owner QA build and an on-device check that
  the mini player shows moving picture are pending.

## 1.70.0 - versionCode 142

- Fixed the top hero slider on Home, Movies and Series showing a single item.
  The hero was fed the Explore/category-filtered list, so selecting a category
  that holds one title left the pager with one page and it could not move. The
  hero now takes its up-to-six candidates from the whole current destination,
  while the Explore/category choice keeps filtering only the content below the
  hero.
- The hero advances on its own every five seconds while the user is not
  touching it. Touching cancels the timer and releasing restarts it, so the
  next automatic change arrives five seconds after the last swipe. Finger swipe
  left/right remains the only manual control; no arrow buttons were added.
- Layout, height, dots, buttons, wording, typography, colors, navigation and TV
  focus are unchanged. Follows `prototypes/hero_carousel_restoration.html`,
  approved by the owner before this edit. Gradle was not run; owner QA build and
  the on-device slider check are pending.

## 1.69.0 - versionCode 141

- Removed the six deprecation warnings reported by the owner's successful
  1.68.0 QA compilation. The relay notification now uses the channel-aware
  constructor on every supported Android version, since the app's minimum SDK
  is already Android 8.
- Catalog loading now requests Android's low-latency Wi-Fi mode on Android 14+
  and retains the former high-performance mode only on older releases where it
  is still the appropriate API.
- Kept the four accepted Send, Help and Sort icons byte-for-byte unchanged.
  Their narrowly scoped deprecation suppressions deliberately avoid switching
  to auto-mirrored assets, which would be a visual RTL change requiring a
  separately approved HTML preview.
- No layout, wording, navigation, gesture or TV focus changed. Gradle was not
  run; owner QA compilation is pending.

## 1.68.0 - versionCode 140

- Reverted the unintended replacement of the established Movies, Series and
  Live screen chrome. The 1.66.0 full-width quick-action header, extra content
  padding and downloaded-count sentence changed the whole screen even though
  the approved HTML described the loading/category flow, not a redesign of the
  existing catalog UI.
- Restored the original mobile header, Live category control, TV navigation and
  original rail layout/gesture surface exactly. Category selection still opens
  after download from the controls that already existed, and the compact
  three-tab category sheet and complete-load progress screen remain intact.
- Removed the downloaded item/category sentence from normal catalog screens.
  Counts remain inside the category sheet, where they are needed to make a
  visibility choice, and on the active download progress screen.
- This release includes the 1.67.0 foreground-service, CPU wake-lock and Wi-Fi
  lock protection for screen-off downloads. Gradle was not run; owner QA build,
  rail interaction check and screen-off test are pending.
- Recorded the exact-scope authorization boundary in root `AGENTS.md`, the
  maintenance guide and the next-session handoff. The documentation gate now
  fails if that rule is removed: previews authorize only their agreed element,
  and any adjacent UI/navigation/focus/gesture change must be approved first.
- Strengthened that boundary with the owner's explicit visual-change sequence:
  every visual change, however small, requires its exact HTML preview and a new
  explicit owner "OK" before production code may be edited. The documentation
  gate checks that this no-exceptions sequence remains recorded.

## 1.67.0 - versionCode 139

- Fixed complete Stalker/Xtream source loads being abandoned when a phone's
  screen turned off and the OS cached the unprotected app process. The complete
  Live/Movies/Series job now holds the same foreground-service protection that
  already kept large M3U downloads alive.
- The foreground service now holds a bounded partial CPU wake lock and a
  high-performance Wi-Fi lock while protected catalog work is active, then
  releases both with the service on success, failure or cancellation.
  Concurrent/nested downloads share a reference-counted service lease; each
  lease can close only once.
- This deliberately does not raise Stalker's proven-safe `pagePool=6`, run the
  three sections concurrently or publish partial sections. Those would trade
  correctness for an unmeasured speed claim. The next throughput decision must
  use the existing three `CatalogLoad` summary lines from a real full load.
- No app layout, navigation or focus changed. Gradle was not run; owner QA build
  and the screen-off test are pending.

## 1.66.0 - versionCode 138

- Corrected the 1.65.0 category UI to follow the approved
  `prototypes/full_load_then_choose.html` as an exact contract. The picker is
  now a compact dark sheet with Series, Movies and Live tabs, category names
  and bare item counts, disabled zero-count rows, and only the `All` / `None`
  bulk actions. The old full-screen picker, filter field, Cancel button and
  Apply button are gone.
- Category changes apply immediately to the complete in-memory snapshot while
  the sheet remains open. Switching tabs edits all three downloaded sections
  without provider I/O or another download.
- Categories moved out of the Android TV navigation rail and the standalone
  Mobile Live icon. Live, Movies and Series now expose the approved horizontal
  quick-action chips for Categories, Sort, Favorites and Refresh, followed by
  the downloaded item/category summary. Android TV returns focus to the
  Categories chip after the sheet closes; empty category rows cannot receive
  focus.
- A full source refresh now uses the approved progress screen: overall percent
  and stage, one status row for each section, completed item counts, waiting
  states and the explanation that complete sections can open immediately. Back
  dismisses the progress surface while the remaining sections continue in the
  background.
- Greek and English QA resources cover every new release string. The owner
  confirmed that the 1.66.0 QA build compiles; exact mobile/TV visual and focus
  verification is still pending. The older 1.63.0 bar wording also remains
  visually unconfirmed.

## 1.65.0 - versionCode 137

- Category selection now happens after the complete Live TV, Movies and Series
  download. The picker reads provider category ids and names from the completed
  in-memory snapshot, shows the item count beside every category and keeps
  provider categories with zero items visible but disabled.
- Applying a category selection is a local visibility change. It does not
  invalidate the full section, contact the provider or download the selected
  categories again; reopening the section and Home uses the same persisted
  visibility choice over the complete session snapshot.
- Removed the pre-download load-mode and refresh-mode dialogs from active state
  and routing. Refresh now fetches the complete source again, while category
  choice remains independent.
- Added the post-download category quick action to Mobile Live and to the
  existing Android TV navigation rail. The full-screen picker retains explicit
  initial focus, right-arrow focus to Apply, non-focusable zero-count rows and
  returns to the still-composed TV rail item when closed.
- Greek and English QA resources cover the new names, summaries, counts and
  actions. The interaction follows the approved
  `prototypes/full_load_then_choose.html` preview.
- Build and device verification are pending. The separate 1.63.0 progress-bar
  wording also still awaits visual confirmation on mobile and TV.

## 1.64.0 - versionCode 136

- Opening a source now downloads Live TV, movies and series in one sequential
  source load, including live channels, instead of asking which section or
  provider categories to fetch first.
- A catalog section is published only after that whole section has completed,
  normalized and entered the bounded session cache. Provider partial batches no
  longer reach `state.channels`, so a Movies or Series screen cannot render a
  half-built section or inherit whichever content type happened to finish a
  batch first. A completed Live section can still open immediately while the
  remaining sections continue.
- Removed the Home background backfill trigger. The complete source load already
  fills every section, and a second silent pass would duplicate provider work.
- The installed `1.63.0-qa` build was confirmed before this change; visual
  confirmation of the 1.63.0 progress-bar wording on mobile and TV remains open.

## 1.63.0 - versionCode 135

- **The loading bar now says which section it is downloading.** It read
  "Downloading from source… 45%" regardless of whether that 45% belonged to live
  channels, movies or series. During a full load that is three different waits
  wearing the same label, and the number appears to jump backwards when one
  section finishes and the next starts from zero. It now reads "45% ·
  Downloading series…".
  - The section identity was already there. `SourceLoadProgress` has carried
    `contentType` all along and the bar ignored it.
  - It is read from the **active progress entry**, not from the screen the user
    is standing on: during a full load you may be looking at Movies while Series
    is what is actually downloading, and a bar that names the screen instead of
    the work would be a more confident kind of wrong.
  - Three separate strings rather than one with a placeholder, because Greek
    inflects the section name and a shared sentence would be ungrammatical in at
    least one of the three.
  - An unrecognised `contentType` falls back to the general wording instead of
    printing the raw key, so a future section cannot leak "vod2" onto the screen.
- First step of the agreed new model, approved in
  `prototypes/full_load_then_choose.html`. Category counts ("category 45 of 271")
  come next: that text is currently built as a Greek sentence inside
  `StalkerClient`, and showing it would hardcode Greek into a release surface, so
  the stage has to become a typed value first. Kept separate on purpose — it
  touches around thirty call sites and should not share a build with a UI change.
- No change to Android TV focus or navigation: the progress bar is a floating
  overlay with no focusable modifier on either surface.

## 1.62.0 - versionCode 134

- **A failed page now records why it failed.** The counter added in 1.60.0 said
  *how many* pages were lost and never *why*, and the two possible reasons need
  opposite fixes: an exception is a transient network failure that a bounded
  retry would rescue, while a well-formed response with no `js.data` means the
  portal answered without data — the same degradation that made twelve workers
  unusable, where retrying only adds pressure. Each failure now logs its kind,
  capped at three samples per section so fourteen stack traces cannot bury the
  line you are trying to read.
- **Found a larger hole while looking: a lost first page was never counted at
  all.** `fetchAllPages` returns an empty list when the first request fails or
  the response carries no `js`/`data`, and that path incremented nothing. The
  parallel counter only ever watched pages 2..N. With 271 categories in the
  series section, one failed first page silently deletes an entire category, and
  the number of missing items cannot even be estimated — the response that would
  have stated it is the one that did not arrive. Now counted and reported
  separately from page failures, because the two losses are not the same size.
- No retry, no change to the page pool, no change to what the user sees. This
  release exists to answer one question: which of the two failures the owner's
  portal is actually producing under the series load — 14 pages of 597, at 20
  requests per second, where live at 7 and movies at 12 lose nothing.

## 1.61.0 - versionCode 133

- **Reverted the page pool from 12 to 6.** The speed gain was real — 27.1s to
  14.3s — and it was not worth what it cost.
  Under twelve workers the portal did not return errors. It returned **stubs**.
  The same film, `id 1813440`, came back twice with the same request and
  different content:
  ```
  6 workers : rating_imdb=6.7  tmdb_id=1284465  description="Na een leven…"  time=123
  12 workers: rating_imdb=N/A  tmdb_id=""       description=N/A              time=1
  ```
  Alongside 30 silently failed pages on movies and 2-3 on live. The speed was
  being bought with ratings, synopses, posters and TMDB ids — precisely the
  fields the previous releases existed to get right. It is likely that some of
  the original "N · A" report was this same degradation rather than app code.
  - The failure counter added in 1.60.0 catches dropped pages but **cannot**
    catch this: a stub row looks perfectly well-formed until it reaches the
    screen. The comment now says so, so nobody raises the pool again by reading
    only the timing.
- Recorded that the owner wants a stated opinion and a proposed approach, not
  only execution of the request.
- **Fixed a stale architecture contract that failed on correct code.** The check
  "MainViewModel delegates catalog group ordering and visibility" required
  *exactly two* call sites of `CatalogPresentationPolicy`. A third one was added
  in 1.54.0 — `visibleHomeChannels()`, which passes the home's union of sections
  through the same locked-group filter so parental control cannot be bypassed by
  a screen that merely wanted more data. Counting call sites punished the right
  change. The rule the contract actually exists to protect — that the ViewModel
  does not reimplement filtering itself — is carried by its other two markers,
  and those still hold. Now `>= 2`, with the reason written next to it. No app
  code changed, so the version is unchanged.

## 1.60.0 - versionCode 132

- **Counting silently dropped pages.** The concurrency increase in 1.59.0 halved
  the time — 27.1s to 14.3s, exactly as predicted — but the same source returned
  **1,981 items instead of 2,304** from the same 14 categories. Every page task
  catches its own failure and returns an empty list so one hiccup cannot fail the
  whole load; the cost is that the loss is invisible. The summary now reports
  failed pages and warns with an estimate of the missing items.
  Until that number is seen on a real load, treat 1.59.0's page pool of 12 as
  **unproven**: if failures are non-zero and consistent, the portal is rejecting
  the extra pressure and the pool goes back to 6. Speed is not worth a catalog
  with holes in it.
- Fixed the Movies and Series screens showing **live channels** while a catalog
  was still loading. Inside its own destination the screen took the channel list
  as-is, trusting that it contained only that kind. That is not true mid-load:
  `state.channels` holds whichever section finished first, so if Live won the
  race, Movies and Series filled with channels. Both now filter by kind always;
  the assumption cost one pass over a list and was wrong.
- Live rows no longer appear on the **home** screen by default. The home is a
  library of films and series — a channel among posters is an empty tile with no
  synopsis and no duration, and Live already has its own section with a list and
  a schedule. They remain in the editor for anyone who wants them, and the
  default applies **only** when no layout has been saved yet: once the user has
  touched the eye, their decision wins permanently.

## 1.59.0 - versionCode 131

- Doubled Stalker page concurrency from 6 to 12 workers, on measurement rather
  than instinct. A real load of the owner's slow source: **2,304 series, 174 page
  requests, 27.1 seconds — 6 requests per second**, or about 940 ms each.
  The rate matched the worker count exactly, which is the signature of a client
  bottleneck: the app was waiting, not downloading. OkHttp already permitted 16
  requests per host, so ten slots sat idle the whole time.
  - Twelve, not sixteen: category workers each hold a connection for their own
    first page, so 12 + 3 stays under OkHttp's 16. Above that the extra requests
    would queue silently and the measured rate would start lying.
  - `categoryPool` deliberately stays at 3. The measurement showed its threads
    spend nearly all their time blocked in `future.get()` waiting for pages and
    issue one request each; raising it would add connections without adding
    throughput. The lever was the page pool.
  - The previous value was conservative on purpose — portals are often small
    machines that answer 429/500 under load — so the comment now records both
    the measurement and how to revert. The `ΣΥΝΟΨΗ` log line says immediately
    whether the change paid: if throughput does not rise, the portal is the
    limit and pushing harder buys nothing.
- **`category=*` was dropped from the plan.** It was meant to remove per-category
  overhead, but the owner's sources have one and two selected categories, where
  there is no such overhead to remove. Measuring first avoided building it.

## 1.58.0 - versionCode 130

- Added a one-line `CatalogLoad` summary at the end of every section load: items,
  seconds, page requests, categories and the resulting throughput. Catalog
  loading is reported as very slow, and the shape of the problem is arithmetic —
  the portal serves **14 items per page**, so ten thousand films is roughly seven
  hundred round trips. Nothing about that improves by guessing; the throughput
  figure is what will say whether a concurrency change helped or simply started
  drawing 429s.
- **Found a silent content loss while measuring.** `fetchAllPages` caps at 500
  pages as a safety valve, and when that cap is hit the remaining items are
  dropped with no error at all — at 14 items per page, a catalog over ~7,000
  items in a single paginated stream is quietly truncated. It now logs a warning
  naming the pages requested and the pages fetched.
  This matters for the next step: fetching Stalker with `category=*` instead of
  per category would collapse everything into **one** stream and walk straight
  into that cap. The idea is still right — it removes the per-category overhead
  and lets all six page workers run on one queue — but it cannot ship until the
  cap is handled, and that is why it is not in this release.

## 1.57.0 - versionCode 129

- Category sections now accept **many categories, each becoming its own rail**,
  completing the approved editor design. Until now one section showed one
  category, so the user was not composing a screen — they were choosing which of
  seventy categories to see.
  - The picker is multi-select, and the order in which categories are ticked is
    the order of the rails, so the selection is stored as a list rather than a
    set.
  - The selection **applies on confirm, not per tick**. Applying immediately
    would rebuild the home underneath the dialog on every tap, leaving it
    flickering over a list that reorders itself.
  - Selections are per destination: the same "Movies" section can show different
    categories on Home and inside Movies.
  - Empty selection keeps the previous single-category value, so nobody loses a
    setting and the "largest category" default still applies.
  - The stored separator is a newline, not `|`: provider category names contain
    pipes (`GR | KIDS | ΠΑΙΔΙΚΑ`) and would have been cut in half.
  - `MobileHomeRailResolver.railsFor` replaces `railFor`; a section may now
    produce several rails, and each rail id carries its category because two
    rails of one section must not share a Compose list key.

## 1.56.0 - versionCode 128

- The home layout editor is now **per destination**, and functional rather than
  cosmetic: Home, Live, Movies and Series each keep their own order and their own
  hidden rows, and each lists only the sections that can actually appear there.
  This is the fix for the original report — the editor listed ten sections while
  the screen showed three, because six of them were impossible on the screen the
  owner was looking at and nothing said so.
  - Four visible chips, not a dropdown. The problem to solve is **discovery**,
    not selection: until now nothing revealed that per-screen layouts could
    exist, and a dropdown would have kept that behind a tap. Four options fit as
    chips and say it without explanation. At eight destinations the answer would
    be different.
  - `HomeLayoutPolicy.allowedIn(destination)` is the single place that decides
    what belongs where, and it is decided by **data availability**, not taste: on
    the Live screen there are no series in memory, so a "New episodes" row there
    could never draw however the user set it.
  - Storage is per destination, and **the "home" destination keeps the original
    preference keys**, so a layout already arranged by the user becomes the Home
    layout with no migration code and no loss. The other three start from their
    defaults.
  - A legacy layout file holds every id, since one setting used to govern all
    four screens. Read as "Live", it cannot leak movie and series rows in — a
    test pins that.
  - The editor opens on the screen you were viewing and resets to it each time,
    so it never silently continues configuring somewhere else. Opened from
    Settings, where there is no such context, it starts on Home.
  - Still open, and the larger half of the approved design: multi-category
    selection, where each ticked provider category becomes its own rail instead
    of one category per section.

## 1.55.0 - versionCode 127

- Fixed newly added home sections landing at the very bottom of the screen. A
  `CatalogLoad` capture confirmed the provider does send `rating_imdb` (`6.7`),
  `added` (`2026-07-26 13:26:00`) and `tmdb_id` on catalog rows, so the ranking
  work was correct and the data was there — the rails were simply **out of
  sight**.
  `resolve()` appended anything absent from the saved layout after everything
  saved. The old comment acknowledged the trade-off — "in the wrong place beats
  never appearing" — but for anyone who has touched their layout, "the end"
  means below dozens of category rails, where nobody scrolls. "Top rated movies"
  was reported as missing when it existed and was unreachable.
  - A missing section now lands immediately after its nearest preceding
    neighbour from `DEFAULT` that is already present: where it would be if the
    layout had never been edited.
  - The user's own ordering is untouched. The test that pinned "goes to the end"
    was asserting the defect and has been replaced; the partial-save test now
    checks **relative** order, which is the real contract, instead of absolute
    positions.
- Note for the next reader: "Suggestions for you" is random **by design** — a
  seeded, category-balanced shuffle. It is not a ranking rail and should not be
  reported as unsorted.

## 1.54.2 - versionCode 126

- Added one `CatalogLoad` diagnostic line per section: the first raw catalog row,
  with `rating_imdb`, `rating_kinopoisk`, `added`, `year` and `tmdb_id` called
  out. One line per load, not per item.
  The owner reports on 1.54.0 that nothing is ordered by rating or by date, and
  the ranking code is demonstrably in that build. The field names were taken
  from the only real response anyone has seen from this portal — the **season
  list** — and were assumed to hold for **category pages**, which nobody has
  captured. If they do not, `topRatedFirst` returns empty and `newestFirst`
  falls back, and the visible result is exactly "everything is random" with no
  error anywhere. This line is the difference between knowing and guessing, and
  the previous rounds in this project were lost to precisely that difference.
  No behaviour changes.

## 1.54.1 - versionCode 125

- Fixed 1.54.0 mixing movies, series and live channels into each other. The
  cross-section union was applied to the Movies and Series screens as well as
  the home, so the Movies screen filled with series and channels.
  The mistake was reading `isCatalogHome` as "this is the home screen". It is
  not: it means "a catalog view with no search and no selected group", which is
  equally true on Movies and on Series. The destination is
  `mobilePrimaryDestination`, and only `"home"` should see everything. The other
  two now show what their name says, and the background backfill is likewise
  restricted to the home.

## 1.54.0 - versionCode 124

- The home screen now draws from **every loaded section**, not only the active
  one. The reported symptom was that the home layout editor lists ten sections
  while the home showed three, with no explanation for the gap.
  The cause: the home was fed `state.channels`, which holds one content type at
  a time. With movies loaded, the series and live rails had no data to draw
  and vanished — so six of the ten switches in the editor could not take effect
  no matter what the user did with them.
  - No new download is required for what is already cached:
    `CatalogSessionStore` keeps an LRU of three snapshots, one per section, and
    `homeCatalogState` is their union — references to the same lists, not
    copies.
  - **Parental control is preserved.** The union goes through
    `CatalogPresentationPolicy` exactly like every other list, via
    `visibleHomeChannels()`. Reading the raw union would have let locked groups
    appear on the home — a parental bypass introduced by a screen that merely
    wanted more data.
  - Missing sections are fetched in the background by `backfillHomeSections()`
    under three conditions: **once per source per session**, **without
    publishing to `_state`** (calling `loadAllSections` here would switch the
    visible section out from under the user), and **never while playback is
    starting or running** — a bulk fetch against the same portal as
    `create_link` is what made playback start slow in the first place.
  - Failures are silent by design: this is supplementary background work, and a
    section that did not arrive is simply absent from the home rather than an
    error over content that already works.
  - The home falls back to the active section's channels while the union is
    still empty, so the first frame after a cold start is unchanged.

## 1.53.0 - versionCode 123

- Brought the four ranked rails to **Android TV**: new movies, new episodes, top
  rated movies and top rated series, using the same ranking policies and the
  same resource strings as the handset, so a title never differs by device.
- **The "unused" rail system turned out to be the television.** The previous
  entry described two rail systems; it did not say that `CatalogPolicy` is the
  *entire* TV home. `TvPremiumHomeScreen` has no layout editor and no resolver of
  its own — it renders exactly the list `buildCatalogRailSections` returns, so
  anything missing from there does not exist on a television. Deleting it, as
  was briefly considered, would have deleted the TV home screen. It is also
  still used on mobile, though only to source the "continue watching" section.
  - Mobile draws every other rail from `HomeLayoutPolicy`, so the new sections
    added here are ignored there and **no rail is duplicated**.
  - Rail order is now explicit in `AdaptiveCatalogHome` instead of falling out
    of the order `buildCatalogRailSections` happens to append in. That order is
    the TV home's layout, and it should be readable in one place.
  - Adds `CatalogRailSectionsTest`, which pins what the television gets: the
    four rails exist, top rails are rating-ordered and marked `ranked`, new
    rails are date-ordered and are **not** marked ranked, rails that cannot be
    filled are omitted, live channels never enter any of them, and the main
    trending rail still survives a source with no ratings at all.
- The two rail systems remain, deliberately. They differ because the surfaces
  differ: the handset offers a user-editable layout, the television needs a
  fixed list with a D-pad focus policy. Merging them is a real refactor with
  focus and navigation risk, not a cleanup, and it is not worth doing while the
  ranking logic itself now lives in one place (`CatalogRankingPolicy`).

## 1.52.0 - versionCode 122

- Added **"Top rated movies"** and **"Top rated series"** to the home screen,
  ordered by the provider's rating. Both appear in the home layout editor like
  every other section, so they can be reordered or hidden.
  - They render **only when the source supplies ratings**. With none, the rail
    resolves to null and the section is simply absent — a rail titled "Top" owes
    the viewer a criterion, and inventing one is worse than showing nothing.
    They stay listed in the editor regardless, so their absence on one source
    does not read as a bug when another source shows them.
- Made **"New movies" and "New episodes"** use the provider's `added` date
  rather than its file order. Until now `newest()` returned
  `items.takeLast(20).asReversed()` — an honest guess, documented as one,
  because M3U and Xtream carry no such date. Stalker portals do send it, and it
  was being thrown away.
  - The guess survives as a fallback, and the switch is **thresholded**: the
    dated ordering is used only when it can fill the rail on its own. In a
    catalog of 5,000 films where three carry `added`, a three-card "New" rail
    would be worse than the old twenty-card guess.
- This is the same defect the previous release fixed on the other home surface.
  The app has **two** rail systems: `CatalogPolicy.buildCatalogRailSections`,
  fixed in 1.51.0, and `HomeLayoutPolicy` + `MobileHomeRailResolver`, which is
  what the mobile home actually draws and what the layout editor lists. The
  first fix was real but invisible on the reported screen. Recorded here so the
  next reader does not spend the round finding that out again.

## 1.51.0 - versionCode 121

- Made the home rails mean what their titles say. Both were reported as
  "random", and both were.
  - **"Top" was not sorted at all.** It was the provider's own order with rank
    numbers 1-20 printed over it. The rail promised a ranking that did not
    exist. It now orders by the provider's rating, and items with **no** rating
    are left out rather than sunk to the bottom: a position in a ranked list is
    a claim, and for those there is nothing to claim.
  - **"New" filtered on `year.length == 4` and then sorted by year.** The
    provider in use sends a full first-air date (`1993-08-28`, eleven
    characters), so the filter discarded nearly everything and the rail was all
    but empty. Worse, the year is the wrong question: a 2024 film added last
    year is not newer than a 2023 one added yesterday. It now orders by
    `added`, the date the item entered the provider's catalog, and falls back to
    the year when a provider does not send one.
  - Adds `Channel.addedAt` and `Channel.rating`, populated by `StalkerClient`,
    plus `CatalogRankingPolicy` — pure, stable-sorted and unit tested. Neither
    field participates in any identity key, for the same reason as `tmdbId`.
  - For a **series**, `addedAt` merges as the **maximum** across its rows, not
    the first one seen. A show that gains an episode every week is new, however
    old its premiere.
  - `0`, `""` and `N/A` are all read as "no rating"; portals use them
    interchangeably. Both `7.4` and `7,4` parse.
  - **The Top rail keeps a fallback.** It is the main rail of the home screen
    and predates this change. If a source supplies no ratings at all — M3U, and
    plenty of portals — it keeps every item as before, but **loses the rank
    numbers**, which were the reason it looked arbitrary in the first place.

## 1.50.0 - versionCode 120

- Episode metadata now uses the TMDB id the **provider already sends**, instead
  of searching TMDB by title. Real Stalker rows carry `"tmdb_id":"2328"` and a
  duplicate `"tmdb"` field, and the app was ignoring both.
  The provider's title is the least reliable field there is: it arrives with
  markers (`#`), prefixes (`LINGO| `), quality tags, and for Greek series it is
  often written in Latin characters, which forced transliteration and then a
  skeleton comparison to avoid accepting the wrong show. Every one of those
  steps is a place a wrong series has actually been matched. When the provider
  states the id, all of them are skipped, along with one search round trip per
  season.
  - Adds `Channel.tmdbId`, populated in `StalkerClient` on both the catalog row
    and the season row, so an episode carries it even when its parent is not on
    screen — the player's info panel needs that.
  - `CatalogNormalizer.mergeParent` merges it like the other display fields:
    first non-empty wins, so a series does not lose the id because the row that
    happened to build the bucket lacked it.
  - **It is not an identity field and must never become one.** It stays out of
    `PlaybackQueue.favKey`, `movieIdentity`/`seriesIdentity` and
    `historyMatchKey`. A provider can add, change or drop it between two catalog
    refreshes; if it keyed anything, favourites, history and resume positions
    would move on their own. Two tests pin this: episodes differing only by
    `tmdbId` keep one key, and a series does not split into two catalog entries
    because one row carried the id.
  - The episode cache key becomes `ep:id<n>:<season>` when the id is known.
    Different shows can clean to the same title, and a stored wrong match would
    otherwise survive the fix.
  - Only a positive integer is accepted; real responses put `0`, `""` and
    `null` in the same field.
  - `TmdbLookup` now states which route was taken — "από τον πάροχο, χωρίς
    αναζήτηση" or "από αναζήτηση τίτλου" — so a capture answers immediately
    whether the id was used.
  - Series-level artwork and synopsis still resolve by title; that path already
    works and was left untouched.

## 1.49.1 - versionCode 119

- Gave the bottom navigation bar a visible focus indicator on Android TV. All
  five items — including the "＋" that is now the only way to add a source —
  carried `clickable` but no `tvFocus`, so a D-pad could reach them while
  nothing on screen changed. The remote was landing on a control the viewer had
  no way to see.
  - **This was not introduced by 1.49.0.** The gap existed in
    `StreamingBottomNavigation` from the start; it was simply not load-bearing
    while the Sources header still had its own `＋ Νέα πηγή` button, which did
    carry `tvFocus`. Removing that button made the bar the only route and
    exposed it. Owner-confirmed on a television before this fix.
  - `tvFocus` is applied **before** `clickable`, matching every other focusable
    surface in the app: it only draws, and must observe the focus event that
    `clickable` produces. Reversing the order silently produces no ring.
  - `tint` and `scale` are off so the bar keeps its fixed height and its
    existing selected-item highlight stays readable against the focus ring;
    the ring alone marks position.
  - Mobile is unchanged: `tvFocus` renders nothing without focus, and touch
    surfaces never take it.

## 1.49.0 - versionCode 118

- Reduced the Sources screen to a single way of adding a source. It had two "＋"
  controls with the same icon and the same destination but a different number of
  steps: the header button opened `AddPlaylistScreen` directly, while the bottom
  bar's "＋" first opened `AddMenuSheet` to ask which kind of source, then opened
  the same screen with that tab preselected. The sheet's first four entries — URL
  playlist, Xtream, MAC portal, device — are already tabs of that screen, so the
  question was asked twice.
  The header button and the sheet are both removed. The bottom bar's "＋" now
  goes straight to `AddPlaylistScreen`, which is where the header button went.
  - Nothing is lost for those four: they remain selectable inside the screen.
  - EPG import is unaffected — it has its own entry on the EPG tab.
  - **"Play a single stream" lost its only entry point.** `SingleStreamDialog`
    was reachable from that sheet and nowhere else. It is left in the tree,
    unreferenced and documented as such, rather than deleted: whether the
    capability disappears or gets a new door is the owner's call and is still
    open. Recorded so it cannot be silently forgotten.
  - The header row is now title and source count only, which fits roughly two
    more source cards on the first screen.

## 1.48.0 - versionCode 117

- Stopped requesting a per-episode description from Stalker portals, because the
  portal does not serve one. A Logcat capture of a real series settled a question
  the previous session could only guess at: requests for `episode_id=25`, `28`,
  `33`, `39` and every other episode returned **byte-identical** responses — the
  season list, carrying the **series** description. The parameter is ignored at
  that depth, and so is `season_id`.
  That explains the reported defect exactly. Every episode card showed the same
  synopsis not because the display path was wrong, but because the data was the
  same string repeated. All four episode renderers already prefer
  `tmdbEpisode.overview` and only fall back to `Channel.plot`; the fallback was
  being filled with a value that described the show.
  - **The cost was real.** The logged series has 81 episodes, so opening it fired
    81 requests, each downloading the entire season list, through a three-thread
    pool **shared with category paging**. The capture timestamps span
    `18:15:25.962` to `18:15:30.380` — 4.5 seconds before the episode list could
    appear, plus 81 requests taken away from catalog loading and from the
    `create_link` call that starts playback.
  - Episodes without their own synopsis now show **nothing** rather than
    repeating the show's. Repetition is not a neutral fallback: it presents
    series-level text as if it described the episode.
  - `Channel.plot` still carries whatever the season list provided. Per-episode
    text comes from TMDB, which is the only source that actually holds it.
- Fixed provider `year` and `duration` rendering raw next to each other as
  "1993-08-28 · N/a". Both fields **feed identity keys** — `movieIdentity`,
  `seriesIdentity` and the persisted `localSeriesId` — so `ProviderMetadataPolicy`
  deliberately leaves them untouched in the model, and rewriting them there would
  move favourites, history and resume positions. They are now cleaned where they
  are **read for display** instead, leaving stored data byte-identical.
  - Adds `ProviderMetadataPolicy.displayYear`, which returns the four-digit year
    found in the value and otherwise nothing: real portals put the full first-air
    date in `year`, and a year field that contains no year is noise, not data.
  - The year pattern is restricted to 1900-2099 so a neighbouring identifier such
    as `3153` cannot be mistaken for one.
  - Applied at `DetailScreen`, the single point where the presentation is built
    for both mobile and TV, plus the mobile home hero and the player context
    panel — the three surfaces that render these fields.

## 1.47.0 - versionCode 116

- Released the twenty-four changes below, which had all accumulated under
  `Unreleased` while `versionName`/`versionCode` stayed at 1.46.0/115 from the
  project baseline onward. That is the reason for this release and it is worth
  recording, because the drift itself caused a defect report: a device
  screenshot showed a title still carrying its provider marker ("#"), a genre
  row still rendering "N · A" and a synopsis still reading "N/A" — all three
  already fixed in the working tree. Nothing on screen and nothing in Settings
  could distinguish an installed build from the current source, so the report
  read as three unfixed bugs rather than as one stale APK, and a diagnosis round
  was spent before the title marker gave it away. From this release on, the
  version shown in Settings identifies the build, and a screenshot is
  self-dating. No behaviour changes with this entry.
- Fixed a greeklish title's Greek query never running because a Latin search had
  already returned something irrelevant. `searchId` accepted the first result of
  every `titleCandidates` attempt blindly, and TMDB's search is tolerant enough
  to return an unrelated show for "To Spiti Dipla Sto Potami". That had two
  consequences rather than one: the app locked onto the wrong series, and the
  Greek query below was never reached because the function had already returned.
  For titles detected as greeklish, every result is now verified against the
  skeleton — including the Latin candidates. Such a title cannot match a Latin
  search correctly unless TMDB holds a romanized alternative title, and that
  passes verification anyway. Behaviour for every other title is unchanged, so
  anything that already resolves keeps resolving by first result.
- Fixed provider placeholder values being displayed as if they were content.
  Many providers write `N/A`, `null`, `-` or `unknown` into a field they do not
  know instead of leaving it empty, and the app treated those as real text. Two
  visible results, both reported as "no information":
  - The genre row splits `genre` on `,` `/` `·` `|` `&` to build tags. `N/A`
    contains a slash, so it became **two** tags and rendered literally as
    "N · A" on the hero.
  - The synopsis showed `N/A` instead of being treated as absent, so the
    "no description" fallback never appeared and nothing indicated the data was
    simply missing.
  Adds `ProviderMetadataPolicy` and applies it in `CatalogNormalizer.normalize`,
  the single point every source passes through. Only display fields are
  cleaned — `plot`, `genre`, `cast`, `director`.
  - **`year` and `duration` are deliberately untouched.** They feed the fallback
    identity keys in `movieIdentity`/`seriesIdentity` and the persisted
    `localSeriesId`, so rewriting them would move favourites and history — the
    same class of mistake already made once with `PlaybackQueue.favKey`.
  - A value must be a placeholder in full, never merely contain one, so
    "Nashville" and "Unknown Origins" survive.
  - Real content is returned unmodified, without trimming or reformatting, and
    an already-clean channel is returned as the same instance so a catalog of
    tens of thousands of items does not pay a `copy()` each load.
- Fixed provider decoration around a title reaching the TMDB search query and
  breaking it. Reported with a real list entry, "To Spiti Dipla Sto Potami #":
  the title itself is correct and the transliteration of it was already right,
  but the trailing "#" is the provider's own marker and survived `cleanTitle`,
  so the query went out as "το σπιτι διπλα στο ποταμι #" (encoded `%23`) and
  matched nothing. Every episode then fell back to the show-level synopsis.
  Verified end to end: with the marker removed the query becomes
  "το σπιτι διπλα στο ποταμι" and reduces to the same skeleton as the real
  title, so the existing verification accepts it.
  `cleanTitle` now strips decorative symbols (`#`, `*`, `~`, bullets, arrows,
  stars, pipes, dashes and similar) from **the edges only**, repeatedly, so
  "*** Title ***" clears in one pass.
  - Interior symbols are never touched, because removing them destroys real
    titles: `M*A*S*H`, `9-1-1`, `S.W.A.T.`, `Sex/Life`, `Law & Order`, `Se7en`.
  - Trailing `!` and `?` are not treated as noise; they are legitimate title
    endings.
  - Runs last, after the existing provider-prefix and quality-tag rules, so it
    also catches decoration left exposed once those are removed.
  Adds `TmdbTitleCleanupTest`, which pins both the cleanup and the survival of
  every legitimate title above, and checks that no symbol reaches the query.
- Fixed "back" not returning where the user actually was when moving between
  catalog sections, on both mobile and Android TV. The current section was a
  single variable (`mobilePrimaryDestination` / `tvSection`), not a stack, so
  no history existed and the affordances that looked like "back" were fixed
  destinations instead. Three concrete failures, one cause:
  - The back arrow in Live called `openSection("home")`, so arriving from
    Series and pressing back landed on Home.
  - At the root of Live the screen's own `BackHandler` and the catalog overlay
    handler were both disabled, so the device back button fell through to
    `MainActivity` and opened the "change source?" confirmation — the same
    gesture as the on-screen arrow, with a completely different outcome.
  - The legacy mobile top bar's back arrow was wired straight to the route's
    `onBack`, which is also the change-source confirmation, so in lists and
    search the arrow labelled "back" never went back.
  Adds `SectionNavigationPolicy`, a pure Android-free history stack in the
  shape of `TvLiveBrowsePolicy`, and routes every back affordance through it.
  Revisiting a section collapses to its existing entry rather than pushing a
  duplicate, matching Android's own bottom navigation, so Home → Movies →
  Series → Movies leaves two entries and one back reaches Home instead of
  walking through Series again. Content-type changes that are not user
  navigation (source load, state restore) replace the top entry instead of
  writing history. At the root the policy returns "not my decision" and the
  screen above still asks for source-change confirmation, unchanged.
  The section handler is declared before the overlay handler and is
  additionally gated to be mutually exclusive with it, so details, library,
  search, pickers and the player always close first.
- Fixed episode descriptions and stills staying empty for the rest of the app's
  life after a single failed TMDB lookup. `TmdbClient.episodeMeta` wrote its
  result into `episodeMemCache` even when that result was empty. The disk cache
  was already guarded (`if (result.isNotEmpty())`) and `fetch()` already returns
  early without caching a miss — the file's own comment describes removing this
  exact negative cache and why — but the in-memory write in `episodeMeta` was
  never brought in line. An empty map cannot distinguish "TMDB has no such
  season" from "the call was rate-limited or the network dropped", and opening a
  season fires many card lookups at once against a semaphore-limited pool, so a
  single throttled miss marked the series unknown until the process was killed.
  Empty results are now returned without being cached at any level.
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
