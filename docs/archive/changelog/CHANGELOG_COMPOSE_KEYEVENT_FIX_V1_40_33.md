# v1.40.33 — Compose key-event compile fix

- Replaced the unavailable `KeyEvent.nativeKeyEvent` access in `TvLiveRail.kt`.
- DPAD confirmation is now detected through Compose `Key` values.
- Press/release handling now uses Compose `KeyEventType`.
- Long-press timing and trailing-click suppression remain unchanged in behavior.
- Version: `1.40.33` (`versionCode 77`).
