# v1.40.12 — Subtitle Size Controls & TV Settings Build Fix

## Player subtitles

- Added `Μέγεθος υποτίτλων` inside the existing subtitle panel.
- Added DPAD/touch actions:
  - `− Μικρότερο`
  - `+ Μεγαλύτερο`
  - `Επαναφορά στο 100%`
- Supported range: 70%–180%, in 10% steps.
- The selected value is persisted per profile in `PlaylistStore.subtitleSizePercent`.
- The setting is applied immediately to:
  - external SRT/OpenSubtitles rendered by the PlayerActivity subtitle TextView;
  - embedded Media3/ExoPlayer subtitles via `SubtitleView.setFractionalTextSize`.
- The menu reopens after `−` or `+`, so repeated adjustment is possible without navigating back through the subtitle root menu.

## TV Settings compile fix

Added the missing imports in `TvPremiumSettingsScreen.kt`:

```kotlin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.OutlinedButton
```

These resolve the reported `OutlinedButton`, `Refresh`, and follow-on composable-context compile diagnostics.

## Version

- versionName: `1.40.12`
- versionCode: `56`
