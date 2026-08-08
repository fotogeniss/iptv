# Changelog — v1.40.44 Controlled Player Chrome

## Architecture

- Added an Android-independent `PlayerChromeController`.
- Moved player overlay visibility, auto-hide timer ownership, transient status timeout and TV focus retry state out of `PlayerActivity`.
- `PlayerActivity` now supplies only a narrow Android host/scheduler adapter.
- Kept all existing player entry points and public members unchanged.

## Behavior preserved

- Mobile tap toggles the overlay exactly as before.
- TV Back hides visible controls before leaving the player.
- Visible TV controls continue to return DPAD handling to Android focus navigation.
- Focus restoration still prefers the previous channel-panel target, center play button, toolbar and favorite action in that order.
- Auto-hide duration continues to come from `PlayerUiPolicy`.
- Subtitle lift remains:
  - fullscreen live: 88dp
  - fullscreen VOD: 112dp
  - windowed live: 14dp
  - windowed VOD: 46dp
  - hidden chrome: 10dp
- Picture-in-Picture continues to hide and restore player chrome.

## Reliability

- Rearming auto-hide cancels the previous timer instead of stacking callbacks.
- Hiding the overlay cancels pending focus recovery and auto-hide work.
- Replacing a transient status message cancels its previous timeout.
- `onDestroy` disposes the controller before the Activity handler is cleared.
- TV focus recovery is bounded and cannot retry indefinitely.

## Validation

- Player chrome focused tests: 9 pass, 0 fail.
- Compatibility contracts: 15 pass, 0 fail.
- Architecture contracts: 31 pass, 2 size warnings, 0 fail.
- Deep validation contracts: 47 pass, 1 documented cleartext warning, 0 fail.
- PlayerActivity syntax-risk scan: 0 syntax diagnostics.
- Full Gradle gate attempted but blocked before startup because Gradle 8.9 could not be downloaded in the current environment.
