# v1.40.4 — Mobile content click routing

## Requested behavior

- Tapping a Live channel card on mobile starts playback immediately.
- Tapping a movie or series card opens the Details screen.
- The explicit Watch/Play action remains available in hero/details UI for VOD and series.
- Android TV focus/select behavior remains unchanged.

## Changes

### `ui/PremiumLiveTvScreen.kt`

The mobile `onSelect` callback now:

1. updates the selected channel key;
2. immediately invokes the existing `onPlay(channel)` callback.

The TV `onSelect` callback still only changes selection, preserving DPAD-first behavior.

### `ui/mobile/home/MobilePremiumHomeScreen.kt`

All mobile catalog rails now route card taps through `onDetails`, including the Continue Watching rail. Playback remains available through the dedicated Watch button in hero/details screens.

## Validation

- Kotlin PSI syntax validation: 132 production Kotlin files, 0 syntax errors.
- Mobile Live tap contract: direct playback callback present.
- TV Live selection contract: unchanged.
- Mobile movie/series rail contract: card taps route to Details.
- Generic browse movie/series click routing: Details behavior preserved.
- Forbidden explicit Compose `weight` import: absent.
- ZIP integrity: verified after packaging.

## Version

- `versionName`: `1.40.4`
- `versionCode`: `48`

A full Android Gradle build was not run in this environment because the Android SDK and a usable Gradle wrapper JAR are unavailable.
