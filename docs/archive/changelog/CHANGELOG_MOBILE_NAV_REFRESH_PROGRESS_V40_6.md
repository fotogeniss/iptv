# v1.40.6 — Mobile navigation insets, refresh and source progress

## Mobile bottom navigation

- Added one shared seven-destination premium mobile navigation component:
  Home, Movies, Series, Live, Search, Library and Settings.
- The bar now respects the real Android navigation-bar inset through
  `WindowInsets.navigationBars` and keeps an additional 10 dp safety gap.
- Scrollable content reserves the bottom bar height, the system navigation
  inset and extra content spacing, so the final cards are not hidden.
- The shared bar is used on the main mobile Home, Movies, Series, Live,
  Search, Library, Settings, Details, filtered catalog and EPG screens.
- Full-screen player, source onboarding and temporary modal flows intentionally
  remain outside the app navigation bar.

## Refresh active source

- Added **Refresh active list** to Mobile and Android TV Settings.
- Added refresh to the active source card action menu.
- Refresh invalidates transient Xtream authentication, starts a new Stalker
  connection, clears transient M3U/session data and downloads the current
  Live/VOD/Series section again.
- Catalog data continues to use `no-cache, no-store`; viewing history and
  resume data remain persisted and source-scoped.

## Real loading progress

- Added transient `SourceLoadProgress`, keyed by the stable source identity.
- HTTP downloads report actual bytes when the provider exposes
  `Content-Length`.
- When a provider hides the total length, the transfer is displayed as
  indeterminate rather than inventing a percentage.
- M3U parsing reports processed lines, Xtream reports download and JSON
  processing stages, and Stalker reports category/page and item processing.
- Progress is visible in the active browse screen and on each source card in
  Settings. Switching source cancels the old source's visible loading state.

## Version

- `versionName`: 1.40.6
- `versionCode`: 50
