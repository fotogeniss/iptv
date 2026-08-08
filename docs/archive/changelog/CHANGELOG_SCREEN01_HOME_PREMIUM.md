# Screen 01 — Premium Home Redesign

## Scope

The approved Home HTML prototype has been implemented as separate touch-first and DPAD-first Compose experiences.

## Android TV

- Full-screen cinematic backdrop driven by the currently focused title.
- Premium hero metadata and actions with predictable initial focus.
- Horizontal rails with focus scale, brightness, depth and metadata reveal.
- Portrait treatment for new releases and My List; landscape treatment for discovery and resume rails.
- Existing expanding TV navigation rail remains the single navigation surface; no duplicate sidebar was added.
- Legacy catalog top bar is hidden only while the premium Home is active.

## Mobile

- Swipeable cinematic hero with TMDB metadata and page indicators.
- Touch-first play, favorite and details actions.
- Category chips backed by provider groups.
- Premium landscape and portrait content rails with press feedback.
- Home bottom navigation wired to Live, Search, My List and Settings.
- Legacy content pills and top toolbar are hidden only on the premium Home.

## Architecture

New packages:

- `ui/components/home/`
- `ui/mobile/home/`
- `ui/tv/home/`

`AdaptiveCatalogHome` is now only a platform dispatcher. The previous `MobileHomeV2.kt` and `TvHomeV2.kt` duplicate screen implementations were removed.

Data loading, repositories, TMDB lookup, favorites, history, playback and navigation contracts remain unchanged.

## Version

- versionCode: 35
- versionName: 1.32.0
