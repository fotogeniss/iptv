# Build fix v7.1

- Removed the explicit `androidx.compose.foundation.layout.weight` import from `PremiumSeriesEpisodes.kt`. With Kotlin 2.1 this could resolve to Compose's internal `RowColumnParentData.weight` property instead of the public `RowScope.weight` modifier.
- Replaced the wildcard foundation layout import in `Shell.kt` with explicit public layout imports to avoid the same symbol-resolution conflict.
- Added the missing `size` and `widthIn` imports used by `PremiumTvHero.kt`.
