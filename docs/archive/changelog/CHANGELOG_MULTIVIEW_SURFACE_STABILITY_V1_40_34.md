# v1.40.34 — Multiview surface and playback stability fix

- Replaced the two default SurfaceView-backed panes with Media3 PlayerViews configured for TextureView composition.
- Added a real fullscreen 50/50 layout: each pane occupies the complete left or right side, with no outer margins.
- Set both panes to zoom resize mode so video fills its full half instead of appearing as a small fitted rectangle.
- Matched the main player HTTP setup (desktop user agent and cross-protocol redirects) and added explicit connection/read timeouts.
- Added Media3 load retries plus a per-pane 12-second stall watchdog.
- A failed or stalled pane restarts independently; the other player is not released or recreated.
- Kept real audio renderer isolation for the inactive pane.
- Added non-sensitive playback diagnostics (pane index and Media3 error code only).
- Bumped version to 1.40.34 / versionCode 78.
