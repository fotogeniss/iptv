# Screen 05 — Premium Live TV

## Version

- versionName: `1.36.0`
- versionCode: `39`

## UI

- Replaced the legacy split list/panel Live TV screen with independent premium
  Mobile and Android TV experiences.
- Added a cinematic hero driven by the selected real channel and its current EPG
  programme.
- Added TV DPAD rails, focus scale/shadow, category filters, Now/Next strip and
  a large channel preview surface without creating a second player instance.
- Added a touch-first Mobile hero, filter chips, live rails, sports rail, quick
  guide and Live-selected bottom navigation.
- Removed the old `MobileLiveTvV2.kt` duplicate implementation.

## Real channel branding

- M3U `tvg-logo`, Xtream `stream_icon` and Stalker `logo` data continue to flow
  through the existing `Channel.logo` field.
- Every new Live TV artwork component loads `Channel.logo` through Coil.
- Missing or failed artwork displays a deterministic premium fallback with the
  channel name and Live TV icon.
- No fictional channel logos, ratings, stream bitrates or 4K labels are injected.

## Architecture

```text
ui/components/live/
    LiveFoundation.kt
    LiveProgress.kt

ui/mobile/live/
    MobilePremiumLiveScreen.kt
    MobileLiveHero.kt
    MobileLiveSections.kt

ui/tv/live/
    TvPremiumLiveScreen.kt
    TvLiveHero.kt
    TvLiveRail.kt
```

- `PremiumLiveTvScreen.kt` is now a small adaptive dispatcher.
- `LiveTvPolicy` now supports provider group filters in addition to All,
  Favorites and Recent.
- Existing playback, EPG, favorites, recents and repository contracts are
  unchanged.
