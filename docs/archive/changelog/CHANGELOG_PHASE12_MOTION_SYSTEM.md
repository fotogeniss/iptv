# Phase 12 — Shared Motion System

Version: 1.39.0 (42)

## Added

- `ui/design/Motion.kt` as the single source of truth for Mobile and Android TV motion.
- Semantic durations: Fast 160ms, Focus 180ms, Medium 280ms, Overlay 320ms, Slow 460ms and Hero 680ms.
- Shared standard and emphasized easing curves.
- Shared Mobile press, TV focus and TV emphasis scale tokens.
- System reduced-motion support based on Android animator duration scale.
- Native View helpers so `PlayerActivity` follows the same tokens as Compose.
- Reusable skeleton shimmer with a static reduced-motion fallback.
- JVM policy tests for duration order and reduced-motion behavior.

## Migrated

- Home, Details, Search, Library, Live TV and EPG cinematic backdrops.
- Mobile press states.
- TV cards, actions, keyboard keys, filters and navigation focus.
- TV navigation rail expansion.
- Player focus and channel-zap overlay animation.
- Snackbar/overlay transitions.
- Details loading skeleton.

## Behavior

- DPAD focus completes in 180ms and never depends on a red focus border.
- Hero transitions use the emphasized 680ms cinematic token.
- Mobile touch feedback uses a 160ms press response.
- When Android animations are disabled, decorative scaling and long transitions are removed while focus contrast and state changes remain visible.

The approved interactive prototype is stored in `prototypes/phase12/`.
