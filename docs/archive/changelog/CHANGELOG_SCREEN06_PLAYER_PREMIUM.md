# SCREEN 06 — Premium Player

Version: 1.37.0 (40)

## UI

- Reworked the native player chrome for separate touch-first and DPAD-first layouts.
- Mobile uses a 64dp central play/pause control and 38dp visual action buttons with 48dp+ touch targets.
- Mobile VOD seeking remains double-tap only; the visible rewind/forward buttons are not shown.
- Double-tap and TV transport seeking are now consistently 10 seconds.
- TV uses labeled premium actions with white focus surfaces, brightness and scale instead of red focus borders.
- Live TV center chrome contains only play/pause. Channel zapping remains available through DPAD/media keys and the channel panel.
- Added a real TV side panel / mobile bottom sheet shared by audio, subtitles, playback engine and quality menus.
- Added real ExoPlayer video-track quality selection. VLC remains automatic and can route to the playback-engine menu.
- Added a compact engine/content status badge on TV without claiming unavailable 4K/HDR/bitrate data.
- Improved VOD/live metadata, progress surfaces, player information panel, reconnect banner and next-episode presentation.

## Architecture

Added reusable native-player UI components:

- `ui/components/player/PremiumPlayerStyle.kt`
- `ui/components/player/PlayerSelectionSheet.kt`

Playback engines, stream resolution, ExoPlayer, VLC, history, resume, subtitles, EPG and queue behavior were not replaced.
