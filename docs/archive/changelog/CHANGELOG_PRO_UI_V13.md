# v1.13.0 — Adaptive Home & Details Rewrite

- Replaced the shared legacy details composition with dedicated mobile and TV screens.
- Mobile details now use a full-width cinematic backdrop, touch-first actions, vertical episode rows, progress and cast rail.
- TV details now use a full-screen backdrop, safe metadata column, explicit DPAD focus and full-width episode rail.
- Mobile VOD/Series home now uses the professional catalog rails instead of falling back to the legacy channel list.
- Removed duplicate Continue Watching rendering and routes it through the platform-specific presentation.
- Backend, playback, source clients, EPG and persistence APIs remain unchanged.
