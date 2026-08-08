# Screen 02 — Premium Details / Episodes

## Version 1.33.0

The legacy adaptive details implementation was replaced by independent premium
Mobile and Android TV compositions while preserving the existing data and
playback contracts.

### Shared UI foundation

- Added immutable `DetailPresentation` state shared by both platform UIs.
- Added reusable cinematic backdrop, metadata, progress, cast and related cards.
- Provider genre/director metadata and real catalog recommendations now flow
  through `DetailHost` without changing repositories or parsers.
- Removed the obsolete duplicated `PremiumSeriesEpisodes` implementation.

### Mobile

- Touch-first cinematic hero with back/share, playback, favorites and restart.
- Sticky details navigation for Episodes, About, Cast and Similar content.
- Rich vertical episode cards with real progress and provider descriptions.
- Season selector, loading skeletons, cast rail and related-title rail.

### Android TV

- DPAD-first cinematic hero with Premium-style scale, depth and brightness.
- Focusable action deck and section navigation without red focus borders.
- Episode rail with a contextual focused-episode information panel.
- Season selector, About panel, cast rail and real related-title rail.

### Unchanged

Data repositories, Xtream series loading, TMDB, favorites, history, playback,
ExoPlayer, VLC and navigation ownership remain unchanged.
