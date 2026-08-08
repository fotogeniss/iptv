# v14 — Adaptive Player Presentation

- Kept ExoPlayer/VLC playback engines unchanged.
- Added visible ±15 second transport controls for VOD on mobile and TV.
- Added a live EPG progress rail to the player overlay.
- Added adaptive transport sizing for touch and DPAD surfaces.
- Improved top-title bounds for narrow mobile screens and widescreen TV.
- Unified overlay auto-hide timing through a pure `PlayerUiPolicy`.
- Added deterministic JVM tests for live progress clamping and control timing.
- Preserved double-tap seek, vertical volume/brightness gestures, channel zapping,
  subtitles, audio tracks, favorites, aspect ratio and fallback playback.
