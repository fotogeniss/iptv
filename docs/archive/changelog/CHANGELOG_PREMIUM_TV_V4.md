# Premium TV v4 — Player & DPAD hardening

## Player overlay
- Removed the permanent technical player/engine label from the viewing experience.
- Added a centered content title and transient status chip for errors, subtitles, EPG loading, and decoder fallback.
- Added current and next EPG programme metadata to the lower player chrome.
- Refreshes live programme metadata once per minute without rebuilding the player.
- Keeps Premium red limited to semantic elements such as LIVE and playback progress.
- Raised subtitles above the fullscreen playback chrome.

## TV focus
- Added explicit DPAD focus routes between back, play/pause, previous/next, seek bar, and toolbar actions.
- The player now restores focus to the previous control after closing the channel panel.
- Left DPAD closes the channel panel and returns to the player controls.
- Disabled previous/next controls at queue boundaries.

## Live channel panel
- Rebuilt the side panel with a neutral black surface and white focus treatment.
- Uses a small red current-channel indicator instead of a red selected background.
- Shows channel number, name, and current EPG programme.
- Opens centered around the current channel and supports preview zapping while remaining open.
- Uses responsive width on phones and TV devices.

## Reliability
- Preserved ExoPlayer/VLC fallback, resume, subtitle, EPG, favorites, and channel-zapping behavior.
- Kept a single EpgManager declaration and all prior build fixes.
