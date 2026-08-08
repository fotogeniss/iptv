# Phase 12 Motion System — Validation Report

## Build target

- Base project: v1.38.0 / versionCode 41
- Output project: v1.39.0 / versionCode 42
- Scope: UI motion only; data, repositories, navigation contracts and playback engines were not replaced.

## Shared motion system

Created `app/src/main/java/com/prelude/iptv/ui/design/Motion.kt` with:

- Fast: 160ms
- Focus: 180ms
- Medium: 280ms
- Overlay: 320ms
- Slow: 460ms
- Hero: 680ms
- Standard and emphasized easing curves
- Mobile press, TV focus and TV emphasis scale tokens
- Compose and native View duration/scale helpers
- Android system reduced-motion detection
- Shared skeleton shimmer with static reduced-motion fallback

`IptvTheme` installs `MotionSystem`, so every Compose screen receives the same reduced-motion preference.

## Migration coverage

The shared system is used by 31 production Kotlin files, including:

- Home, Details, Search, Library, Live TV and EPG cinematic backdrops
- Mobile card press states
- TV focus cards, actions, filters, keyboard keys and navigation
- TV navigation rail expansion
- Native PlayerActivity focus and channel-zap overlay
- Main overlay/snackbar visibility
- Details loading skeleton

The obsolete `StreamingMotion` declaration was removed. No production `tween(<number>)` or `setDuration(<number>)` animation literals remain.

## Validation performed

- Kotlin compiler PSI parser: 32 changed/new Kotlin files parsed with zero syntax errors.
- Motion policy assertions: semantic tokens match the approved HTML and remain ordered.
- Reduced-motion policy assertions: durations become effectively instant and decorative scale becomes 1f.
- Import audit: no duplicate imports and every `tween` call has the required import.
- Integration audit: `MotionSystem` is installed in `IptvTheme`; native PlayerActivity uses View motion helpers.
- Prototype included at `prototypes/phase12/PHASE_12_MOTION_SYSTEM_SHOWCASE.html`.
- New `Motion.kt` is 119 lines and below the 300-line component target.

## Known build limitation

A full Android Gradle compile could not be executed in this environment because the supplied project still contains a placeholder `gradlew`, no `gradle-wrapper.jar`, and no Android SDK. The report therefore does not claim a successful `assembleDebug`; Android Studio/CI must run the final Gradle build.

The three legacy integration files modified only at their animation call sites (`MainActivity.kt`, `PlayerActivity.kt`, and `Shell.kt`) remain above 300 lines. Their structural split belongs to Phase 13 Production Refactor.
