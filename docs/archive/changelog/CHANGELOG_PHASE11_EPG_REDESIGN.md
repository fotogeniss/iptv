# Phase 11 — Premium EPG Redesign

## Scope

The EPG data, XMLTV loading, catch-up, reminders, channel playback and callback contract remain unchanged. This phase replaces only the Compose presentation layer.

## Adaptive entry point

`EpgGridScreen.kt` is now a small adaptive dispatcher:

- Android TV / non-touch devices → `TvEpgScreen`
- Phones and tablets → `MobileEpgScreen`

## Shared EPG components

`ui/components/epg/EpgFoundation.kt`

- shared EPG clock and six-hour window
- programme formatting and progress helpers
- deterministic cinematic color palettes
- reusable channel logo, progress and backdrop components
- shared programme visual-state model

## TV — DPAD-first

`ui/tv/epg/`

- cinematic programme hero and poster treatment
- deterministic root DPAD navigation
- shared horizontal timeline state
- independent vertical channel navigation
- Premium-style focus using scale, brightness and shadow, without a red focus border
- live progress, past-state dimming, selected state and current-time indicator
- next-programme information and unchanged play/catch-up/reminder callbacks

## Mobile — touch-first

`ui/mobile/epg/`

- cinematic hero that follows the selected programme
- touch-first guide sheet instead of a compressed desktop grid
- time chips and functional Now/Later/All/Movies/Sports filters
- independent programme rails per channel
- separate select and action behavior for programme cards
- live progress, channel launch action and system-bar insets

## Structure and production cleanup

- New UI files are split below 300 lines.
- The old inline EPG implementation was removed.
- A dangling `@Comp` token found at the end of `AdaptiveCatalogHome.kt` was removed because it was a real Kotlin syntax error unrelated to the EPG redesign.
- Approved HTML prototypes are included under `prototypes/phase11/`.
