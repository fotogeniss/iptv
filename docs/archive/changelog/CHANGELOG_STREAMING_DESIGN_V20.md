# v20 — Unified Streaming Chrome

This phase begins the app-wide visual unification approved in the HTML prototype.

## Shared design components
- Added `StreamingChrome.kt` as the single implementation for:
  - segmented navigation,
  - mobile bottom navigation,
  - screen headers.
- Navigation selection uses neutral surfaces and white selection.
- Red remains reserved for semantic accents such as LIVE and playback progress.

## Applied screens
- Root mobile navigation now uses the shared streaming bottom bar.
- Live / Movies / Series switcher now uses the shared segmented control.
- Live TV filters now use the same segmented control.
- Live TV header now uses the shared screen-header component.

## Version
- versionCode 23
- versionName 1.20.0
