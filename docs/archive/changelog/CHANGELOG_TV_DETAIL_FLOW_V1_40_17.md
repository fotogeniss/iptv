# v1.40.17 — TV Details Flow Redesign

## Scope

This release only changes the Android TV movie/series details screen. Provider, playback, history, resume, subtitles, mobile details and navigation contracts remain unchanged.

## Fixed

- Removed the oversized focus target around the `ΣΧΕΤΙΚΑ` tab.
- Replaced the full-width red tab indicator with a compact 28dp indicator.
- Converted the TV detail tabs into a compact, fixed-height DPAD rail.
- Made the tab rail sticky so it remains stable while the hero scrolls away.
- Reduced the TV hero from 500dp to 430dp and tightened internal vertical spacing.
- Added an opaque lower-content background so details do not float over the cinematic backdrop.
- Rebuilt the About section as a compact two-column information panel.
- Kept the About panel focusable for DPAD reading without styling it as a giant button.
- Reduced excess vertical padding in episode, cast and similar sections.

## Root causes

1. `TvDetailHero` used a fixed 500dp height, which occupied almost the entire TV viewport.
2. The old tab indicator used `fillMaxWidth()` inside a `Row`, expanding the tab focus bounds and underline unexpectedly.
3. The tabs were a normal `LazyColumn` item, so focus-driven scrolling left unstable intermediate positions.
4. Lower sections were drawn over the full-screen backdrop without an opaque content surface.

## Version

- versionName: `1.40.17`
- versionCode: `61`
